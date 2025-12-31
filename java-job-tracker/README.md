# Java Job Tracker

Small Java console app for keeping track of your job applications.

You can add applications (company, role, status, notes), list them, and update their status as your search progresses. All data is stored in a simple CSV file so it’s easy to back up or inspect in a spreadsheet.

---

## Features

- Add a new job application (company, job title, source, status, notes, etc.).
- Update the status of an existing application  
  (e.g. `APPLIED → PHONE_SCREEN → INTERVIEW → OFFER → REJECTED`).
- List all applications or filter by status.
- CSV-backed storage (no database required).
- Simple console menu, suitable for quick day-to-day use.

---

## Tech stack

- **Language:** Java (11+ recommended)
- **Build tool:** Maven
- **Persistence:** CSV file under `CSV/`
- **Packaging:** standard Maven JAR (`target/*.jar`)

---

## Project structure

```text
java-job-tracker/
├── CSV/                        # CSV storage folder
│   └── applications.csv        # Created/updated at runtime
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── johndoan/
│       │           └── jobtracker/
│       │               ├── ApplicationRepository.java  # CSV load/save logic
│       │               ├── ApplicationService.java     # Business logic
│       │               ├── ApplicationStatus.java      # Enum of statuses
│       │               ├── JobApplication.java         # Domain model
│       │               └── Main.java                   # Entry point (menu)
│       └── resources/          # (unused for now, reserved for future config)
├── target/                     # Maven build output (generated)
├── pom.xml                     # Maven configuration
└── README.md
```

The `target/classes/com/johndoan/jobtracker/...` tree is Maven’s compiled output that mirrors `src/main/java`.

---

## Getting started

### 1. Prerequisites

Make sure you have:

- **Java JDK** 11 or 17 installed
  ```bash
  java -version
  ```
- **Maven** 3.8+ installed
  ```bash
  mvn -v
  ```

### 2. Clone the repo

```bash
git clone https://github.com/<your-username>/java-job-tracker.git
cd java-job-tracker
```

---

## Build

From the project root:

```bash
mvn clean package
```

If everything compiles, Maven will produce a JAR under `target/`, e.g.:

```text
target/java-job-tracker-1.0-SNAPSHOT.jar
```

---

## Run

### Option A – Run the packaged JAR

```bash
java -jar target/java-job-tracker-1.0-SNAPSHOT.jar
# or if the version changes:
java -jar target/java-job-tracker-*.jar
```

### Option B – Run via Maven

```bash
mvn clean compile exec:java -Dexec.mainClass="com.johndoan.jobtracker.Main"
```

> Adjust the `exec.mainClass` if you change the package or class name.

Once started, you will see a menu similar to:

```text
==== Job Tracker ====
1) Add application
2) List applications
3) List applications by status
4) Update application status
5) Exit
Choose an option:
```

Follow the prompts to input data.

---

## Data storage (CSV)

- All applications are stored in `CSV/applications.csv`.
- If `applications.csv` does not exist on first run, `ApplicationRepository` will create it the first time you save data.
- Because storage is just a CSV file, you can:
  - Open it with Excel/Sheets to review your pipeline.
  - Back it up or version-control it easily.

---

## Typical workflow

1. Start the app (`java -jar ...`).
2. Choose **1** to add each application as you apply.
3. Choose **2** or **3** to review what is in the pipeline.
4. Choose **4** to move an application to the next status when you hear back.
5. Choose **5** to exit. Data is preserved in `CSV/applications.csv`.

---

## Possible future extensions

- Add more filters (by company, date range, job title).
- Compute simple stats: applications per week, offers vs rejections, etc.
- Colorize the console output for better UX.
- Replace the CSV layer with a database (H2/PostgreSQL) while keeping the same service layer.

---

Feel free to tweak wording (e.g. menu options) if your `Main` class prints slightly different labels, but this README now matches your current package structure and build setup.
