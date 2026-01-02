# Java Job Tracker — Stages 1–5 Summary

This repository evolves a simple **Job Application Tracker** from a console-only Java app into a **desktop Swing UI** and then into a **SQLite-backed desktop application** with search/sort enhancements and packaging guidance.

> **Audience:** personal use on macOS (with notes for Windows packaging later).  
> **Java:** 17+ for building (some packaging steps may use a newer JDK that includes `jpackage`).  
> **Build tool:** Maven.

---

## Quick Stage Map (What Changed When)

### Stage 1 — Console MVP (in-memory)
**Goal:** Establish a clean domain model + simple workflow.

- Console menu UI
- In-memory repository (data disappears when the app exits)
- Add / list / filter by status / update status
- First “export to CSV” capability (or placeholder depending on your snapshot)

**Run**
```bash
mvn clean package
java -cp target/classes com.johndoan.jobtracker.Main
```

---

### Stage 2 — CSV persistence (load/save)
**Goal:** Keep data between runs by reading/writing CSV.

- CSV export + import (Load)
- Default path typically `CSV/job_applications.csv`
- On launch, the app may start empty and requires Load (recommended), depending on your Stage 2 configuration.

**Run**
```bash
mvn clean package
java -cp target/classes com.johndoan.jobtracker.Main
```

**Notes**
- Closing the console ends the process; **memory is not persisted** unless you load/save CSV.
- Multiple CSV files are fine (e.g., `job_applications_2.csv`)—they are just different datasets.

---

### Stage 3 — Desktop UI (Swing) + CSV
**Goal:** Move from console to a usable desktop UI for personal use.

- Swing UI with:
    - Table view
    - Add application form
    - Update status
    - Delete selected row
    - Filter by status
    - CSV path text field + Browse + Load + Save buttons
- Optional “start empty” behavior (no auto-load) for demo mode

**Run (recommended)**
```bash
mvn clean package
java -cp target/classes com.johndoan.jobtracker.UiMain
```

**Run as a JAR (if shade/uber jar is configured)**
```bash
# Example name; check your target/ directory for the exact file
java -jar target/java-job-tracker-1.0-SNAPSHOT-ui.jar
```

---

### Stage 4 — SQLite persistence (replaces CSV as primary store)
**Goal:** Replace CSV as the main persistence mechanism with SQLite.

- SQLite DB stored in your home folder (typical):
    - `~/.jobtracker/job_tracker.db`
- Database schema creates an `applications` table with `AUTOINCREMENT` ID
- UI continues to work, but data now persists via SQLite
- CSV may remain as optional import/export (depending on how you kept Stage 3 features)

**Run**
```bash
mvn clean package
java -cp target/classes com.johndoan.jobtracker.UiMain
```

**Verify database is working (CLI)**
```bash
sqlite3 ~/.jobtracker/job_tracker.db
.tables
.schema applications
SELECT COUNT(*) FROM applications;
SELECT * FROM applications ORDER BY id DESC LIMIT 20;
.quit
```

**Important ID note**
- SQLite `AUTOINCREMENT` does **not reset** when you delete rows.
- IDs will continue increasing over time (this is expected).
- If you want a “1..N display row number”, show a separate **Row #** column in the UI (Stage 5 option).

---

### Stage 5 — UI Enhancements + Testing + Packaging direction (v3 line)
**Goal:** Improve usability and project quality.

Typical additions:
- Search by company/position (and/or combined search)
- Sorting in the UI (e.g., click column headers)
- “Clear View” button (clears table display without deleting DB data)
- JUnit 5 test scaffolding for service/repository (depending on your stage snapshot)
- Packaging direction for distribution:
    - macOS `.app/.dmg` via `jpackage`
    - Windows `.msi` later

**Run**
```bash
mvn clean package
java -cp target/classes com.johndoan.jobtracker.UiMain
```

---

## CSV vs SQLite — Which One “Wins”?
- **Stage 3:** CSV is the persistence mechanism.
- **Stage 4+:** SQLite should be the **source of truth** for persistence.
- Keeping CSV import/export in Stage 4/5 is useful for:
    - backup
    - migration
    - sharing a dataset
- Having multiple CSV files does not “hurt” anything—just treat them as optional import/export targets.

---

## Packaging (macOS) — Recommended Practice

### Option A: Keep the repo lightweight (recommended)
Do **not** commit `.dmg` files to GitHub (GitHub blocks files > 100MB unless you use LFS).

Add these to `.gitignore`:
```gitignore
# Build outputs
/target/
/dist/

# macOS
.DS_Store

# IntelliJ
.idea/
*.iml
```

### Option B: Build a `.dmg` locally with `jpackage`
You’ll need a JDK that includes `jpackage` (often JDK 17+; you may have a newer JDK installed for packaging).

1) Build the UI jar (uber jar / “ui jar”)
```bash
mvn clean package
ls -la target | grep -E "ui\.jar|shaded\.jar"
```

2) Package it
```bash
APP_NAME="JobTracker"
VERSION="3.0.0"
JAR_NAME="java-job-tracker-3.0.0-SNAPSHOT-ui.jar"   # adjust to whatever exists in target/

mkdir -p dist

jpackage   --type dmg   --name "$APP_NAME"   --app-version "$VERSION"   --input target   --main-jar "$JAR_NAME"   --dest dist   --java-options "--enable-native-access=ALL-UNNAMED"
```

**If the installed app won’t open**
- Confirm the installed app name/path in `/Applications`
- Run from Terminal to see the real error:
```bash
"/Applications/JobTracker.app/Contents/MacOS/JobTracker"
```

Common causes:
- Wrong `--main-jar` (file not found in `--input`)
- Wrong main class (manifest/launcher mismatch)
- Packaging used an unexpected launcher name

---

## Versioning Guidance
A simple mental model:
- **Stage 3:** v1.x (CSV + UI)
- **Stage 4:** v2.x (SQLite persistence)
- **Stage 5:** v3.x (Search/Sort + packaging focus)

You can reflect this in `pom.xml`:
```xml
<version>3.0.0-SNAPSHOT</version>
```

And in the UI title:
- `Job Tracker v3.0.0 (SQLite • Search/Sort)`

---

## Suggested Next Enhancements (Stage 6+)
- More advanced search (multi-field, “contains”, status + date range)
- Sorting by date applied (ascending/descending toggle)
- Data validation (date parsing, required fields, clearer UI messages)
- Migrations (schema evolution)
- Proper logging (Logback config) rather than SLF4J “NOP logger”
- Windows packaging:
    - `jpackage --type msi` (requires WiX Toolset or appropriate tooling on Windows)

---

## Troubleshooting Cheatsheet

### “No applications yet” but CSV exists (Stage 2/3)
You still need to press **Load** (unless you enabled auto-load).

### SQLite table name mismatch
Your table is likely named **`applications`** (not `job_applications`):
```sql
SELECT COUNT(*) FROM applications;
```

### IDs look “wrong”
SQLite `AUTOINCREMENT` continues across deletes. This is normal.

### DMG too large to commit
Ignore `dist/` or use Git LFS. Prefer ignoring.

---

## Stage-by-Stage Run Commands (Copy/Paste)
```bash
# Stage 1/2 console
mvn clean package
java -cp target/classes com.johndoan.jobtracker.Main

# Stage 3/4/5 UI
mvn clean package
java -cp target/classes com.johndoan.jobtracker.UiMain
```
