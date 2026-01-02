# Job Application Tracker with Maven (Stage 2 – CSV persistence)

Small Java console app for keeping track of job applications, with a simple
**load / save to CSV** layer so your data survives between runs.

Stage 2 builds directly on the Stage 1 design (same domain model, same console
menu) and adds file-based persistence.

---

## 1. What Stage 2 adds

Stage 1 was intentionally in‑memory only: once you exited the program, all
applications were lost.

Stage 2 keeps that same workflow but introduces a very small persistence layer:

- On startup, the repository **loads existing applications from a CSV file**
  (for example `CSV/job_applications.csv`) if it exists.
- While the app is running, everything still lives in memory for fast access.
- From the menu, you can **export all current applications back to CSV**, so
  the next run can pick them up.
- CSV lives alongside the project so it is easy to inspect, back up, or reset.

Non‑goals for Stage 2:

- No database (that would be Stage 3+).
- No GUI or web API – the UI is still 100% console‑based.
- No complex schema migrations; the CSV format is intentionally simple.

---

## 2. Features (Stage 2)

Everything from Stage 1, plus CSV persistence:

- Add a new job application with:
  - Company
  - Position / role title
  - Location
  - Status (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`)
  - Date applied
- List all applications currently in memory (including those loaded from CSV
  on startup).
- Filter applications by status (e.g. show only `INTERVIEW`).
- Update the status of an existing application by ID.
- **Export all applications to CSV** via a menu option.
- **Reload saved applications on the next run** if the CSV file is present.

Typical usage pattern:

1. Start the program – it will attempt to load `CSV/job_applications.csv`.
2. Add or update applications during your session.
3. Choose _“Export applications to CSV”_ before exiting.
4. Next time you run the program, your exported rows are loaded back in.

---

## 3. Tech stack

- **Language:** Java 17+
- **Build tool:** Maven
- **Runtime:** Console application (standard input/output)
- **Persistence:** Plain CSV file on disk

---

## 4. Project structure (Stage 2)

The core layout remains the same as Stage 1, with a dedicated CSV folder used
for exports/imports:

```text
java-job-tracker (Stage 2)/
├── CSV/
│   └── job_applications.csv                 # Saved applications (created at runtime)
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── johndoan/
│       │           └── jobtracker/
│       │               ├── ApplicationRepository.java  # In‑memory store + CSV load/save
│       │               ├── ApplicationService.java     # Business logic + export helper
│       │               ├── ApplicationStatus.java      # Enum of statuses
│       │               ├── JobApplication.java         # Domain model
│       │               └── Main.java                   # Entry point + menu
│       └── resources/                                  # (unused)
├── target/                                             # Maven build output
├── pom.xml                                             # Maven configuration
└── README.md
```

The **Stage 2 repository** is responsible for:

- Assigning incremental IDs for in‑memory applications.
- Loading initial data from `CSV/job_applications.csv` (if present).
- Writing rows back out to CSV when requested by the service.

---

## 5. CSV format

The CSV format is intentionally straightforward and “human readable”:

```csv
id,company,position,location,status,appliedDate
1,DemoCo,Backend Engineer,Remote,APPLIED,2025-12-31
2,AnotherCo,Data Analyst,NYC,INTERVIEW,2026-01-02
```

- `id` is an integer assigned by the repository.
- `status` matches the `ApplicationStatus` enum.
- `appliedDate` is stored as `YYYY-MM-DD`.

The loader in `ApplicationRepository` takes care of parsing these rows back
into `JobApplication` objects on startup.

If you ever want to “reset” your data, simply delete the CSV file – the app
will start with an empty in‑memory list again.

---

## 6. Build

From the Stage 2 project root (where the `pom.xml` lives):

```bash
mvn clean compile
```

or, to build the JAR:

```bash
mvn clean package
```

Maven will place compiled classes in `target/classes` and the JAR under
`target/` (for example `target/java-job-tracker-1.0-SNAPSHOT.jar`).

---

## 7. Run the console app

After a successful compile:

```bash
java -cp target/classes com.johndoan.jobtracker.Main
```

When the app starts, you should see something like:

```text
=== Job Application Tracker (Java, Console) ===
Loaded 3 application(s) from CSV/job_applications.csv

Choose an option:
  1) Add application
  2) List all applications
  3) List applications by status
  4) Update application status
  5) Export applications to CSV
  0) Exit
Your choice:
```

Use the menu options as follows:

- **1 – Add application**: interactively enter company, position, location,
  status, and applied date.
- **2 – List all applications**: see everything currently in memory.
- **3 – List applications by status**: filter by `APPLIED`, `INTERVIEW`,
  `OFFER`, or `REJECTED`.
- **4 – Update application status**: select an application by its ID and set a
  new status.
- **5 – Export applications to CSV**: write the current in‑memory list to
  `CSV/job_applications.csv`.
- **0 – Exit**: close the app; you can safely restart later and reload from CSV.

---

## 8. Notes

Stage 2 shows a simple but realistic pattern: **keep the domain model and
service layer clean, and bolt persistence on via a repository**.

For now, Stage 2 provides a compact demo of:

- Java + Maven
- Layered design (model / repository / service / UI)
- File-based persistence that keeps console workflows simple but durable.
