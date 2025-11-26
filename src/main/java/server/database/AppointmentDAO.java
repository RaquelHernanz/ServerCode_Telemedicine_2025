package server.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class AppointmentDAO {
    public static class AppointmentMeta {
        private final int id;
        private final int doctorId;
        private final int patientId;
        private final String datetime;
        private final String message;

        public AppointmentMeta(int id, int doctorId, int patientId, String datetime, String message) {
            this.id = id;
            this.doctorId = doctorId;
            this.patientId = patientId;
            this.datetime = datetime;
            this.message = message;
        }

        public int getId()        { return id; }
        public int getDoctorId()  { return doctorId; }
        public int getPatientId() { return patientId; }
        public String getDatetime() { return datetime; }
        public String getMessage()  { return message; }
    }

    /**
     * Inserta una cita.
     * @return id autogenerado o null si falla
     */
    public static Integer insert(int doctorId, int patientId, String datetimeIso, String message) {
        String sql = "INSERT INTO appointments(doctor_id, patient_id, datetime, message) VALUES (?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, doctorId);
            ps.setInt(2, patientId);
            ps.setString(3, datetimeIso);
            ps.setString(4, message);

            int updated = ps.executeUpdate();
            if (updated == 0) {
                return null;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[DB] Appointment insert error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lista las citas de un doctor.
     */
    public static List<AppointmentMeta> listByDoctor(int doctorId) {
        String sql = "SELECT id, doctor_id, patient_id, datetime, message " +
                "FROM appointments WHERE doctor_id = ? " +
                "ORDER BY datetime DESC, id DESC";
        List<AppointmentMeta> result = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AppointmentMeta(
                            rs.getInt("id"),
                            rs.getInt("doctor_id"),
                            rs.getInt("patient_id"),
                            rs.getString("datetime"),
                            rs.getString("message")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Appointment listByDoctor error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Lista las citas de un paciente.
     */
    public static List<AppointmentMeta> listByPatient(int patientId) {
        String sql = "SELECT id, doctor_id, patient_id, datetime, message " +
                "FROM appointments WHERE patient_id = ? " +
                "ORDER BY datetime DESC, id DESC";
        List<AppointmentMeta> result = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AppointmentMeta(
                            rs.getInt("id"),
                            rs.getInt("doctor_id"),
                            rs.getInt("patient_id"),
                            rs.getString("datetime"),
                            rs.getString("message")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Appointment listByPatient error: " + e.getMessage());
        }
        return result;
    }

    // Comprueba si un slot (DoctorId + Datetime) ya está ocupado.
    public static boolean isSlotTaken(int doctorId, String datetime) {
        // SQL: Busca cualquier fila que coincida con el doctor Y la fecha/hora.
        String sql = "SELECT 1 FROM appointments WHERE doctor_id = ? AND datetime = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setString(2, datetime);
            // Si el ResultSet tiene una fila (next() devuelve true), el slot está tomado.
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[DB] Appointment isSlotTaken error: " + e.getMessage());
            // Si hay un error de DB, devolvemos true por seguridad (asumimos que está ocupado).
            return true;
        }
    }
}
