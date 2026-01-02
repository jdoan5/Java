# Job Application Tracker (Stage 1)

A small Java console app for tracking job applications.

Stage 1 focuses on clean OOP structure and a simple command-line workflow (in-memory only).

---

## Features

- Add an application (company, position, location, status, date applied)
- List all applications
- Filter by status
- Update application status by ID

> Stage 1 stores data **in memory** only. When you exit the app, the data is lost.

---

## Tech

- Java 17
- Maven

---

## Project structure

```text
src/main/java/com/johndoan/jobtracker/
  Main.java
  JobApplication.java
  ApplicationStatus.java
  ApplicationRepository.java
  ApplicationService.java
```

---

## Build

From the project root:

```bash
mvn clean compile
```

(Optional) build the jar:

```bash
mvn clean package
```

---

## Run

Run from compiled classes:

```bash
java -cp target/classes com.johndoan.jobtracker.Main
```

---

## Console menu

```text
1) Add application
2) List all applications
3) List applications by status
4) Update application status
0) Exit
```
