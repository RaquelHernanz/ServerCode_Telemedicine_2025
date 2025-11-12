package server.database;

import java.sql.*;

/** Conexión con la base de datos (SQLite) */

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:telemedicina.db"; // fichero .db en la raíz
    private static Connection conn;

    // Conecta (si no está ya conectada), activa PRAGMAs SQLite y crea tablas.
    public static void connect() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(URL);
                System.out.println("[DB] Connected to SQLite");

                // PRAGMAs útiles para SQLite
                try (Statement s = conn.createStatement()) {
                    s.execute("PRAGMA foreign_keys = ON;"); // para la seguridad y fiabilidad
                    s.execute("PRAGMA journal_mode = WAL;"); // para que rinda mejor cuando se lea y escriba a la vez
                    s.execute("PRAGMA busy_timeout = 5000;"); // locl --> bloqueo de 5s
                }
                createTables(); // crea tablas al abrir la conexión
            }
        } catch (SQLException e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
        }
    }

    // Devuelve la conexión actual, si está abierta o no
    public static Connection get() { return conn; }

    // Cierra la conexión si está abierta
    public static void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[DB] Connection closed.");
            }
        } catch (SQLException ignored) {}
    }

    // Crea esquema mínimo (patients, symptoms, measurements)
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
            type TEXT,
            started_at TEXT,
            file_path TEXT,
            FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
          );""";

        String idxEmail = "CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);";
        String idxSymPt = "CREATE INDEX IF NOT EXISTS idx_symptoms_pid ON symptoms(patient_id);";
        String idxMeaPt = "CREATE INDEX IF NOT EXISTS idx_measurements_pid ON measurements(patient_id);";

        try (Statement st = conn.createStatement()) {
            st.execute(patients);
            st.execute(symptoms);
            st.execute(measurements);
            st.execute(idxEmail);
            st.execute(idxSymPt);
            st.execute(idxMeaPt);
        } catch (SQLException e) {
            System.err.println("[DB] Schema error: " + e.getMessage());
        }
    }
}