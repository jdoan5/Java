# Java Projects Portfolio

Four Java projects, each built in numbered stages so the progression is visible: a console app
that grew into a packaged desktop application, a Spring Boot + Angular full-stack app, and an
OAuth2-secured REST API that ends up containerised and deployed on Google Cloud.

Every stage is a complete, self-contained snapshot rather than a diff, so you can open any one
of them and run it without rewinding the others.

**Live demo —** the Bookmark API is deployed on Cloud Run:

- Landing page: <https://bookmark-api-990816290294.us-central1.run.app/>
- Health: <https://bookmark-api-990816290294.us-central1.run.app/actuator/health>

It scales to zero, so the first request after an idle period may take a few seconds or return a
503 while a container starts. Try again and it comes up.

---

## What is here

| Project | What it is | Stages | Stack |
|---|---|---|---|
| [Bookmark API](#bookmark-api) | REST API with its own OAuth2 authorization server, containerised and deployed | 1–6 | Spring Boot 3.4.1, Spring Security, JPA, H2, Docker, Helm, Kubernetes, Cloud Run |
| [Helpdesk Ticket Triage](#helpdesk-ticket-triage) | Ticket CRUD API with an Angular front end | 1–4 | Spring Boot 3.5.16, JPA, H2, springdoc, Angular 20 |
| [Job Application Tracker](#job-application-tracker) | Console app → Swing desktop app → SQLite-backed, packaged as a `.dmg` | 1–5 | Java 21, Maven, Swing, SQLite via JDBC |
| [Java Interview](#java-interview) | Scratch pad for practice exercises. Not portfolio work | — | Plain Java |

**If you only have a few minutes,** open the Bookmark API at Stage 6. It is the most complete
piece of work here: a service that issues its own JWTs, validates them, enforces per-user
ownership, and ships as a non-root container with a Helm chart and a Cloud Run deployment script.

---

## Prerequisites

- **JDK 21.** Every Maven project targets `<java.version>21</java.version>`. A newer JDK on your
  `PATH` may work but is not what these were built against — export `JAVA_HOME` to a 21 if you
  hit compiler plugin errors.
- **Maven 3.9.x.** There is no Maven wrapper committed, so `mvn` must be on your `PATH`.
- **Node 20+ and npm**, only for the Angular front end in Helpdesk Stage 4.
- **Docker**, only for the Bookmark API container sections (Stages 5–6).
- A network connection on first build, so Maven can populate `~/.m2`.

> **Directory names contain spaces and parentheses.** Every `cd` below is written from the
> repository root and the paths are quoted. Keep the quotes or the commands will not run, and
> return to the repository root between blocks.

---

## Bookmark API

**Path:** `Java Project 3/Spring Boot Application/bookmark-api (Stage N)`

A bookmarking REST API that is also its own identity provider. It runs the Spring Authorization
Server and the resource server in the same application, so it issues the JWTs that it then
validates — useful for seeing both halves of OAuth2 without standing up Keycloak.

| Stage | What it adds |
|---|---|
| 1 | REST CRUD, in-memory store, no security |
| 2 | OAuth2 `client_credentials` — an app gets a token, no human involved |
| 3 | Persistence via Spring Data JPA and H2 in file mode |
| 4 | Browser login with `authorization_code` + PKCE, and per-user bookmark ownership |
| 5 | Dockerfile, Helm chart, Kubernetes manifests, GKE deploy script |
| 6 | Cloud Run deploy script, on top of everything in Stage 5 |

### Run it

```bash
cd "Java Project 3/Spring Boot Application/bookmark-api (Stage 6)"
mvn clean package
java -jar target/bookmark-api-1.0-SNAPSHOT.jar
```

Then open <http://localhost:8080/>. Two demo users are registered in memory, `john` and `jane`,
both with password `password` — throwaway credentials for a local demo, declared with `{noop}`
in `SecurityConfig` so it is obvious they are not hashed and not real.

Get a token and call the API:

```bash
TOKEN=$(curl -s -u bruno-client:bruno-secret \
  -d 'grant_type=client_credentials&scope=bookmark.read bookmark.write' \
  http://localhost:8080/oauth2/token | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/bookmarks
```

A [Bruno](https://www.usebruno.com/) collection is committed under
`bookmark-api (Stage N)/bruno/Bookmark API/`. It grows with the stages — Stage 1 predates
security and has no token request; token fetch appears at Stage 2, and bulk create and tag
filtering from Stage 3 on.

### Container and cluster

```bash
cd "Java Project 3/Spring Boot Application/bookmark-api (Stage 6)"
docker build -t bookmark-api:local .
docker run -p 8080:8080 bookmark-api:local
```

The image is a multi-stage build that ships only a JRE and the jar, runs as numeric `USER 1000`
so Kubernetes' `runAsNonRoot` can verify it, and sets `-XX:MaxRAMPercentage` so the JVM sizes its
heap from the container limit rather than the host's RAM. The Helm chart in `helm/bookmark-api/`
documents every value it accepts, and `deploy-gke.sh` / `deploy-cloudrun.sh` are the scripts
behind the live demo above.

---

## Helpdesk Ticket Triage

**Path:** `Java Project 2/Helpdesk Ticket Triage System/helpdesk-triage (Stage N)`

A support-ticket API — create a ticket, list tickets, update priority and status — with an
Angular front end added at Stage 4.

| Stage | What it adds |
|---|---|
| 1 | Domain model and project skeleton |
| 2 | REST controller, service layer, validation, springdoc/Swagger UI |
| 3 | JPA persistence with H2 |
| 4 | Angular 20 front end in `frontend/helpdesk-ui/`, backend moved under `backend/` |

### Run the backend

```bash
cd "Java Project 2/Helpdesk Ticket Triage System/helpdesk-triage (Stage 4)/Helpdesk Ticket Triage System/backend"
mvn clean package
java -jar target/helpdesk-triage-0.0.1-SNAPSHOT.jar
```

- API: <http://localhost:8080/api/tickets>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- H2 console: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:helpdesk`, user `sa`, no password)

The database is in-memory, so tickets are gone on restart. There is **no Spring Security in this
project** — the API and the H2 console are both open. That is fine bound to localhost and is the
main thing I would add before this went anywhere real.

### Run the front end

With the backend already running, in a second terminal:

```bash
cd "Java Project 2/Helpdesk Ticket Triage System/helpdesk-triage (Stage 4)/Helpdesk Ticket Triage System/frontend/helpdesk-ui"
npm install
npm start
```

Open <http://localhost:4200>. The page lists existing tickets and creates new ones.

**Use port 4200 specifically.** The front end calls the backend at the absolute URL in
`src/environments/environment.ts` (`http://localhost:8080`), and the backend's `CorsConfig`
allows exactly one origin, `http://localhost:4200`. Serve on any other port and every request
fails CORS. There is a `proxy.conf.json` in the project, but it is leftover CLI scaffolding —
nothing references it in `angular.json` or `package.json`, and the absolute `apiBaseUrl` would
bypass a dev-server proxy anyway.

---

## Job Application Tracker

**Path:** `Java Project 1/java-job-tracker/java-job-tracker (Stage N)`

Track job applications — company, position, location, status, date — as the app grows from a
console menu into a packaged desktop application.

| Stage | What it adds | Interface |
|---|---|---|
| 1 | Domain model, in-memory repository, service layer | Console |
| 2 | CSV export and reload on startup | Console |
| 3 | Swing desktop UI | Swing |
| 4 | SQLite persistence via JDBC | Swing |
| 5 | Search and sort, `.dmg` packaging | Swing |

```bash
cd "Java Project 1/java-job-tracker/java-job-tracker (Stage 2)"
mvn clean package
java -jar target/java-job-tracker-1.0-SNAPSHOT.jar
```

Stage 3 onward the jar launches the Swing UI: `Main` is a thin shim that immediately calls
`UiMain`, so there is no separate console command. The jar name changes by stage —
`java-job-tracker-1.0-SNAPSHOT.jar` for Stages 1–3, `-2.0.0-SNAPSHOT` for Stage 4 and
`-2.1.0-SNAPSHOT` for Stage 5.

CSV exports are written to the `CSV/` folder inside each stage.

---

## Java Interview

**Path:** `Java Interview/`

A scratch pad for interview practice, kept in the repository so the working notes are not lost.
It is a plain IntelliJ module with no `pom.xml`; JUnit is resolved from the jars committed in
`lib/`. `Solution.java` is a file-reading and number-parsing exercise and reads `numbers.txt`
from the working directory.

This is practice code, not portfolio work, and it is labelled that way deliberately.

---

## Repository layout

```text
.
├── Java Interview/                     # practice scratch pad (no Maven build)
│   ├── lib/                            # JUnit jars — this module's only classpath source
│   └── src/
├── Java Project 1/
│   └── java-job-tracker/
│       └── java-job-tracker (Stage 1..5)/
├── Java Project 2/
│   └── Helpdesk Ticket Triage System/
│       └── helpdesk-triage (Stage 1..4)/
│           └── Helpdesk Ticket Triage System/
│               ├── backend/            # Spring Boot API   (Stage 4 layout)
│               └── frontend/helpdesk-ui/   # Angular 20 app (Stage 4 only)
└── Java Project 3/
    └── Spring Boot Application/
        └── bookmark-api (Stage 1..6)/
            ├── bruno/                  # API request collection
            ├── helm/ k8s/              # Stages 5–6
            └── Dockerfile              # Stages 5–6
```

Each Maven project stands alone — there is no aggregator POM, so build them individually.

---

## Testing

Coverage is uneven, and this table is the honest version rather than the flattering one.

| Project | State |
|---|---|
| Bookmark API | Real coverage. Stage 6 runs **12 tests** — controller tests across the CRUD surface plus a context load. |
| Helpdesk — Angular front end | Real coverage. **14 tests** — the service asserts verb, URL and body for all six HTTP methods; the component covers load-on-init, both error paths, and form reset after create. |
| Helpdesk — backend | **None.** `TicketControllerTest` and `TicketServiceTest` exist at every stage but are empty four-line class shells with no `@Test` methods, so `mvn test` reports `Tests run: 0`. |
| Job Tracker | Stage 5 has two real test classes, but they sit under `src/src/test/java/` — a nested `src` — so Maven never picks them up. |

**No workflow builds this repository or runs these tests.** GitHub's CodeQL default setup does
scan every push and pull request — it is configured in repository settings, not by a workflow
file — but nothing compiles the Maven projects or executes a test suite automatically. Adding a
build workflow is the obvious next step.

---

## License

MIT — see [LICENSE](LICENSE). Copyright © 2025 John Doan.
