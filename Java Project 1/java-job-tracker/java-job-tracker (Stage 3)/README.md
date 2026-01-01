# Job Application Tracker with Maven (Stage 3 — Desktop UI + CSV Persistence)

A small **Java + Maven** project that evolves across stages:
- **Stage 1:** Console-based tracker (OOP + layered design)
- **Stage 2:** Adds **CSV persistence** so records survive app restarts
- **Stage 3 (this folder):** Adds a **Swing desktop UI** with Load/Save + CRUD actions

This project is intentionally **local-first**: it stores data in a CSV on your machine (no database required).

---

## What Stage 3 adds

### Desktop UI (Swing)
- Table view of job applications
- Add / update status / delete application
- Status filter (e.g., APPLIED / INTERVIEW / OFFER / REJECTED)
- Validation + friendly UI messages

### CSV persistence (local)
- A CSV path field in the UI:
  - Default: `CSV/job_applications.csv`
  - You can change it to: `CSV/job_applications_2.csv`, etc.
- Buttons:
  - **Load** (reads records from the CSV into memory/table)
  - **Save** (writes the current in-memory list to the CSV)

---

## Tech stack
- Java 17+
- Maven
- Swing (desktop UI)
- CSV persistence via `java.nio.file.*`

---

## Project structure (typical)
> Names may vary slightly depending on your refactor, but the responsibilities should match.

- `src/main/java/com/johndoan/jobtracker/`
  - `UiMain.java` — UI entry point
  - `Main.java` — Stage 1 console entry point (kept for reference)
  - `JobApplication.java` — domain model
  - `ApplicationStatus.java` — enum
  - `ApplicationRepository.java` — in-memory store + CSV load/save helpers
  - `ApplicationService.java` — business logic (calls repository)
- `src/main/java/com/johndoan/jobtracker/ui/`
  - `JobTrackerFrame.java` — Swing UI window

- `CSV/` — default folder for CSV exports

---

## Data format

Default CSV location: `CSV/job_applications.csv`

Expected columns:

```csv
id,company,position,location,status,appliedDate
1,Acme,Analyst,Remote,APPLIED,2025-12-31
2,Foobar Inc,Engineer,NYC,INTERVIEW,2025-12-20
```

Notes:
- `appliedDate` uses `YYYY-MM-DD`
- IDs should remain stable across loads/saves
- When loading, the repository should set `nextId` to `max(existingId)+1` so new rows get unique IDs

---

## How to run (Mac / Linux / Windows)

### Option A: Run from IDE (recommended during development)
1. Open the project in IntelliJ IDEA.
2. Run:
  - `com.johndoan.jobtracker.UiMain` (Stage 3 Swing UI)

### Option B: Run with Maven (CLI)
From the Stage 3 folder:

```bash
mvn clean package
```

Then run the UI main class from compiled classes:

```bash
java -cp target/classes com.johndoan.jobtracker.UiMain
```

> If you rely on external dependencies later, you’ll want a runnable “fat jar” (see next section).

---

## Build a runnable JAR (single command)

If `java -jar target/...jar` fails with “no main manifest attribute”, your JAR does not have a `Main-Class` entry.

Recommended approach: Maven Shade Plugin (fat JAR). Add this to your `pom.xml` under `<build><plugins>`:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.1</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <createDependencyReducedPom>false</createDependencyReducedPom>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>com.johndoan.jobtracker.UiMain</mainClass>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Rebuild:

```bash
mvn clean package
```

Then run the generated JAR (name depends on your Maven config):

```bash
ls target
java -jar target/java-job-tracker-1.0-SNAPSHOT.jar
```

If you prefer a dedicated “all” jar name, set a classifier in shade config (e.g., `-all`) and run that file.

---

## Make it a desktop app (macOS) with `jpackage`

Once you have a runnable fat jar, you can create a native macOS app bundle using the JDK tool `jpackage`.

Example:

```bash
jpackage   --name JobTracker   --input target   --main-jar java-job-tracker-1.0-SNAPSHOT.jar   --type app-image
```

Outputs an app-image folder you can double-click. For a distributable installer, use `--type dmg` (requires signing/notarization for distribution).

---

## Troubleshooting

### “No applications yet” but the CSV file exists
That means the app hasn’t loaded it into memory. Use:
- **Load** button in the UI, or
- Ensure your startup flow calls the repository’s load method.

### “CSV path not configured for repository”
Ensure the repository has:
- a default path (`CSV/job_applications.csv`), or
- the UI passes the path to `load` / `save` methods explicitly.

### IDs all show as 0
This typically means IDs are not being set when:
- creating new `JobApplication` objects, or
- loading from CSV

Fix: the repository should assign IDs (incrementing `nextId`) and persist them when saving.

---

