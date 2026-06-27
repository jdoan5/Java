# Bookmark API — Stage 3 (database persistence)

Stage 2 secured the API with OAuth2 but still stored bookmarks in memory — they
vanished on restart. **Stage 3 adds a real database** with **Spring Data JPA**
and **H2**, plus a **bulk-create** endpoint. The OAuth2 setup is unchanged, so
everything you learned in Bruno still works exactly the same.

## What's new vs Stage 2

- **Persistence:** `Bookmark` is now a JPA `@Entity` mapped to a `bookmarks`
  table; the in-memory store is replaced by a Spring Data `JpaRepository`.
- **Survives restarts:** H2 runs in **file mode** (`./data/bookmarks.mv.db`).
- **Browse it in IntelliJ:** connect the IDE's Database tool to the same file.
- **Bulk create:** `POST /api/bookmarks/batch` accepts a JSON **array** and
  creates them all at once (returns `201` with the created list).
- **Idempotent seeding:** sample data is inserted only when the database is empty
  (no more duplicates piling up on every boot).

The bookmark logic, the controllers, and the whole OAuth layer are essentially
the same — this stage swaps out *storage*, which is exactly the kind of change
the layered design was built to absorb.

## Endpoints

| Method | Path | Scope |
|--------|------|-------|
| GET    | `/public/health`        | open |
| GET    | `/actuator/health`      | open |
| GET    | `/api/bookmarks`        | `bookmark.read` |
| GET    | `/api/bookmarks/{id}`   | `bookmark.read` |
| POST   | `/api/bookmarks`        | `bookmark.write` — one object `{…}` |
| POST   | `/api/bookmarks/batch`  | `bookmark.write` — array `[ {…}, {…} ]` |
| PUT    | `/api/bookmarks/{id}`   | `bookmark.write` |
| DELETE | `/api/bookmarks/{id}`   | `bookmark.write` |

## How to run (IntelliJ IDEA Ultimate)

Same as before: **File → Open…** → select **`bookmark-api (Stage 3)`** → run
`BookmarkApiApplication` on a **JDK 21**. On first run Hibernate creates the
schema (you'll see `create table bookmarks …` in the console because
`show-sql` is on), and the seeder inserts two bookmarks. A `data/` folder
appears in the project root holding the H2 file.

## Token + API flow in Bruno (unchanged)

Open the **`bookmark-api (Stage 3)/bruno/Bookmark API`** collection → **Local**
environment → run **Get Token**, then the protected requests. New this stage:

- **Create bookmarks (bulk)** — sends a JSON array to `/api/bookmarks/batch`.
  This is the right way to create several at once; the single **Create bookmark**
  still takes one object (sending it an array returns `400` by design).

To prove persistence: create a bookmark, **stop and restart** the app in
IntelliJ, then run **List bookmarks** again — it's still there.

## Browse the database in IntelliJ

This is the big IntelliJ payoff for this stage. While the app is running
(`AUTO_SERVER=TRUE` allows a second connection):

1. **View → Tool Windows → Database** → **+** → **Data Source → H2**.
2. In the dialog, set the **URL** field directly to the exact same URL the app
   uses (this is what makes `AUTO_SERVER` connect you to the running instance):
   ```
   jdbc:h2:file:<absolute-path>/bookmark-api (Stage 3)/data/bookmarks;AUTO_SERVER=TRUE
   ```
   Replace `<absolute-path>` with the real path (drag the `data` folder in from
   Finder to get it). **User:** `sa`, **Password:** *(empty)*. If IntelliJ offers
   to download the H2 driver, accept it.
3. **Test Connection** → **OK**. Expand the schema and open the `BOOKMARKS` and
   `BOOKMARK_TAGS` tables → right-click → **Edit Data**, or open a query console
   and run `select * from bookmarks;`.

> If IntelliJ offers to download the H2 driver, accept it.

## Where to look in the code

```text
domain/Bookmark.java            # now a JPA @Entity (@Table, @Id, @ElementCollection)
repository/BookmarkRepository.java  # interface extends JpaRepository + @Query findByTag
service/BookmarkService.java    # @Transactional; findAll(Sort), existsById+deleteById
config/DataSeeder.java          # seeds only when count() == 0
web/BookmarkController.java     # adds POST /api/bookmarks/batch
src/main/resources/application.yml   # H2 datasource + JPA settings
src/test/resources/application.yml   # tests use a throwaway in-memory H2
config/AuthorizationServerConfig.java, SecurityConfig.java  # OAuth, unchanged
```

## Roadmap

- **Stage 4 (planned):** real user login. Move from the `client_credentials`
  grant (app-only) to `authorization_code` + PKCE so a human logs in, and make
  each bookmark **owned by the logged-in user** (filter by their `sub`). With a
  database now in place, ownership is a natural next step.
