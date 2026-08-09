# Bookmark API — Stage 5 (Docker, Kubernetes & Helm)

Stages 1–4 built the API and its OAuth2 security. **Stage 5 ships it**: the same
app is containerised, deployed to Kubernetes, and packaged as a Helm chart that
one command can install, upgrade, or roll back.

The application code is unchanged from Stage 4 apart from **one security line**
(explained below) — everything else here is packaging and operations.

```
  jar ──▶ Docker image ──▶ raw K8s manifests ──▶ Helm chart ──▶ GKE
        (5.1)             (5.2)                (5.3)          (5.4)
```

## Why raw manifests before Helm?

`k8s/` and `helm/` deploy the *same two resources*. Reading them side by side is
the fastest way to understand what Helm actually does: it is a **templating and
release layer** over YAML you could have written by hand — plus versioned
releases you can roll back.

| | `k8s/deployment.yaml` | `helm/.../templates/deployment.yaml` |
|---|---|---|
| replicas | hard-coded `2` | `{{ .Values.replicaCount }}` |
| image | hard-coded | `{{ .Values.image.repository }}:{{ ... }}` |
| names | fixed `bookmark-api` | `{{ include "bookmark-api.fullname" . }}` → `dev-bookmark-api` |
| dev vs prod | copy the file, edit it | one chart + a different `values.yaml` |
| undo a bad deploy | re-apply the old file by hand | `helm rollback dev 1` |

---

## 5.1 — Containerise

The [`Dockerfile`](Dockerfile) is **multi-stage**: a Maven+JDK image builds the
jar, then only the jar is copied into a slim JRE image. Build tools never ship.

```bash
docker build -t bookmark-api:0.1.0 .
docker run -p 8080:8080 bookmark-api:0.1.0
```

Nothing needs installing — the builder image brings its own Maven.

**Three details that matter, each learned the hard way:**

1. **`USER 1000`, not `USER spring`.** With `runAsNonRoot: true`, Kubernetes must
   prove the user isn't root *before* starting the container, and it does not read
   `/etc/passwd` from the image. A named user is unverifiable and the pod dies with:
   ```
   Error: container has runAsNonRoot and image has non-numeric user (spring),
   cannot verify user is non-root
   ```
2. **`COPY --chown`, not `RUN chown`.** A separate `chown` rewrites every file
   into a *second* layer — it silently doubled the 62 MB jar in the image.
   With that fixed and an Alpine JRE base, the image went **755 MB → 403 MB**.
3. **`-XX:MaxRAMPercentage=75`.** The JVM sizes its heap from the *container's*
   memory limit instead of the host's RAM. Without it the JVM overshoots the
   cgroup limit and Kubernetes OOM-kills the pod.

## 5.2 — Raw Kubernetes manifests

```bash
kubectl apply -f k8s/
kubectl get pods -w
kubectl port-forward svc/bookmark-api 8080:80
```

- **Deployment** — runs 2 replicas, restarts them if they die.
- **Service** — one stable in-cluster address load-balancing across ready pods.

**Health probes** use Spring Boot's Kubernetes-shaped endpoints, enabled by
`management.endpoint.health.probes.enabled` in
[`application-k8s.yml`](src/main/resources/application-k8s.yml):

| Probe | Endpoint | On failure |
|---|---|---|
| `startupProbe` | `/actuator/health/liveness` | keeps the other probes waiting while the JVM boots |
| `livenessProbe` | `/actuator/health/liveness` | **restarts** the container |
| `readinessProbe` | `/actuator/health/readiness` | removes the pod from the Service (no restart) |

> **The one app-code change in Stage 5.** `SecurityConfig` permitted the exact
> path `/actuator/health`, which does **not** match `/actuator/health/liveness`.
> kubelet sends no credentials, so the probes got `401`, Kubernetes read that as
> "unhealthy", and the pods would have restart-looped forever. The fix is the
> added `"/actuator/health/**"` matcher.

## 5.3 — The Helm chart

