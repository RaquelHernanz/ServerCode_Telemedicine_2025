package server.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    public static class MessageMeta {
        private final int id;
        private final int doctorId;
        private final int patientId;
        private final String senderRole; // "DOCTOR" o "PATIENT"
        private final String timestamp;
        private final String text;

        public MessageMeta(int id, int doctorId, int patientId,
                           String senderRole, String timestamp, String text) {
            this.id = id;
            this.doctorId = doctorId;
            this.patientId = patientId;
            this.senderRole = senderRole;
            this.timestamp = timestamp;
            this.text = text;
        }

        public int getId() { return id; }
        public int getDoctorId() { return doctorId; }
        public int getPatientId() { return patientId; }
        public String getSenderRole() { return senderRole; }
        public String getTimestamp() { return timestamp; }
        public String getText() { return text; }
    }

    /**
     * Inserta un mensaje en la conversación doctor–paciente.
     */
    public static Integer insert(int doctorId, int patientId,
                                 String senderRole, String timestamp, String text) {
        String sql = "INSERT INTO messages(doctor_id, patient_id, sender_role, timestamp, text) " +
                "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, doctorId);
            ps.setInt(2, patientId);
            ps.setString(3, senderRole);
            ps.setString(4, timestamp);
            ps.setString(5, text);

            int updated = ps.executeUpdate();
            if (updated == 0) return null;

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Message insert error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Devuelve todos los mensajes de la conversación doctor–paciente,
     * ordenados por timestamp ascendente (lo típico en un chat).
     */
    public static List<MessageMeta> listConversation(int doctorId, int patientId) {
        String sql = """
          SELECT id, doctor_id, patient_id, sender_role, timestamp, text
          FROM messages
          WHERE doctor_id = ? AND patient_id = ?
          ORDER BY timestamp ASC, id ASC
          """;

        List<MessageMeta> result = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setInt(2, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MessageMeta(
                            rs.getInt("id"),
                            rs.getInt("doctor_id"),
                            rs.getInt("patient_id"),
                            rs.getString("sender_role"),
                            rs.getString("timestamp"),
                            rs.getString("text")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Message listConversation error: " + e.getMessage());
        }
        return result;
    }
}
