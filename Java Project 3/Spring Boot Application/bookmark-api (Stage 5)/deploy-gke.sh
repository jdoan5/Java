#!/usr/bin/env bash
# =============================================================================
# Stage 5.4 — deploy the Bookmark API to GKE.
#
# Safe to re-run: every step checks whether it has already been done, so a
# second run just updates the release instead of erroring on "already exists".
#
#   ./deploy-gke.sh              # build, push, create cluster if needed, deploy
#   ./deploy-gke.sh --status     # what exists right now, and what it costs
#   ./deploy-gke.sh --destroy    # delete the cluster (stop the meter)
# =============================================================================
set -euo pipefail

# --- Settings ----------------------------------------------------------------
PROJECT_ID="${PROJECT_ID:-gke-lab-504304}"
REGION="${REGION:-us-central1}"
CLUSTER="${CLUSTER:-bookmarks}"
REPO="${REPO:-bookmarks}"
APP="bookmark-api"
TAG="${TAG:-0.1.0}"
RELEASE="${RELEASE:-prod}"

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/${APP}"
HELM="$(dirname "$0")/tools/helm"

# Homebrew's gcloud isn't always on a non-login shell's PATH.
export PATH="/opt/homebrew/bin:/opt/homebrew/share/google-cloud-sdk/bin:$PATH"

bold() { printf "\n\033[1m▸ %s\033[0m\n" "$1"; }
ok()   { printf "  \033[32m✓\033[0m %s\n" "$1"; }
warn() { printf "  \033[33m!\033[0m %s\n" "$1"; }
die()  { printf "\n\033[31m✗ %s\033[0m\n" "$1" >&2; exit 1; }

# --- Preflight ---------------------------------------------------------------
command -v gcloud >/dev/null || die "gcloud not found. Install: brew install --cask gcloud-cli"
command -v docker >/dev/null || die "docker not found. Start Docker Desktop."
[ -x "$HELM" ]               || die "helm not found at $HELM (see README: repo-local tools)"
docker info >/dev/null 2>&1  || die "Docker daemon isn't running. Start Docker Desktop."

gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null | grep -q . \
  || die "Not logged in. Run:  gcloud auth login"

# Since Kubernetes 1.26 kubectl cannot authenticate to GKE on its own — it shells
# out to this plugin. Checked HERE rather than later, because without it the
# script would create a cluster (5-10 min, and billable) and only then fail at
# the deploy step with "executable gke-gcloud-auth-plugin not found".
command -v gke-gcloud-auth-plugin >/dev/null \
  || die "gke-gcloud-auth-plugin not found. Install: gcloud components install gke-gcloud-auth-plugin"

# --- Modes -------------------------------------------------------------------
if [ "${1:-}" = "--destroy" ]; then
  bold "Deleting cluster '$CLUSTER' (this stops the billing meter)"
  gcloud container clusters delete "$CLUSTER" --region "$REGION" --project "$PROJECT_ID" --quiet
  ok "Cluster deleted. Images in Artifact Registry are kept (a few cents/month)."
  exit 0
fi

if [ "${1:-}" = "--status" ]; then
  bold "Project: $PROJECT_ID"
  gcloud container clusters list --project "$PROJECT_ID" 2>/dev/null || true
  echo
  warn "A running Autopilot cluster bills for its pods even while idle."
  warn "Stop it with:  ./deploy-gke.sh --destroy"
  exit 0
fi

# --- 1. Project + APIs -------------------------------------------------------
bold "1/6  Project and APIs"
gcloud config set project "$PROJECT_ID" >/dev/null 2>&1
ok "project = $PROJECT_ID"
# Enabling an already-enabled API is a no-op, so this is safe every run.
gcloud services enable container.googleapis.com artifactregistry.googleapis.com \
  --project "$PROJECT_ID" >/dev/null
ok "container + artifactregistry APIs enabled"

# --- 2. Artifact Registry ----------------------------------------------------
bold "2/6  Artifact Registry"
if gcloud artifacts repositories describe "$REPO" \
     --location "$REGION" --project "$PROJECT_ID" >/dev/null 2>&1; then
  ok "repository '$REPO' already exists"
else
  gcloud artifacts repositories create "$REPO" \
    --repository-format=docker --location "$REGION" --project "$PROJECT_ID" \
    --description="Bookmark API images"
  ok "repository '$REPO' created"
fi
gcloud auth configure-docker "${REGION}-docker.pkg.dev" --quiet >/dev/null 2>&1
ok "docker configured to push to ${REGION}-docker.pkg.dev"

# --- 3. Build ----------------------------------------------------------------
bold "3/6  Build image (linux/amd64)"
# --platform is REQUIRED on Apple Silicon: this Mac builds arm64 by default,
# GKE nodes are amd64, and a mismatched image crash-loops with "exec format error".
docker build --platform linux/amd64 -t "${IMAGE}:${TAG}" .
ok "built ${IMAGE}:${TAG}"

# --- 4. Push -----------------------------------------------------------------
bold "4/6  Push to Artifact Registry"
docker push "${IMAGE}:${TAG}"
ok "pushed"

# --- 5. Cluster --------------------------------------------------------------
bold "5/6  GKE cluster"
if gcloud container clusters describe "$CLUSTER" \
     --region "$REGION" --project "$PROJECT_ID" >/dev/null 2>&1; then
  ok "cluster '$CLUSTER' already exists"
else
  warn "creating Autopilot cluster — this takes about 5-10 minutes..."
  gcloud container clusters create-auto "$CLUSTER" \
    --region "$REGION" --project "$PROJECT_ID"
  ok "cluster created"
fi
gcloud container clusters get-credentials "$CLUSTER" \
  --region "$REGION" --project "$PROJECT_ID" >/dev/null 2>&1
ok "kubectl now points at the GKE cluster"

# --- 6. Deploy ---------------------------------------------------------------
bold "6/6  Helm deploy"
# `upgrade --install` = install if absent, upgrade if present. One command for
# both, which is what makes this script safe to re-run.
"$HELM" upgrade --install "$RELEASE" "$(dirname "$0")/helm/$APP" \
  -f "$(dirname "$0")/helm/$APP/values-gke.yaml" \
  --set image.repository="$IMAGE" \
  --set image.tag="$TAG" \
  --wait --timeout 10m
ok "release '$RELEASE' deployed"

cat <<EOF

$(bold "Done — try it")

  kubectl get pods
  kubectl port-forward svc/${RELEASE}-${APP} 8080:80

  # then in another terminal:
  curl localhost:8080/actuator/health

  TOKEN=\$(curl -s -u bruno-client:bruno-secret -X POST \\
    localhost:8080/oauth2/token \\
    -d grant_type=client_credentials -d scope=bookmark.read \\
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

  curl -H "Authorization: Bearer \$TOKEN" localhost:8080/api/bookmarks

$(warn "When you are finished for the day:  ./deploy-gke.sh --destroy")
EOF