```
helm/bookmark-api/
├── Chart.yaml          # chart version + appVersion (the image tag)
├── values.yaml         # the chart's public API — everything overridable
└── templates/
    ├── _helpers.tpl    # reusable name/label partials
    ├── deployment.yaml
    ├── service.yaml
    ├── serviceaccount.yaml
    ├── secret.yaml     # only rendered when secretEnv is set
    ├── hpa.yaml        # only rendered when autoscaling.enabled
    ├── ingress.yaml    # only rendered when ingress.enabled
    └── NOTES.txt       # printed after install
```

```bash
helm lint helm/bookmark-api
helm template dev helm/bookmark-api      # render locally, apply nothing
helm install dev helm/bookmark-api --wait
helm upgrade dev helm/bookmark-api --set replicaCount=3 --wait
helm rollback dev 1
helm history dev
```

**Chart details worth knowing:**

- **Selector labels are immutable.** `bookmark-api.selectorLabels` deliberately
  excludes `version` — a Deployment's selector cannot change, so a version label
  in there breaks every future `helm upgrade`.
- **The secret checksum annotation.** The pod template carries
  `checksum/secret: {{ ... | sha256sum }}`. Change a secret and the hash changes,
  which changes the pod spec, which rolls the pods. Without it, pods keep running
  with the **old** secret until something unrelated restarts them.
- **`replicas` is omitted when autoscaling is on**, so Helm and the HPA don't
  fight over the replica count.

### Environment overlays

```bash
# dev: 1 replica, small
helm install dev helm/bookmark-api --set replicaCount=1

# prod: autoscaling, ingress, external database
helm install prod helm/bookmark-api \
  --set autoscaling.enabled=true \
  --set ingress.enabled=true \
  --set env.DB_URL=jdbc:postgresql://bookmark-db:5432/bookmarks \
  --set env.DB_DRIVER=org.postgresql.Driver \
  --set secretEnv.DB_PASSWORD=...
```

Every `DB_*` variable is already wired through `application-k8s.yml`, so moving
from in-memory H2 to PostgreSQL needs **no code change** — only values.

> **Why in-memory H2 in Kubernetes?** A pod's filesystem is ephemeral and each
> replica gets its own copy, so a *file* database buys nothing in a cluster —
> two replicas would silently disagree. Real persistence arrives with PostgreSQL
> in step 5.5.

## Local cluster (no cloud spend)

