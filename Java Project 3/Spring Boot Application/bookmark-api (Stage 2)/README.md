# Bookmark API — Stage 2 (OAuth2)

Stage 1 was an open REST API. **Stage 2 secures it with OAuth2** — and does so in
a single, self-contained app so there are **no external accounts to register**.

This one app plays two roles:

1. **Authorization Server** — *issues* JWT access tokens at `POST /oauth2/token`.
2. **Resource Server** — *validates* those tokens and protects `/api/bookmarks/**`.

Bruno is the "client": it swaps a client id/secret for a token, then sends that
token on every API call.

```
  Bruno ──(client id + secret)──▶  POST /oauth2/token   (Authorization Server)
        ◀──────(JWT access token)──
  Bruno ──(Authorization: Bearer <jwt>)──▶  /api/bookmarks  (Resource Server)
```

---

## Key concepts (the whole point of this stage)

| Term | What it means here |
|------|--------------------|
| **Grant type** | `client_credentials` — for app-to-app calls with no human logging in. |
| **Client** | `bruno-client` / secret `bruno-secret` (in `AuthorizationServerConfig`). |
| **Access token** | A signed **JWT**. Paste it into <https://jwt.io> to read its claims. |
| **Scope** | `bookmark.read` and `bookmark.write` — permissions baked into the token. |
| **Authority mapping** | Spring turns scope `bookmark.read` into authority `SCOPE_bookmark.read`. |
| **401 vs 403** | 401 = no/invalid token (not authenticated). 403 = valid token, missing scope. |

### Who can do what

| Method | Path | Required scope |
|--------|------|----------------|
| GET    | `/public/**`          | none (open) |
| GET    | `/actuator/health`    | none (open) |
| GET    | `/api/bookmarks`      | `bookmark.read` |
| GET    | `/api/bookmarks/{id}` | `bookmark.read` |
| POST   | `/api/bookmarks`      | `bookmark.write` |
| PUT    | `/api/bookmarks/{id}` | `bookmark.write` |
| DELETE | `/api/bookmarks/{id}` | `bookmark.write` |

---

## What changed vs Stage 1

- **`pom.xml`** — added `spring-boot-starter-oauth2-resource-server`,
  `spring-security-oauth2-authorization-server`, and `spring-security-test`.
- **`config/AuthorizationServerConfig.java`** — the token issuer: registered
  client, RSA signing key, JWK source, JWT decoder, server settings.
- **`config/SecurityConfig.java`** — the resource server: which scopes each
  endpoint requires.
- **Tests** — now use the `jwt()` post-processor; added 401 (no token) and 403
  (wrong scope) cases.
- **Bruno** — new **Get Token** request + every protected request sends
  `Authorization: Bearer {{accessToken}}`.

The bookmark logic (controller/service/repository/domain) is **unchanged** —
security is layered on top, which is exactly how it works in real apps.

---

## How to run (IntelliJ IDEA Ultimate)

Same as Stage 1: **File → Open…** → select the **`bookmark-api (Stage 2)`**
folder (the one with `pom.xml`) → **Open as Project** → let Maven import → use a
**JDK 21** → run `BookmarkApiApplication`. It starts on port **8080**.

On startup, browse <http://localhost:8080/public/health> (open) and try
<http://localhost:8080/api/bookmarks> (now returns **401** — that's the point).

---

## How to get a token and call the API in Bruno

1. **Open the collection** — Bruno → **Open Collection** → select
   `bookmark-api (Stage 2)/bruno/Bookmark API`.
2. **Pick the environment** — top-right dropdown → **Local**. It defines
   `baseUrl`, `clientId` (`bruno-client`), `clientSecret` (`bruno-secret`), and a
   secret `accessToken` variable that gets filled in automatically.
3. **Get Token** (run it first) — it POSTs to `/oauth2/token` with the client id
   and secret as HTTP **Basic** auth and `grant_type=client_credentials`. You get
   back JSON like:
   ```json
   {
     "access_token": "eyJraWQ...<jwt>...",
     "token_type": "Bearer",
     "expires_in": 3599,
     "scope": "bookmark.read bookmark.write"
   }
   ```
   A post-response script saves `access_token` into `{{accessToken}}`.
4. **Call the API** — run **List bookmarks**, **Create bookmark**, etc. Each one
   sends `Authorization: Bearer {{accessToken}}` automatically.
5. **See security work** — duplicate **List bookmarks**, delete its
   `Authorization` header (or clear `{{accessToken}}`), and resend → **401**. To
   see a **403**, request a token with only `scope: bookmark.read` in **Get
   Token**, then run **Create bookmark**.

> The token expires in 1 hour. When calls start returning 401, just run **Get
> Token** again.

---

## Where to look in the code

```text
config/
  AuthorizationServerConfig.java  # issues JWTs (/oauth2/token, signing key)
  SecurityConfig.java             # protects /api/** by scope
  DataSeeder.java                 # unchanged: seeds 2 bookmarks
web/, service/, repository/, domain/   # unchanged from Stage 1
```

Inspect a token's contents by pasting `access_token` into <https://jwt.io>, or
hit the public key set at <http://localhost:8080/oauth2/jwks>.

---

## Roadmap

- **Stage 3 (planned):** persist bookmarks in a database (H2/JPA); add the
  `authorization_code` + PKCE flow so a real user can log in (not just an app);
  scope bookmarks to their owner.
