# Job Tracker (Stage 6)

A personal desktop **Job Application Tracker** built in Java (Swing UI) with **SQLite persistence**, **advanced search**, **sorting**, improved **validation**, and **real logging**.

---

## What’s New in Stage 6

Compared with Stage 5, Stage 6 focuses on usability and correctness:

- **Advanced search (multi-field)**  
  Search by **Company** and/or **Position** using “contains” matching, optionally combined with:
    - **Status filter**
    - **Date range** (From / To)

- **Sorting controls**  
  Sort results by a selected field (commonly **Date Applied** or **ID**) with **ascending/descending** toggle.

- **Better validation + clearer UI messages**
    - Required fields (company, position, location)
    - Date parsing with friendly error messages
    - Guardrails for empty searches / invalid ranges

- **Logging (Logback)**
    - No more SLF4J “NOP logger” behavior
    - Logs written to console by default (configurable)

- **Cleaner distribution workflow**
    - Build a runnable fat-jar (for `java -jar`)
    - Optional macOS **.dmg** packaging using `jpackage`
    - Windows packaging notes for **.msi**

---

## Tech Stack

- Java **17+** (project targets release 17)
- Maven
- Swing (desktop UI)
- SQLite via `org.xerial:sqlite-jdbc`
- SLF4J + Logback (logging)

---

## Project Structure (Key Files)

```
src/main/java/com/johndoan/jobtracker/
  ApplicationRepository.java
  ApplicationService.java
  ApplicationStatus.java
  JobApplication.java
  SearchFilter.java
  SortField.java
  SortDirection.java
  Validation.java
  UiMain.java

src/main/java/com/johndoan/jobtracker/persistence/
  Database.java
  JdbcApplicationRepository.java

src/main/java/com/johndoan/jobtracker/ui/
  JobTrackerFrame.java

src/main/resources/
  schema.sql
  logback.xml
```

---

## Build & Run (Local)

From the Stage 6 project directory:

```bash
mvn clean package
```

Run the UI jar:

```bash
java -jar target/*-ui.jar
```

### SQLite “native access” warning (Java 21+ / 25)

If you see warnings similar to:

> “Restricted methods will be blocked in a future release…”

You can silence them by running with:

```bash
java --enable-native-access=ALL-UNNAMED -jar target/*-ui.jar
```

---

## Where the Database Lives

By default, the app stores the SQLite database under your home directory:

- **macOS/Linux:** `~/.jobtracker/job_tracker.db`

This keeps your data stable even if you rebuild or move the project folder.

---

## How to Verify SQLite is Working

1) **Run the app** and add a couple of applications.

2) Confirm the DB file exists:

```bash
ls -la ~/.jobtracker/job_tracker.db
```

3) Inspect the database with `sqlite3`:

```bash
sqlite3 ~/.jobtracker/job_tracker.db
```

Inside the SQLite shell:

```sql
.tables
.schema applications

SELECT COUNT(*) FROM applications;
SELECT * FROM applications ORDER BY id DESC LIMIT 20;
.quit
```

Notes:
- The table name is **`applications`** (not `job_applications`).
- `id` is **AUTOINCREMENT**; if you delete rows, IDs will not “reuse” old values.

---

## UI Tips (Stage 6)

### Search
- Enter **Company** and/or **Position** terms (contains match).
- Optionally set:
    - **Status**
    - **Date From** / **Date To**

Then click **Search** (or “Apply Filter”, depending on your UI labels).

### Sort
- Choose a **Sort Field** and **Direction** (ASC/DESC), then refresh/search again.

### Clear View
- Clears the **table display only** (does not delete DB data).  
  Use **Refresh** or **Search** to load rows again.

---

## CSV Import/Export (Optional)

If your Stage 6 UI still includes CSV controls, they are typically for:
- **Import**: load CSV into the current view (and optionally into DB, depending on implementation)
- **Export**: write the currently displayed rows to CSV

If you are “all-in” on SQLite, you can treat CSV as a convenience feature and keep it optional.

---

## Schema & Migrations

- `src/main/resources/schema.sql` contains the database schema used at startup.
- For schema evolution, a simple pattern is:
    - Maintain a `schema_version` table
    - Apply incremental migration scripts (`V1__...sql`, `V2__...sql`, etc.)

Stage 6 includes the structure needed to support migrations; you can extend it further in Stage 7+.

---

## Logging (Logback)

Stage 6 ships with a `logback.xml` under `src/main/resources/`.

If you want logs written to a file, update `logback.xml` to add a rolling file appender and set the desired log directory.

---

## Packaging for Distribution

### Option A: Share the runnable JAR (fastest)
After `mvn clean package`, share:

- `target/<artifact>-ui.jar`

Run with:

```bash
java -jar <artifact>-ui.jar
```

### Option B: macOS `.dmg` (jpackage)

Prereqs:
- JDK installed (includes `jpackage`)
- You built the UI jar in `target/`

Example:

```bash
APP_NAME="JobTracker"
VERSION="3.1.0"
JAR_NAME="$(ls target/*-ui.jar | xargs -n1 basename)"

mkdir -p dist

jpackage   --type dmg   --name "$APP_NAME"   --app-version "$VERSION"   --input target   --main-jar "$JAR_NAME"   --dest dist   --java-options "--enable-native-access=ALL-UNNAMED"
```

The DMG will appear in:

- `dist/`

### Windows `.msi` (later)
On Windows, `jpackage --type msi` typically requires the **WiX Toolset** (or other required tooling depending on your setup).  
Once built, the same jar can be packaged as an MSI for easy installation.

---

## Git Hygiene (Avoid committing huge DMGs)

DMG files can exceed GitHub’s size limits (100MB). Recommended:

- Do **not** commit `.dmg` files.
- Add a `.gitignore` entry:

```gitignore
/dist/
/target/
*.dmg
*.pkg
*.msi
```

If you must version large binaries, use **Git LFS**.

---

## Troubleshooting

### “No suitable driver found for jdbc:sqlite:…”
Usually means the SQLite JDBC driver is not on the classpath.
- Ensure Maven dependency `org.xerial:sqlite-jdbc` is present.
- Use the `*-ui.jar` (fat jar) produced by the Shade plugin.

### “Could not find or load main class …” after installing the .app
Typically indicates the `.app` points at the wrong main class or the wrong jar.
- Ensure your jar manifest has the correct `Main-Class`
- When running `jpackage`, pass the **exact** jar name under `target/` via `--main-jar`.

---

## Roadmap (Stage 7+ Ideas)

- Multi-column sort + clickable table header sorting
- Better migration framework (Flyway/Liquibase)
- Full-text search / indexing improvements
- Export with validation + error reporting
- Cross-platform installers (macOS notarization, Windows signing, etc.)
