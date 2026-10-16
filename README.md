# MotorPH Payroll System — IAS2 · H4101 Group 21

Java Swing payroll system with a MySQL backend. Continuation of the AOOP project,
now focused on security hardening for IAS 2.

## Prerequisites

- JDK 21
- MySQL Server and MySQL Workbench
- VS Code with the Extension Pack for Java
- Git

## 1. Clone

    git clone https://github.com/kjudge-zxc/IAS2_motorph.git

## 2. Set up the database

In MySQL Workbench, connect as root and run:

    CREATE DATABASE payrollsystem_db;

Then import the SQL dump from the group's Google Drive folder:
Server > Data Import > Import from Self-Contained File, with
`payrollsystem_db` as the target schema.

## 3. Set your MySQL root password

The app connects as `root` with a password that is currently hardcoded in the
source. Match it instead of editing the code:

    ALTER USER 'root'@'localhost' IDENTIFIED BY 'Mmdc_2025*';
    FLUSH PRIVILEGES;

Connection target is `jdbc:mysql://localhost:3306/payrollsystem_db`.

## 4. Run

Open the project folder in VS Code and wait for the Java extension to finish
indexing (watch the status bar). Then open `src/gui/Login.java` and click Run
above the `main` method.

Dependencies are committed in `lib/` and already wired up in
`.vscode/settings.json`. Nothing to download or configure.


### Apply the IAS 2 control scripts

The Google Drive dump predates the IAS 2 security controls, so run both
scripts in `db/` after importing it. In Workbench: File > Open SQL Script,
then click the lightning bolt to execute.

    db/01_create_audit_log.sql      creates the audit_log table
    db/02_add_lockout_columns.sql   adds failedAttempts and lockedUntil to employee

Confirm both applied:

    SHOW TABLES LIKE 'audit_log';
    SHOW COLUMNS FROM employee WHERE Field IN ('failedAttempts', 'lockedUntil');

The second should return two rows.

Without these the app still runs and logins still work, but audit logging and
account lockout are silently inactive — the errors appear only in the console.

Note: `02` can only be applied once. Re-running it returns "Duplicate column
name", which is harmless and just means it is already in place.