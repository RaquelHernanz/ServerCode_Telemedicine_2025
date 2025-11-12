package server.database;

import java.sql.*;
import java.time.LocalDateTime;

public class SymptomDAO {

    public static boolean insertSymptom(int patientId, String description) {
        String sql = "INSERT INTO symptoms(patient_id,description,timestamp) VALUES(?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, description);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Symptom insert error: " + e.getMessage());
            return false;
        }
    }
}