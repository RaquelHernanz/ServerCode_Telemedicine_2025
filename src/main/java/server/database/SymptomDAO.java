package server.database;

import pojos.Symptoms;
import pojos.Patient;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    //Obtiene todos los síntomas reportados por un paciente específico (por patient_id).
    public static List<Symptoms> getSymptomsByPatientId(int patientId) {
        // Consulta SQL: Selecciona todos los campos de la tabla symptoms
        // donde el patient_id coincida con el ID proporcionado.
        String sql = "SELECT * FROM symptoms WHERE patient_id = ?";
        List<Symptoms> symptomsList = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, patientId); // Asigna el ID del paciente a la consulta
            ResultSet rs = ps.executeQuery(); // Ejecuta la consulta

            // Itera sobre cada síntoma encontrado
            while (rs.next()) {
                // Crea un objeto Symptoms a partir de los datos de la fila actual
                Symptoms symptom = new Symptoms(
                        rs.getInt("id"),
                        rs.getString("description"),
                        // Convierte el String "timestamp" de la DB a LocalDateTime de Java
                        LocalDateTime.parse(rs.getString("timestamp")),
                        // El objeto Patient es null o un placeholder; se requiere en el POJO Symptoms.
                        new Patient()
                );
                symptomsList.add(symptom);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error getting symptoms by patient ID: " + e.getMessage());
        }
        return symptomsList; // Devuelve la lista de objetos Symptoms
    }
}