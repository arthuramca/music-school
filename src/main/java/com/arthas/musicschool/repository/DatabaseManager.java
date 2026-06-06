package com.arthas.musicschool.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_DIR = System.getProperty("user.home") + "/music-school";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/school.db";

    private static Connection connection;

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Files.createDirectories(Paths.get(DB_DIR));
            } catch (IOException e) {
                throw new SQLException("Não foi possível criar o diretório do banco: " + DB_DIR, e);
            }
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initializeSchema(connection);
        }
        return connection;
    }

    private static void initializeSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    name            TEXT    NOT NULL,
                    cpf             TEXT    DEFAULT '',
                    birth_date      TEXT,
                    phone           TEXT    DEFAULT '',
                    email           TEXT    DEFAULT '',
                    address         TEXT    DEFAULT '',
                    instrument      TEXT    DEFAULT '',
                    level           TEXT    DEFAULT '',
                    teacher         TEXT    DEFAULT '',
                    start_date      TEXT,
                    monthly_fee     REAL    DEFAULT 0.0,
                    payment_due_day INTEGER DEFAULT 5,
                    status          TEXT    DEFAULT 'Ativo',
                    notes           TEXT    DEFAULT '',
                    photo_path      TEXT    DEFAULT ''
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payments (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id      INTEGER NOT NULL,
                    reference_month TEXT    NOT NULL,
                    amount          REAL    DEFAULT 0.0,
                    paid_date       TEXT    DEFAULT '',
                    status          TEXT    DEFAULT 'Pendente',
                    notes           TEXT    DEFAULT '',
                    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
                )
                """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lessons (
                    id           INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id   INTEGER NOT NULL,
                    lesson_date  TEXT    NOT NULL,
                    attended     INTEGER DEFAULT 1,
                    notes        TEXT    DEFAULT '',
                    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
                )
                """);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }
}
