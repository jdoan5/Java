# Bookmark API — Stage 6: Cloud Run (the permanently-live demo)

**Live:** https://bookmark-api-990816290294.us-central1.run.app
&nbsp;&nbsp;·&nbsp;&nbsp;sign in with `john` / `password`

Stage 5 proved the app runs on Kubernetes. Stage 6 takes the **exact same
container image** and runs it on **Cloud Run** instead — because a demo you want
to leave online for months has a different requirement than one you spin up to
learn on.

## Why a second runtime at all

| | GKE (Stage 5) | Cloud Run (Stage 6) |
|---|---|---|
| Billing | Per hour, **as long as the cluster exists** | Per request; **scales to zero** |
| Idle cost | Real money, every day | ≈ nothing |
| You manage | Cluster, nodes, Deployment, Service | A container and a URL |
| Good for | Learning Kubernetes; complex systems | A small service you want permanently online |

Both deployments use the *same* `Dockerfile` and the *same* image in Artifact
Registry. Only the runtime differs — which is the whole point of containers.

## Deploy

```bash
./deploy-cloudrun.sh              # build, push, deploy, wire the redirect URI
./deploy-cloudrun.sh --status     # URL + current config
./deploy-cloudrun.sh --destroy    # remove the service
```

Cost controls are baked in: `--min-instances 0` (scale to zero) and
`--max-instances 2` (a hard ceiling, so a traffic spike can't run up a bill).

## The two things Stage 5's code could not do

Kubernetes reached the app through `kubectl port-forward`, so it was always
`http://localhost:8080`. The moment the app is served publicly over HTTPS at a
different hostname, two assumptions break. Both fixes live in this stage.

### 1. The redirect URI was hard-coded

`AuthorizationServerConfig` used to register exactly one redirect URI:

```java
.redirectUri("http://localhost:8080/authorized")   // Stage 5
```

OAuth requires the redirect URI to match **exactly**, so the browser login flow
fails anywhere else. It now comes from configuration and accepts several
environments at once:

```java
@Value("${app.oauth.redirect-uris:http://localhost:8080/authorized}")
private String[] redirectUris;
```

The deploy script sets `APP_OAUTH_REDIRECT_URIS` to the Cloud Run URL *and*
localhost, so the same build works in both places.

> This is a chicken-and-egg problem: the redirect URI must contain the public
> URL, but Cloud Run only assigns that URL once the service exists. The script
> deploys first, reads the URL, then patches the variable.

### 2. TLS is terminated at the edge

Cloud Run handles HTTPS and forwards **plain HTTP** to the container. Spring
therefore believed it was serving `http://`, and advertised that in the OAuth2
issuer and every URL in the discovery document — on an `https://` site. Browsers
block the mix, so the authorization-code flow breaks.

```yaml
server:
  forward-headers-strategy: framework   # honour X-Forwarded-Proto
```

With it, the issuer is correct:

```console
$ curl -s .../.well-known/oauth-authorization-server | jq '{issuer}'
{ "issuer": "https://bookmark-api-990816290294.us-central1.run.app" }
```

The same setting is what you would need behind a Kubernetes Ingress, so it is
not Cloud Run trivia — it is the general "running behind a proxy" lesson.

## Verified live

```
✓ /actuator/health                 200  (~80 ms warm)
✓ /api/bookmarks without a token   401
✓ client_credentials token         issued
✓ /api/bookmarks with a token      200 + seeded data
✓ /login                           200
✓ issuer                           https://…  (not http://)
```

## Things worth knowing

- **Data is in-memory** (`SPRING_PROFILES_ACTIVE=k8s`). When the service scales
  to zero the database goes with it; the seeded bookmarks come back on the next
  cold start. Fine for a demo, and it means the public demo is self-cleaning.
- **First request after idle takes ~10–15 s** — a JVM cold start. A 503 right
  after a quiet period is that, not a failure. `--cpu-boost` softens it.
- **Cloud Run publishes two URL formats** — a legacy `…-uc.a.run.app` and the
  newer `…-<project-number>.<region>.run.app`. Both work and both are
  registered as redirect URIs. Note `gcloud … --format="value(status.url)"`
  returns the *legacy* one while the console shows the newer one.
- **gcloud and commas:** values containing commas need a delimiter prefix, or
  gcloud parses the comma as the start of a second variable:
  ```bash
  --update-env-vars "^@^KEY=a,b"
  ```

## Also in this folder

The Kubernetes work from Stage 5 is carried over (`helm/`, `k8s/`,
`deploy-gke.sh`) so the two runtimes can be compared side by side. To use
`deploy-gke.sh` you need the repo-local `tools/` binaries described in the
Stage 5 README; `deploy-cloudrun.sh` needs only `gcloud` and `docker`.

## Run locally

Unchanged from earlier stages — the defaults still point at localhost:

```bash
docker build -t bookmark-api:local .
docker run --rm -p 8080:8080 bookmark-api:local
```
