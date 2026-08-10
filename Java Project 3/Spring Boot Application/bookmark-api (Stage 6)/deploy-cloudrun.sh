#!/usr/bin/env bash
# =============================================================================
# Stage 6 — deploy the SAME container to Cloud Run.
#
# Why Cloud Run and not GKE for a permanent demo: Cloud Run scales to zero, so
# an idle service costs essentially nothing, while a GKE cluster bills for as
# long as it exists. Same image, same Artifact Registry — only the runtime
# differs.
#
#   ./deploy-cloudrun.sh            # build, push, deploy, wire the redirect URI
#   ./deploy-cloudrun.sh --status   # show the URL and current config
#   ./deploy-cloudrun.sh --destroy  # delete the service
# =============================================================================
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-gke-lab-504304}"
REGION="${REGION:-us-central1}"
REPO="${REPO:-bookmarks}"
APP="bookmark-api"
SERVICE="${SERVICE:-bookmark-api}"
TAG="${TAG:-0.2.0}"

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${APP}"

export PATH="/opt/homebrew/bin:/opt/homebrew/share/google-cloud-sdk/bin:$PATH"

bold() { printf "\n\033[1m▸ %s\033[0m\n" "$1"; }
ok()   { printf "  \033[32m✓\033[0m %s\n" "$1"; }
warn() { printf "  \033[33m!\033[0m %s\n" "$1"; }
die()  { printf "\n\033[31m✗ %s\033[0m\n" "$1" >&2; exit 1; }

command -v gcloud >/dev/null || die "gcloud not found. brew install --cask gcloud-cli"
gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null | grep -q . \
  || die "Not logged in. Run:  gcloud auth login"

url_of() {
  gcloud run services describe "$SERVICE" --region "$REGION" --project "$PROJECT_ID" \
    --format="value(status.url)" 2>/dev/null
}

if [ "${1:-}" = "--destroy" ]; then
  bold "Deleting Cloud Run service '$SERVICE'"
  gcloud run services delete "$SERVICE" --region "$REGION" --project "$PROJECT_ID" --quiet
  ok "deleted"
  exit 0
fi

if [ "${1:-}" = "--status" ]; then
  bold "Cloud Run service '$SERVICE'"
  gcloud run services describe "$SERVICE" --region "$REGION" --project "$PROJECT_ID" \
    --format="table(status.url, spec.template.spec.containers[0].image, spec.template.metadata.annotations['autoscaling.knative.dev/maxScale'])" \
    2>/dev/null || warn "not deployed"
  exit 0
fi

command -v docker >/dev/null || die "docker not found"
docker info >/dev/null 2>&1 || die "Docker daemon isn't running"

# --- 1. APIs -----------------------------------------------------------------
bold "1/5  Enable APIs"
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  --project "$PROJECT_ID" >/dev/null
ok "run + artifactregistry enabled"

# --- 2. Build + push ---------------------------------------------------------
bold "2/5  Build and push (linux/amd64)"
# Cloud Run runs amd64, same as GKE — this Mac is arm64, so the flag stays.
docker build --platform linux/amd64 -t "${IMAGE}:${TAG}" .
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet >/dev/null 2>&1
docker push "${IMAGE}:${TAG}"
ok "pushed ${IMAGE}:${TAG}"

# --- 3. Deploy ---------------------------------------------------------------
bold "3/5  Deploy to Cloud Run"
# --max-instances is a COST CAP, not a performance setting: it bounds what a
# traffic spike (or a bored stranger with curl) can possibly cost.
# --min-instances=0 lets it scale to zero, which is why idle is ~free.
DEPLOY_LOG="$(mktemp)"
gcloud run deploy "$SERVICE" \
  --image "${IMAGE}:${TAG}" \
  --region "$REGION" \
  --project "$PROJECT_ID" \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --cpu 1 --memory 1Gi \
  --min-instances 0 \
  --max-instances 2 \
  --cpu-boost \
  --set-env-vars "SPRING_PROFILES_ACTIVE=k8s" \
  --quiet 2>&1 | tee "$DEPLOY_LOG"

# Cloud Run answers on TWO hostnames: a legacy "SERVICE-HASH-REGION.a.run.app"
# and the newer "SERVICE-PROJECTNUMBER.REGION.run.app". Annoyingly
# `describe --format=value(status.url)` reports only the LEGACY one while the
# deploy output prints the newer one — so collect both, or the redirect URI is
# registered for a hostname the visitor isn't actually using.
SERVICE_URL="$(url_of)"
ALT_URL="$(grep -oE 'https://[a-zA-Z0-9.-]+\.run\.app' "$DEPLOY_LOG" | sort -u | grep -v "^${SERVICE_URL}$" | head -1 || true)"
rm -f "$DEPLOY_LOG"
[ -n "$SERVICE_URL" ] || die "Deployed but could not read the service URL"
ok "service is at $SERVICE_URL"
[ -n "$ALT_URL" ] && ok "also reachable at $ALT_URL"

# --- 4. Wire the OAuth redirect URI ------------------------------------------
bold "4/5  Register the redirect URI"
# Chicken-and-egg: the redirect URI must contain the public URL, but Cloud Run
# only assigns that URL once the service exists. So deploy first, then patch the
# env var — which rolls out a new revision.
# The leading ^@^ tells gcloud to split this flag on "@" instead of ",". Without
# it gcloud reads the comma between the two URIs as the start of a second
# env var and fails with "Bad syntax for dict arg".
REDIRECTS="${SERVICE_URL}/authorized"
[ -n "$ALT_URL" ] && REDIRECTS="${REDIRECTS},${ALT_URL}/authorized"
REDIRECTS="${REDIRECTS},http://localhost:8080/authorized"

gcloud run services update "$SERVICE" \
  --region "$REGION" --project "$PROJECT_ID" \
  --update-env-vars "^@^APP_OAUTH_REDIRECT_URIS=${REDIRECTS}" \
  --quiet >/dev/null
ok "redirect URIs registered: ${REDIRECTS}"

# --- 5. Verify ---------------------------------------------------------------
bold "5/5  Verify"
sleep 5
HEALTH="$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}/actuator/health" || true)"
[ "$HEALTH" = "200" ] && ok "health 200" || warn "health returned $HEALTH (a cold start can take ~15s; retry)"

NOAUTH="$(curl -s -o /dev/null -w "%{http_code}" "${SERVICE_URL}/api/bookmarks" || true)"
[ "$NOAUTH" = "401" ] && ok "unauthenticated request correctly refused (401)" || warn "expected 401, got $NOAUTH"

cat <<EOF

$(bold "Live at ${SERVICE_URL}")

  Login page:   ${SERVICE_URL}/login          (john / password)
  Health:       ${SERVICE_URL}/actuator/health

  TOKEN=\$(curl -s -u bruno-client:bruno-secret -X POST \\
    ${SERVICE_URL}/oauth2/token \\
    -d grant_type=client_credentials -d scope=bookmark.read \\
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

  curl -H "Authorization: Bearer \$TOKEN" ${SERVICE_URL}/api/bookmarks

$(warn "Data is IN-MEMORY: it resets whenever the service scales to zero. That is")
$(warn "fine for a demo — the seed data comes back on the next cold start.")
$(warn "Costs ~nothing while idle. Remove entirely with: ./deploy-cloudrun.sh --destroy")
EOF
