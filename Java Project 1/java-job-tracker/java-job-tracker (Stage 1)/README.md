# Job Application Tracker with Maven (Stage 1)

Small Java console app for keeping track of job applications.

Stage 1 focuses on clean, object-oriented design and a simple command-line workflow.
You can add applications, list them, filter by status, and update their status as your search progresses.

---

## 1. What Stage 1 covers

Stage 1 is intentionally minimal:

- Console-only UI (no GUI, no web).
- In-memory storage (data lives for the duration of the process).
- Clear domain model and layering:
  - `JobApplication` (domain object)
  - `ApplicationStatus` (enum)
  - `ApplicationRepository` (in-memory store)
  - `ApplicationService` (business logic)
  - `Main` (menu + user I/O)

Later stages (Stage 2+) can introduce CSV persistence and/or a richer UI, but they are out of scope for this README.

---

## 2. Features (Stage 1)

- Add a new job application with:
  - Company
  - Position / role title
  - Location
  - Status (`APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`, etc.)
  - Date applied
- List all applications currently in memory.
- Filter applications by status (e.g. show only `INTERVIEW`).
- Update the status of an existing application by ID.
- Simple text menu that loops until you choose to exit.

All data is stored in memory only for Stage 1. Once you exit the program, the data is cleared.

---

## 3. Tech stack

- **Language:** Java 17+ (11+ would also work if you adjust the `pom.xml` accordingly)
- **Build tool:** Maven
- **Runtime:** Console application (standard input/output)

---

## 4. Project structure (Stage 1)

```text
java-job-tracker/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── johndoan/
│       │           └── jobtracker/
│       │               ├── ApplicationRepository.java  # In-memory storage
│       │               ├── ApplicationService.java     # Business logic
│       │               ├── ApplicationStatus.java      # Enum of statuses
│       │               ├── JobApplication.java         # Domain model
│       │               └── Main.java                   # Entry point + menu
│       └── resources/                                  # (unused in Stage 1)
├── target/                                             # Maven build output
├── pom.xml                                             # Maven configuration
└── README.md
```

---

## 5. Getting started

### 5.1 Prerequisites

Make sure you have:

- **Java JDK** (17 recommended):

  ```bash
  java -version
  ```

- **Maven** 3.8+:

  ```bash
  mvn -v
  ```

Both commands should print version information. If `mvn` is not found, install Maven first.

---

## 6. Build

From the project root:

```bash
mvn clean compile
```

or, if you prefer to also build the JAR:

```bash
mvn clean package
```

This will create compiled classes under `target/classes` and a JAR under `target/` (e.g. `target/java-job-tracker-1.0-SNAPSHOT.jar`).

> Note: In Stage 1, the JAR may not yet have a `Main-Class` manifest entry.
> The simplest, always-works way to run is to use the compiled classes on the classpath (see below).

---

## 7. Run the console app

### Option A – Run from compiled classes

After `mvn clean compile`:

```bash
java -cp target/classes com.johndoan.jobtracker.Main
```

This runs the `Main` class directly from the compiled output.

### Option B – Run via Maven exec plugin (if configured)

If you decide to add the Maven Exec plugin to `pom.xml`, you can also run:

```bash
mvn compile exec:java -Dexec.mainClass="com.johndoan.jobtracker.Main"
```

(Only works if `exec-maven-plugin` is configured in your `pom.xml`.)

---

## 8. What the console looks like

Once started, you will see a menu similar to:

```text
=== Job Application Tracker (Java, Console) ===

Choose an option:
  1) Add application
  2) List all applications
  3) List applications by status
  4) Update application status
  0) Exit
Your choice:
```

Sample flow:

1. Press **1** to add a new application and follow the prompts.
2. Press **2** to list everything you have entered in this session.
3. Press **3** to filter by a particular status.
4. Press **4** to update the status of an application by its ID.
5. Press **0** to exit.

Because Stage 1 is in-memory only, data is cleared when you exit.

---
## 9. Summary

Stage 1 of the Java Job Tracker is about:

- Practicing **OOP** and **layered design** in Java.
- Getting comfortable with **Maven** as a build tool.
- Building a small but concrete console app that you can demonstrate and evolve in later stages.
