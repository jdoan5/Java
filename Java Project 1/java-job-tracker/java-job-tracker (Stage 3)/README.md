# Job Application Tracker (Stage 3 — Desktop UI + CSV)

A personal job application tracker written in Java.  
**Stage 3** introduces a **Swing desktop UI** while keeping **CSV** as the persistence format.

---

## What Stage 3 adds

- **Swing UI** (`UiMain`) with:
  - Table view of applications
  - Add application
  - Update status
  - Delete selected row
  - Filter by status
- **CSV import/export** from the UI
  - Default file path: `CSV/job_applications.csv`
  - You can browse/select a different file (e.g., `CSV/job_applications2.csv`)

> Notes on persistence:
> - Java does **not** “remember” data after you close the app unless you **save** it somewhere (CSV, DB, etc.).
> - In Stage 3, persistence is **CSV-based**. If you close the UI without exporting/saving, those in‑memory changes are not stored.

---

## Requirements

- Java **17+**
- Maven **3.8+**
- macOS / Windows / Linux (UI uses Swing)

---

## Project structure (typical)

```
src/main/java/com/johndoan/jobtracker/
  ApplicationRepository.java
  ApplicationService.java
  ApplicationStatus.java
  JobApplication.java
  Main.java          # Console entry point (Stage 1/2)
  UiMain.java        # Swing UI entry point (Stage 3)

src/main/java/com/johndoan/jobtracker/ui/
  JobTrackerFrame.java

CSV/
  job_applications.csv
```

---

## Build

From the Stage 3 project root:

```bash
mvn clean package
```

This will compile the project and create jars in `target/`.

---

## Run (UI)

### Option A: Run from compiled classes (simple)

```bash
mvn -q -DskipTests package
java -cp target/classes com.johndoan.jobtracker.UiMain
```

### Option B: Run the packaged “UI jar”

Depending on your `pom.xml` / shade configuration, you will typically get a UI jar like one of these:

- `target/java-job-tracker-1.0-SNAPSHOT-ui.jar`
- `target/java-job-tracker-1.0-SNAPSHOT-shaded.jar`

List candidates:

```bash
ls -la target | grep -E "ui\.jar|shaded\.jar|SNAPSHOT\.jar"
```

Then run the correct file:

```bash
java -jar target/<THE_UI_JAR_NAME>.jar
```

If you see `Error: Unable to access jarfile ...`, verify the filename actually exists under `target/` and that your shell path quoting is correct.

---

## Run (Console)

Stage 3 still includes the console app:

```bash
java -cp target/classes com.johndoan.jobtracker.Main
```

---

## Using CSV in the UI

1. Start the UI.
2. Set **CSV path** (defaults to `CSV/job_applications.csv`).
3. Click:
  - **Load**: read records from the selected CSV into the table.
  - **Save**: export current table to the selected CSV.

### Multiple CSV files

Yes, you can keep multiple CSV files (e.g., `job_applications.csv`, `job_applications2.csv`).  
The UI loads/saves whichever path you select; it does not automatically merge multiple files.

---

## Common issues (and fixes)

### 1) “class X is public, should be declared in a file named X.java”
In Java, a `public` class must match the filename exactly.  
Fix by renaming the file or changing the class visibility.

### 2) Duplicate class errors
You likely have two copies of the same class name under the same package.  
Remove/rename the duplicate.

### 3) “reference to println is ambiguous”
Usually happens with method references like `System.out::println` when the compiler cannot infer the type.  
Use an explicit lambda instead, for example:
```java
apps.forEach(a -> System.out.println(a));
```

### 4) “No applications yet” even though a CSV exists
Stage 3 does not automatically load CSV on startup (unless you explicitly coded that).  
Click **Load** (and ensure the CSV path is correct).

---
