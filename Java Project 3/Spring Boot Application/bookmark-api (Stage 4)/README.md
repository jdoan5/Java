# Bookmark API — Stage 4 (user login + ownership)

Stages 2–3 used the **`client_credentials`** grant: an *app* gets a token, no
human involved. **Stage 4 adds real user login** with the **`authorization_code`
+ PKCE** grant, plus **per-user ownership** — each person only sees their own
bookmarks. Both grant types now work side by side.

## The two ways to get a token

```
client_credentials  (an APP):
  Bruno ──client id+secret──▶ /oauth2/token ──▶ JWT (sub = "bruno-client")

authorization_code + PKCE  (a USER):
  Bruno ─▶ /oauth2/authorize ─▶ login page ─▶ (user logs in) ─▶ redirect w/ code
  Bruno ─▶ /oauth2/token (code + PKCE verifier) ─▶ JWT (sub = "john")
```

The token's **`sub`** (subject) is the **owner**: `bruno-client` for the app
token, or the username for a logged-in user. The API stores it on each bookmark
and filters every query by it.

## Clients and users

| Client | Grant | Auth | Use |
|--------|-------|------|-----|
| `bruno-client` | `client_credentials` | secret `bruno-secret` | app token (Stages 2–3) |
| `bruno-pkce-client` | `authorization_code` + PKCE | **public** (no secret) | user login |

**Login users** (in-memory): `john` / `password` and `jane` / `password`.

> Seeded data: 2 bookmarks owned by `bruno-client`, 2 owned by `john`. So the app
> token sees its 2, john sees his 2, jane sees none until she creates some — a
> live demo of ownership isolation.

## What changed vs Stage 3

- **`AuthorizationServerConfig`** — registers the new `bruno-pkce-client`
  (`authorization_code` + `requireProofKey(true)`), and sends browsers to
  `/login` for the authorize endpoint.
- **`SecurityConfig`** — now three chains: the auth server, the JWT API
  (`/api/**`), and a **form-login** chain that serves `/login`. Adds two
  in-memory users + a delegating `PasswordEncoder`.
- **`Bookmark`** — new `owner` column; **repository/service** filter every
  operation by owner; **controller** reads the owner from
  `@AuthenticationPrincipal Jwt` (`jwt.getSubject()`).
- **`/authorized`** — a tiny landing page for the OAuth redirect URI.

## How to run (IntelliJ IDEA Ultimate)

Open **`bookmark-api (Stage 4)`** and run `BookmarkApiApplication` on JDK 21
(port 8080). Visiting <http://localhost:8080/oauth2/authorize> in a browser now
redirects you to a **login page** — that's the new piece.

## Testing in Bruno

Open the **`bookmark-api (Stage 4)/bruno/Bookmark API`** collection → **Local**
environment.

### A) App token (unchanged) — `client_credentials`
Run **Get Token**, then **List bookmarks** etc. You'll see the 2 bookmarks owned
by `bruno-client`.

### B) User login — `authorization_code` + PKCE
Use the **List bookmarks (as user)** request. Its Auth tab is set to **OAuth 2.0
/ Authorization Code** with these values (set them in the UI if the request's
pre-filled config needs adjusting):

| Field | Value |
|-------|-------|
| Grant Type | Authorization Code (with **PKCE** enabled) |
| Callback URL | `http://localhost:8080/authorized` |
| Auth URL | `http://localhost:8080/oauth2/authorize` |
| Access Token URL | `http://localhost:8080/oauth2/token` |
| Client ID | `bruno-pkce-client` |
| Client Secret | *(empty — public client)* |
| Scope | `bookmark.read bookmark.write` |

Click **Get Access Token** (Bruno opens a browser) → log in as **`john` /
`password`** → Bruno captures the code, exchanges it with PKCE, and sends the
token. **Send** the request → you'll see **john's** 2 bookmarks, not the app
token's. Create one and it's owned by `john`. Log in as `jane` instead and you
start empty.

> Paste the resulting `access_token` into <https://jwt.io> and compare the `sub`
> claim: `bruno-client` vs `john`.

### Why two clients?
A public client (browser/tool) can't safely keep a secret, so it uses **PKCE** —
a one-time code verifier/challenge — instead of a client secret to prove the
token request comes from the same party that started the login. That's the
modern standard for SPAs, mobile apps, and API tools like Bruno.

## Per-user ownership, end to end

- `POST /api/bookmarks` stamps the new row with `owner = jwt.sub`.
- `GET /api/bookmarks` / `/{id}` only return rows where `owner = jwt.sub`.
- Asking for someone else's id returns **404** (not 403) — we don't even reveal
  that it exists.

## Roadmap

- **Stage 5 (ideas):** a custom branded login page; refresh-token rotation;
  move users and registered clients into the database (JDBC repositories); an
  OIDC `id_token` + `/userinfo`; method-level `@PreAuthorize` checks.
