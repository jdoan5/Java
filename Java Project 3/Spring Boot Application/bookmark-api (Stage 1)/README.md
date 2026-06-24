# Bookmark API — Stage 1 (REST, in-memory)

A small **Spring Boot** REST API for saving bookmarks (links with tags and
notes). This is **Stage 1**: a clean, working CRUD API with **no security yet**.
Stage 2 adds OAuth2.

This project is where I'm learning three things:

1. **Spring Boot** — controllers, services, dependency injection, validation.
2. **Bruno** — a git-friendly API client (the `bruno/` folder is a ready-made
   request collection).
3. **IntelliJ IDEA Ultimate** — importing a Maven project, running, and using
   the HTTP/Spring tooling.

> OAuth is the headline goal of the overall project, but it lands in **Stage 2**.
> Stage 1 deliberately keeps everything open so the REST + Bruno + IntelliJ
> basics are solid first.

---

## What it does

| Method | Path                     | Description                          | Auth (Stage 1) |
|--------|--------------------------|--------------------------------------|----------------|
| GET    | `/public/health`         | Liveness check                       | open           |
| GET    | `/api/bookmarks`         | List all (optional `?tag=` filter)   | open           |
| GET    | `/api/bookmarks/{id}`    | Get one                              | open           |
| POST   | `/api/bookmarks`         | Create (returns 201 + `Location`)    | open           |
| PUT    | `/api/bookmarks/{id}`    | Replace                              | open           |
| DELETE | `/api/bookmarks/{id}`    | Delete (returns 204)                 | open           |

Two sample bookmarks are seeded on startup so the first `GET` returns data.

A bookmark looks like:

```json
{
  "id": 1,
  "title": "Spring Boot Reference",
  "url": "https://docs.spring.io/spring-boot/index.html",
  "tags": ["spring", "reference"],
  "notes": "Official Spring Boot documentation.",
  "createdAt": "2026-06-23T18:00:00Z",
  "updatedAt": "2026-06-23T18:00:00Z"
}
```

---

## What I'm practicing

- **Layered design:** `web` (controllers/DTOs) → `service` (logic) →
  `repository` (storage). The same shape as the other projects in this repo.
- **DTOs vs domain:** request/response records keep the API contract separate
  from the internal `Bookmark` object.
- **Bean Validation:** `@NotBlank` / `@URL` on request records, surfaced as
  clean 400 JSON via a `@RestControllerAdvice`.
- **In-memory storage:** a `ConcurrentHashMap` + `AtomicLong`, ready to be
  swapped for a database in a later stage.
- **Testing:** a context-loads smoke test plus `MockMvc` web tests.

---

## How to run (IntelliJ IDEA Ultimate)

This machine has no command-line JDK or Maven, so run it **inside IntelliJ**
(it ships with both a JDK and a bundled Maven).

1. **Open the project**
   - `File → Open…`
   - Select this folder: **`bookmark-api (Stage 1)`** (the one containing
     `pom.xml`).
   - Choose **Open as Project**. IntelliJ detects Maven and imports it.

2. **Set the JDK (if prompted)**
   - `File → Project Structure → Project → SDK`.
   - Pick a **JDK 21** (any vendor). If none is listed, choose
     **Add SDK → Download JDK… → version 21** and IntelliJ downloads one.

3. **Let Maven finish importing**
   - Watch the bottom status bar / the **Maven** tool window (right edge) until
     dependency download is done.

4. **Run it**
   - Open `src/main/java/com/johndoan/bookmarks/BookmarkApiApplication.java`.
   - Click the green ▶ in the gutter next to `main` → **Run**.
   - The console should end with `Started BookmarkApiApplication ... on port 8080`.

5. **Quick check** — open <http://localhost:8080/public/health> in a browser,
   or run the Bruno requests below.

> Prefer the terminal later? Once a JDK 21 is on your `PATH` you can also run
> `mvn spring-boot:run` or `mvn test`. IntelliJ's bundled Maven works without
> that, via the Maven tool window.

---

## How to test with Bruno

The `bruno/Bookmark API/` folder **is** a Bruno collection (plain `.bru` text
files, safe to commit to git).

1. Open **Bruno** → **Open Collection** → select
   `bookmark-api (Stage 1)/bruno/Bookmark API`.
2. Top-right environment dropdown → pick **Local** (sets
   `baseUrl = http://localhost:8080`).
3. With the app running, send requests in order:
   - **Health (public)** → `200`, `{"status":"UP", ...}`
   - **List bookmarks** → the two seeded entries
   - **Create bookmark** → `201`; a post-response script stores the new id in
     `{{bookmarkId}}`
   - **Get / Update / Delete bookmark** → reuse `{{bookmarkId}}`
   - **List bookmarks by tag** → `?tag=spring`

Try breaking things on purpose: send **Create bookmark** with `"title": ""` to
see the validation 400, or **Get bookmark** with id `999999` for the 404 shape.

---

## Project layout

```text
bookmark-api (Stage 1)/
  pom.xml
  bruno/Bookmark API/          # Bruno collection (.bru files)
  src/main/java/com/johndoan/bookmarks/
    BookmarkApiApplication.java # entry point
    domain/Bookmark.java        # core model
    repository/BookmarkRepository.java  # in-memory store
    service/BookmarkService.java        # business logic
    web/
      BookmarkController.java    # /api/bookmarks
      PublicController.java      # /public/health
      GlobalExceptionHandler.java
      NotFoundException.java
      dto/                       # request/response records
    config/DataSeeder.java       # seeds sample data
  src/main/resources/application.yml
  src/test/java/com/johndoan/bookmarks/  # tests
```

---

## Roadmap

- **Stage 2 — OAuth2 (next):** turn this app into a self-contained OAuth2 setup.
  A built-in **Authorization Server** issues JWT access tokens; a **Resource
  Server** protects `/api/bookmarks/**` by scope (`bookmark.read` /
  `bookmark.write`). Bruno fetches a token (client-credentials grant) and sends
  it as a `Bearer` header. No external accounts needed.
- **Stage 3 (planned):** swap the in-memory store for a database (H2/JPA) and
  add per-user ownership of bookmarks.
