helpdesk-triage-stage1/
├─ README.md
├─ pom.xml
├─ .gitignore
├─ data/
│  └─ helpdesk.db                # local SQLite file (generated at runtime or shipped empty)
├─ docs/
│  ├─ screenshots/               # screenshots for portfolio
│  └─ notes.md                   # design notes / stage notes
└─ src/
├─ main/
│  ├─ java/
│  │  └─ com/johndoan/helpdesk/
│  │     ├─ App.java                          # JavaFX entrypoint
│  │     ├─ config/
│  │     │  └─ AppConfig.java                 # paths, db location, constants
│  │     ├─ db/
│  │     │  ├─ Db.java                        # connection factory (SQLite)
│  │     │  ├─ Schema.java                    # creates tables / migrations
│  │     │  └─ DaoException.java              # DB exception wrapper
│  │     ├─ model/
│  │     │  ├─ Ticket.java                    # entity
│  │     │  ├─ TicketStatus.java              # OPEN, IN_PROGRESS, RESOLVED, CLOSED
│  │     │  └─ Priority.java                  # LOW, MEDIUM, HIGH (optional)
│  │     ├─ dao/
│  │     │  ├─ TicketDao.java                 # CRUD + search
│  │     │  └─ SqlTicketDao.java              # JDBC implementation
│  │     ├─ service/
│  │     │  ├─ TicketService.java             # workflow rules
│  │     │  └─ SearchService.java             # basic keyword search orchestration
│  │     ├─ ui/
│  │     │  ├─ controller/
│  │     │  │  ├─ MainController.java         # main window controller
│  │     │  │  ├─ TicketFormController.java   # create/edit ticket
│  │     │  │  └─ TicketDetailController.java # view + status transitions
│  │     │  ├─ view/
│  │     │  │  ├─ main.fxml                   # layout
│  │     │  │  ├─ ticket_form.fxml
│  │     │  │  └─ ticket_detail.fxml
│  │     │  └─ UiState.java                   # selected ticket, filters, etc.
│  │     └─ util/
│  │        ├─ Validators.java                # input validation
│  │        └─ DateTimeUtil.java              # formatting helpers
│  └─ resources/
│     ├─ app.css
│     └─ logback.xml                          # optional logging
└─ test/
└─ java/
└─ com/johndoan/helpdesk/
├─ TicketServiceTest.java
└─ SqlTicketDaoTest.java