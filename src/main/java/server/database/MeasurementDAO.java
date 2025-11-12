package server.database;

import java.sql.*;

public class MeasurementDAO {

    public static boolean insertMeta(int patientId, String type, String startedAt, String filePath) {
        String sql = "INSERT INTO measurements(patient_id,type,started_at,file_path) VALUES(?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, type);
            ps.setString(3, startedAt);
            ps.setString(4, filePath);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Measurement meta error: " + e.getMessage());
            return false;
        }
    }
}