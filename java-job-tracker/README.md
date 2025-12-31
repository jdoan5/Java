# Java Job Application Tracker (Stage 1)

Small console app to keep track of job applications while I learn modern Java, Maven, and IntelliJ IDEA.

Stage 1 focuses on:

- A clean **console UI**
- Basic **CRUD-like** operations (add, list, filter, update)
- Simple **CSV export** of in-memory data

> Data is **in memory only** – when you exit the program, the list is cleared.  
> The CSV export is for reporting, not for re-loading (that’s a possible Stage 2).

---

## Features

**Core console actions**

- `1) Add application`
    - Company
    - Position
    - Location
    - Status: `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`
    - Date applied (defaults to **today** if left blank)

- `2) List all applications`
    - Shows each record with an auto-incremented ID and details

- `3) List applications by status`
    - Filter by one of the four statuses

- `4) Update application status`
    - Choose an application by ID and change its status

- `0) Exit`
    - Ends the program

**(Optional) Stage 1.5: CSV export**

If you’ve enabled `exportToCsv` in `ApplicationService` and wired it into the menu:

- `5) Export applications to CSV`
    - Writes a file like `job_applications.csv` with:
        - `id, company, position, status, appliedDate, notes` (notes can be empty)

---

## Tech Stack

- **Language:** Java 17+ (tested with Java 21 via IntelliJ SDK)
- **Build:** Maven
- **IDE:** IntelliJ IDEA (Ultimate)
- **Packaging:** Standard Maven layout  
  `src/main/java`, `src/main/resources`

---

## Project Structure

```text
java-job-tracker/
├─ pom.xml
└─ src
   └─ main
      ├─ java
      │  └─ com
      │     └─ johndoan
      │        └─ jobtracker
      │           ├─ Main.java
      │           ├─ JobApplication.java
      │           ├─ ApplicationStatus.java
      │           ├─ ApplicationRepository.java
      │           └─ ApplicationService.java
      └─ resources