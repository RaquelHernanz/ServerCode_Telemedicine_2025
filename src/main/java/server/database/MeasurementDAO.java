package server.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MeasurementDAO {

    // DTO sencillo para devolver metadatos de las mediciones
    public static class MeasurementMeta {
        private final int id;
        private final String type;
        private final String startedAt;
        private final String filePath;

        public MeasurementMeta(int id, String type, String startedAt, String filePath) {
            this.id = id;
            this.type = type;
            this.startedAt = startedAt;
            this.filePath = filePath;
        }

        public int getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getStartedAt() {
            return startedAt;
        }

        public String getFilePath() {
            return filePath;
        }
    }

    // Inserta metadatos de una medición (ECG/EDA)
    public static boolean insertMeta(int patientId, String type, String startedAt, String filePath) {
        String sql = "INSERT INTO measurements(patient_id, type, started_at, file_path) VALUES (?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ps.setString(2, type);
            ps.setString(3, startedAt);
            ps.setString(4, filePath);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Measurement insert error: " + e.getMessage());
            return false;
        }
    }

    // Lista las mediciones de un paciente (solo metadatos, no los valores del CSV)
    public static List<MeasurementMeta> listByPatientId(int patientId) {
        String sql = "SELECT id, type, started_at, file_path " +
                "FROM measurements " +
                "WHERE patient_id = ? " +
                "ORDER BY started_at DESC, id DESC";

        List<MeasurementMeta> result = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MeasurementMeta m = new MeasurementMeta(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("started_at"),
                            rs.getString("file_path")
                    );
                    result.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Measurement list error: " + e.getMessage());
        }

        return result;
    }

    // Obtiene los metadatos de una medición por su ID
    public static MeasurementMeta getById(int measurementId) {
        String sql = "SELECT id, type, started_at, file_path " +
                "FROM measurements WHERE id = ?";

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, measurementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new MeasurementMeta(
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("started_at"),
                            rs.getString("file_path")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB] Measurement getById error: " + e.getMessage());
        }
        return null;
    }

}