[kind](https://kind.sigs.k8s.io) runs a real Kubernetes cluster in Docker:

```bash
tools/kind create cluster --name bookmarks
docker build -t bookmark-api:0.1.0 .
tools/kind load docker-image bookmark-api:0.1.0 --name bookmarks   # no registry needed
tools/helm install dev helm/bookmark-api --wait
tools/kind delete cluster --name bookmarks                          # clean up
```

`helm` and `kind` live in `tools/` as repo-local binaries (same pattern as the
Terraform project) — nothing is installed system-wide, and `tools/` is gitignored.

### Verified end to end

```
✓ image builds, boots in ~4s, runs as uid 1000
✓ 2/2 pods Running, both passing readiness probes
✓ probes return 200 with NO credentials
✓ OAuth token issued through the Service; GET /api/bookmarks → 200 + seeded data
✓ no token → 401
✓ helm upgrade 2→3 pods, helm rollback → back to 2, history intact
```

## Smoke test through the Service

```bash
kubectl port-forward svc/dev-bookmark-api 8080:80

curl localhost:8080/actuator/health

TOKEN=$(curl -s -u bruno-client:bruno-secret -X POST \
  localhost:8080/oauth2/token \
  -d grant_type=client_credentials -d scope=bookmark.read \
  | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/bookmarks
```

The Bruno collection from earlier stages still works — point it at the
forwarded port.

---

## 5.4 — Deploy to GKE ✅ done

Chosen over EKS purely on cost: GKE's free tier covers one zonal cluster's
management fee, while EKS bills ~$0.10/hour (~$73/month) for the control plane
before a single node runs.

The whole flow is scripted in [`deploy-gke.sh`](deploy-gke.sh) — run that rather
than pasting commands:

```bash
./deploy-gke.sh
```

It sets the project, enables the APIs, creates the registry and cluster if they
don't exist, builds **for the right CPU architecture**, pushes, and installs the
chart. Re-running it is safe: every step is skipped if already done.

Doing it by hand instead, set the variables **once** and quote them afterwards:

```bash
PROJECT_ID=gke-lab-504304      # no angle brackets!
REGION=us-central1
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/bookmarks/bookmark-api"

gcloud auth login
gcloud config set project "$PROJECT_ID"
gcloud services enable container.googleapis.com artifactregistry.googleapis.com

# 1. Registry + push (the cluster can only pull from somewhere it can reach)
gcloud artifacts repositories create bookmarks \
  --repository-format=docker --location="$REGION"
gcloud auth configure-docker "$REGION-docker.pkg.dev"

docker build --platform linux/amd64 -t "$IMAGE:0.1.0" .
docker push "$IMAGE:0.1.0"

# 2. Cluster
gcloud container clusters create-auto bookmarks --region "$REGION"
gcloud container clusters get-credentials bookmarks --region "$REGION"

# 3. Deploy the same chart — only the image repository changes
tools/helm install prod helm/bookmark-api --set image.repository="$IMAGE" --wait
```

> **Never paste `<PROJECT_ID>` literally.** In bash `<` means "read input from a
> file", so `gcloud config set project <PROJECT_ID>` dies with
> `syntax error near unexpected token 'newline'`, and
> `IMAGE=.../<PROJECT_ID>/...` gives `PROJECT_ID: No such file or directory` —
> leaving `$IMAGE` empty and the later build failing with `invalid reference
> format`. Assign a real value to a variable first, as above.

> **Apple Silicon → `--platform linux/amd64` is mandatory.** Your Mac builds
> arm64 images by default; GKE nodes are amd64 (Container-Optimized OS). Without
> the flag the pods crash-loop with `exec format error`, which looks like an app
> bug and isn't. `deploy-gke.sh` always passes it.

> **`kubectl` needs `gke-gcloud-auth-plugin`.** Since Kubernetes 1.26 kubectl no
> longer authenticates to GKE by itself — it shells out to a separate binary.
> Without it, everything up to cluster creation succeeds and then the deploy dies:
> ```
> Error: Kubernetes cluster unreachable: getting credentials:
>        exec: executable gke-gcloud-auth-plugin not found
> ```
> Fix once:
> ```bash
> gcloud components install gke-gcloud-auth-plugin
> ```
> Homebrew's cask does **not** symlink it into `/opt/homebrew/bin`, so add the SDK
> bin directory to your shell profile or the plugin stays invisible to `kubectl`:
> ```bash
> export PATH="/opt/homebrew/share/google-cloud-sdk/bin:$PATH"
> ```
> `deploy-gke.sh` checks for the plugin in its preflight — deliberately *before*
> spending 10 billable minutes creating a cluster.

> **Cost:** set a billing alert on day one. When you're done for the day:
> ```bash
> gcloud container clusters delete bookmarks --region us-central1
> ```
> Autopilot bills for running pods, so a cluster left up overnight still costs.

### Verified on GKE (2026-08-09)

```
cluster  bookmarks (Autopilot, us-central1)   project gke-lab-504304
pod      prod-bookmark-api-…  1/1 Running     on an amd64 Container-Optimized OS node
✓ /actuator/health → 200        ✓ OAuth token issued
✓ GET /api/bookmarks → 200 + seeded data      ✓ no token → 401
```

## Remaining steps

- **5.5 — PostgreSQL** as a chart dependency, replacing in-memory H2; credentials
  via Secrets. (The `DB_*` env plumbing is already in place.)
- **5.6 — Ingress + HTTPS** with a managed certificate; the OAuth issuer URL then
  needs to match the public hostname.
- **5.7 — CI/CD**: GitHub Actions builds the image on push and runs `helm upgrade`.
- **5.8 — Terraform** to provision the cluster, tying in the Terraform project.
