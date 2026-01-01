-- Stage 5 schema (SQLite)
-- Table name is `applications` (matches JdbcApplicationRepository).

CREATE TABLE IF NOT EXISTS applications (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  company      TEXT NOT NULL,
  position     TEXT NOT NULL,
  location     TEXT NOT NULL,
  status       TEXT NOT NULL,
  date_applied TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_applications_status ON applications(status);
CREATE INDEX IF NOT EXISTS idx_applications_company ON applications(company);
CREATE INDEX IF NOT EXISTS idx_applications_position ON applications(position);
