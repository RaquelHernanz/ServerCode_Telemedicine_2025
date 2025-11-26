package server.database;

import pojos.Doctor;
import pojos.Patient;
import pojos.Sex;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO { // crear paciente en la base de datos

    private static Integer getDoctorIdByName(String doctorName) {
        String sql = "SELECT id FROM doctors WHERE name = ?";

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, doctorName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("id"); // doctor encontrado
            }
            return null; // doctor NO encontrado

        } catch (SQLException e) {
            System.err.println("[DB] Doctor lookup error: " + e.getMessage());
            return null;
        }
    }

    // Inserta paciente
    public static boolean registerPatient(String name, String surname, String email, String pwd,
                                          String dob, String sex, String phone, int doctorId) {

        String sql = "INSERT INTO patients(name,surname,email,password,dob,sex,phone,doctor_id) " +
                "VALUES(?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, surname);
            ps.setString(3, email);
            ps.setString(4, pwd);
            ps.setString(5, dob);
            ps.setString(6, sex);
            ps.setString(7, phone);
            ps.setInt(8, doctorId);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.err.println("[DB] Email already registered: " + email);
            } else {
                System.err.println("[DB] Register error: " + e.getMessage());
            }
            return false;
        }
    }

    // Valida credenciales del paciente (email+password)
    public static boolean validateLogin(String email, String password) {
        String sql = "SELECT 1 FROM patients WHERE email=? AND password=?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return false;
        }
    }

    // Devuelve id del paciente por email

    public static Integer getIdByEmail(String email) {
        String sql = "SELECT id FROM patients WHERE email = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : null;
        } catch (SQLException e) {
            System.err.println("[DB] getIdByEmail error: " + e.getMessage());
            return null;
        }
    }

    public static Patient getPatientById(int id) {
        String sql = "SELECT id, name, surname, email, dob, sex, phone, doctor_id FROM patients WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int patientId      = rs.getInt("id");
                String name        = rs.getString("name");
                String surname     = rs.getString("surname");
                String email       = rs.getString("email");
                String dob         = rs.getString("dob");          // o conviertes a LocalDate si lo usas así
                String sexString   = rs.getString("sex");
                String phone       = rs.getString("phone");
                int doctorId       = rs.getInt("doctor_id");
                Doctor d = DoctorDAO.getDoctorById(doctorId);

                Sex sex = null;
                if (sexString != null) {
                    try {
                        sex = Sex.valueOf(sexString.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println("[DB] Unknown sex value in DB: " + sexString);
                    }
                }

                Patient p = new Patient(
                        patientId,
                        name,
                        surname,
                        email,
                        sex,
                        phone,
                        dob,
                        new ArrayList<>(),   // appointments
                        new ArrayList<>(),   // measurements
                        new ArrayList<>(),   // symptoms
                        d,                // doctor (puedes cargarlo con DoctorDAO.getDoctorById(doctorId) si quieres)
                        new ArrayList<>()    // messages
                );

                return p;
            }
        } catch (SQLException e) {
            System.err.println("[DB] getPatientById error: " + e.getMessage());
        }
        return null;
    }



}