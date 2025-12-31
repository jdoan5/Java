# Java Projects Portfolio

This folder collects my Java learning and portfolio projects.  
Each project focuses on a specific skill area: clean OOP design, basic persistence, console UX, and (later) simple GUI or API work.

## 1. Goals of This Repo

- Practice **core Java** (classes, interfaces, enums, collections, exceptions).
- Learn how to structure small apps with **layers** (domain, service, persistence, UI).
- Use **Maven** for builds, dependencies, and packaging.
- Experiment with basic **persistence patterns**:
    - In-memory only
    - CSV export/import
    - (Planned) simple database or file storage
- Build projects that are:
    - Easy to re-run from the command line
    - Easy to extend in future stages (Stage 1 → Stage 2 → Stage 3)

---

## 2. Projects in This Folder

### 2.1 Job Application Tracker (Console, Maven)

**Directory:** `java-job-tracker`  
**Status:**
- Stage 1 – In-memory tracker + console UI
- Stage 2 – CSV export & reload on startup
- Stage 3 – (Planned) richer UI or REST API

**What it does:**

A small console app to track job applications. You can:

- Add applications (company, position, location, status, date).
- List all applications.
- Filter by status (APPLIED / INTERVIEW / OFFER / REJECTED).
- Update the status as your process moves forward.
- (Stage 2) Export applications to CSV and reload them on startup.

**What I’m practicing:**

- Domain modeling with a `JobApplication` class and `ApplicationStatus` enum.
- Service + repository pattern:
    - `ApplicationService` for business logic.
    - `ApplicationRepository` for storage (in-memory + CSV).
- Console UI loop in `Main` (menu, input validation, cheap but clear UX).
- Using **Maven** to compile, test, and package the app.

#### How to run (Job Application Tracker)

From `java-job-tracker/`:

```bash
# 1) Build
mvn clean package

# 2) Run (Stage 1 / Stage 2)
java -cp target/java-job-tracker-1.0-SNAPSHOT.jar com.johndoan.jobtracker.Main
```

CSV files (for Stage 2) are stored under:

```text
java-job-tracker/CSV/job_applications.csv
```

---

## 3. Tech Stack

Across these Java projects I focus on a small, practical toolset:

- **Language:** Java 17+
- **Build:** Maven
- **IDE:** IntelliJ IDEA
- **Testing:** (Planned) JUnit for unit tests
- **Persistence (learning-focused):**
    - In-memory collections (Lists/Maps)
    - CSV export/import
    - (Future) lightweight database or file-based storage
- **UI styles:**
    - Console menus (Stage 1–2)
    - (Future) simple GUI or web front ends for Stage 3 projects

---

## 4. Repository Layout

A typical project in this folder follows:

```text
project-name/
  pom.xml                     # Maven build file
  src/
    main/
      java/
        com/yourname/project/ # Java package
          Main.java           # entry point
          ...                 # domain, service, repository classes
      resources/              # (optional) config files, templates
    test/
      java/                   # (planned) unit tests
  CSV/                        # (optional) CSV exports/imports
  README.md                   # project-specific readme
```

This matches the standard Maven layout and makes it easy to open any project in IntelliJ or build from the command line.

---

## 5. How to Work With These Projects

1. **Clone the repo:**

   ```bash
   git clone https://github.com/jdoan5/Java.git
   cd Java
   ```

2. **Open a project in IntelliJ IDEA:**
    - `File → Open…`
    - Select the project folder (for example, `java-job-tracker/`).
    - Let IntelliJ import it as a Maven project.

3. **Build with Maven:**

   ```bash
   mvn clean package
   ```

4. **Run from command line:**

   ```bash
   java -cp target/<jar-name>.jar com.yourpackage.Main
   ```

   Replace `com.yourpackage.Main` with the actual main class for that project.

---

## 6. Roadmap

Planned improvements across Java projects:

- Add **JUnit tests** for service and repository layers.
- Introduce **logging** instead of only `System.out.println`.
- For the Job Tracker:
    - Stage 3 – Simple UI (JavaFX or web) that wraps the same core logic.
    - Optional: switch from CSV to a small embedded DB (like H2 or SQLite via JDBC).
- Add more small Java projects (e.g., CLI utilities, small REST APIs) that reuse the same design patterns.

---

If you’re reviewing this as part of my portfolio and want to see a specific project or pattern (e.g., unit tests, REST API, or DB integration), feel free to jump into the individual project folders and open their `README.md` files.
