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
            password TEXT, dob TEXT, sex TEXT, phone TEXT,
            doctor_id INTEGER,  
            FOREIGN KEY(doctor_id) REFERENCES doctors(id) ON DELETE SET NULL
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

        String doctors = """
          CREATE TABLE IF NOT EXISTS doctors(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT, surname TEXT, email TEXT UNIQUE,
            password TEXT, phone TEXT
            );""";

        // NUEVA TABLA DE CITAS
        String appointments = """
      CREATE TABLE IF NOT EXISTS appointments(
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        doctor_id INTEGER NOT NULL,
        patient_id INTEGER NOT NULL,
        datetime TEXT NOT NULL,
        message TEXT,
        FOREIGN KEY(doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
        FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
      );""";

        String messages = """
    CREATE TABLE IF NOT EXISTS messages(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    doctor_id INTEGER NOT NULL,
    patient_id INTEGER NOT NULL,
    sender_role TEXT NOT NULL, -- 'DOCTOR' o 'PATIENT'
    timestamp TEXT NOT NULL,
    text TEXT NOT NULL,
    FOREIGN KEY(doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    FOREIGN KEY(patient_id) REFERENCES patients(id) ON DELETE CASCADE
  );""";

        String idxEmail = "CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);";
        String idxSymPt = "CREATE INDEX IF NOT EXISTS idx_symptoms_pid ON symptoms(patient_id);";
        String idxMeaPt = "CREATE INDEX IF NOT EXISTS idx_measurements_pid ON measurements(patient_id);";
        String idxDoctorEmail = "CREATE INDEX IF NOT EXISTS idx_doctors_email ON doctors(email);";
        String idxAppDoctor   = "CREATE INDEX IF NOT EXISTS idx_appointments_did ON appointments(doctor_id);";
        String idxAppPatient  = "CREATE INDEX IF NOT EXISTS idx_appointments_pid ON appointments(patient_id);";
        String idxMsgDoctor  = "CREATE INDEX IF NOT EXISTS idx_messages_did ON messages(doctor_id);";
        String idxMsgPatient = "CREATE INDEX IF NOT EXISTS idx_messages_pid ON messages(patient_id);";
        String idxMsgTime    = "CREATE INDEX IF NOT EXISTS idx_messages_ts  ON messages(timestamp);";

        try (Statement st = conn.createStatement()) {
            //Create tables
            st.execute(patients);
            st.execute(symptoms);
            st.execute(measurements);
            st.execute(doctors);
            st.execute(appointments);
            st.execute(messages);
            //Create indexes
            st.execute(idxEmail);
            st.execute(idxSymPt);
            st.execute(idxMeaPt);
            st.execute(idxDoctorEmail);
            st.execute(idxAppDoctor);
            st.execute(idxAppPatient);
            st.execute(idxMsgDoctor);
            st.execute(idxMsgPatient);
            st.execute(idxMsgTime);

        } catch (SQLException e) {
            System.err.println("[DB] Schema error: " + e.getMessage());
        }
    }
}