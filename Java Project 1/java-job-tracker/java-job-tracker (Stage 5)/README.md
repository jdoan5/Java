# Job Application Tracker (Stage 5)

Stage 5 upgrades the project into a **personal desktop application** (Java Swing UI) backed by **SQLite persistence**.

- **Stage 3 (v1.x)**: UI + CSV import/export (file-based)
- **Stage 4 (v2.x)**: UI + SQLite persistence (no auto-load; DB stored locally)
- **Stage 5 (v3.x)**: UI + SQLite + **search + sorting** + stronger validation + starter **JUnit 5** test setup

---

## Features (Stage 5)

- Desktop UI (Swing) to add / update / delete job applications
- SQLite persistence (data survives app restarts)
- Search by **company** and/or **position**
- Sort results (e.g., by ID, company, position, status, date applied)
- Optional “Clear View” button (clears the table display without deleting DB data)

---

## Requirements

- Java **17+** (recommended: 17 or 21 LTS for development)
- Maven **3.9+**
- macOS: `sqlite3` (preinstalled on most systems)

Verify:

```bash
java -version
mvn -v
```

---

## Build & run (UI)

From the Stage 5 project root:

```bash
mvn clean package
java -jar target/*-ui.jar
```

### Note: sqlite-jdbc native-access warning (newer JDKs)
On newer JDKs (e.g., 25), you may see warnings about native access from `sqlite-jdbc`.
If you want to suppress them while running the jar directly:

```bash
java --enable-native-access=ALL-UNNAMED -jar target/*-ui.jar
```

---

## Where the SQLite database lives

By default, the app stores data in:

- `~/.jobtracker/job_tracker.db`

The schema/table name is:

- **`applications`**

If you previously tried `job_applications`, that table name does not exist in Stage 4/5.

---

## How to view the SQL table (verify persistence)

### Option A: interactive sqlite shell

```bash
sqlite3 ~/.jobtracker/job_tracker.db
```

Inside the `sqlite>` prompt:

```sql
.tables
.schema applications
SELECT COUNT(*) FROM applications;
SELECT * FROM applications ORDER BY id DESC LIMIT 20;
.quit
```

Important: commands like `.tables` and `.schema` only work **inside** the sqlite shell (they are not regular zsh commands).

### Option B: one-liner query (no sqlite shell)

```bash
sqlite3 ~/.jobtracker/job_tracker.db "SELECT id, company, position, status, date_applied FROM applications ORDER BY id DESC LIMIT 20;"
```

---

## Packaging for distribution (macOS DMG via jpackage)

You can package the app into a `.dmg` for personal use on macOS.

### 1) Build the UI jar

```bash
mvn clean package
ls -la target | grep ui.jar
```

You should see something like:

- `java-job-tracker-2.1.0-SNAPSHOT-ui.jar`

### 2) Create the DMG

From the project root:

```bash
APP_NAME="JobTracker"
APP_VERSION="3.0.0"
JAR_NAME="$(ls target/*-ui.jar | xargs -n1 basename | head -n 1)"

mkdir -p dist

jpackage   --type dmg   --name "$APP_NAME"   --app-version "$APP_VERSION"   --input target   --main-jar "$JAR_NAME"   --main-class com.johndoan.jobtracker.UiMain   --dest dist   --java-options "--enable-native-access=ALL-UNNAMED"   --java-options "-Dapple.awt.application.name=Job Tracker"
```

This produces a DMG under `dist/`.

### 3) Rename DMG file (optional)

If you want the filename to include your version:

```bash
mv "dist/${APP_NAME}-${APP_VERSION}.dmg" "dist/Job Tracker-${APP_VERSION}.dmg"
```

### 4) If the app won’t open after dragging to Applications

macOS Gatekeeper may block unsigned apps.

- Try right-click the app → **Open** (first launch)
- Or remove quarantine:

```bash
xattr -dr com.apple.quarantine /Applications/JobTracker.app
```

---

## Troubleshooting

### “Could not find or load main class …” when launching the packaged app
This usually means the `jpackage` command used the wrong entrypoint.

Fix:
- Ensure your jar manifest includes `Main-Class: com.johndoan.jobtracker.UiMain`
- And include `--main-class com.johndoan.jobtracker.UiMain` in your `jpackage` command (see above)

### “No suitable driver found for jdbc:sqlite:…”
This happens if `sqlite-jdbc` is not on the runtime classpath.
For packaging, you should use the `*-ui.jar` produced by the Maven Shade plugin (it bundles dependencies).

---

## Stage 5 vs Stage 4 (what changed)

- Adds **search + sorting** controls in the UI
- Improves UX around filtering and refreshing the table
- Keeps Stage 4 persistence model (SQLite, local DB)
- Sets up scaffolding for unit tests (JUnit 5) so you can expand coverage
