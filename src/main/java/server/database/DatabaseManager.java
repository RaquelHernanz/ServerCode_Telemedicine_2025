package server.database;

import java.sql.*;

/** Conexión con la base de datos (SQLite) */

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:telemedicina.db";
    private static Connection conn;

    /** Abre la conexión (si no estaba abierta) y crea tablas */
    public static void connect() {
        try {
            if (conn == null || conn.isClosed()) { // si está cerrado
                conn = DriverManager.getConnection(URL); // abre base de datos
                System.out.println("[DB] Connected to SQLite");

                // PRAGMAs para que SQLite sea más fiable y seguro
                try (Statement s = conn.createStatement()) {
                    s.execute("PRAGMA foreign_keys = ON;"); // activa integridad referencial
                    s.execute("PRAGMA journal_mode = WAL;");   // mejor rendimiento cuando hay lecturas y escrituras a la vez
                    s.execute("PRAGMA busy_timeout = 5000;");  // 5s de espera si está bloqueada --> lock
                }

                createTables();
            }
        } catch (SQLException e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
        }
    }

    /** Devuelve la conexión actual (si está o no activa) */
    public static Connection get() { return conn; }

    /** Cierra la conexión si está abierta */
    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException ignored) {}
    }

    /** Crea el esquema mínimo al iniciar la conexión */
    private static void createTables() {
        String patients = """
          CREATE TABLE IF NOT EXISTS patients(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT, surname TEXT, email TEXT UNIQUE,
            password TEXT, dob TEXT, sex TEXT, phone TEXT
          );""";

        String symptoms = """
          CREATE TABLE IF NOT EXISTS symptoms(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER,
            description TEXT,
            timestamp TEXT,
            FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
          );""";

        String measurements = """
          CREATE TABLE IF NOT EXISTS measurements(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id INTEGER,
            type TEXT,          -- ECG/EDA
            started_at TEXT,    -- inicio de la toma
            file_path TEXT,     -- ruta del CSV
            FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
          );""";

        // Índices útiles
        String idxEmail = "CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);";
        String idxSymptomsPid = "CREATE INDEX IF NOT EXISTS idx_symptoms_pid ON symptoms(patient_id);";
        String idxMeasPid = "CREATE INDEX IF NOT EXISTS idx_measurements_pid ON measurements(patient_id);";

        try (Statement st = conn.createStatement()) {
            st.execute(patients);
            st.execute(symptoms);
            st.execute(measurements);
            st.execute(idxEmail);
            st.execute(idxSymptomsPid);
            st.execute(idxMeasPid);
        } catch (SQLException e) {
            System.err.println("[DB] Schema error: " + e.getMessage());
        }
    }
}