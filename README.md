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