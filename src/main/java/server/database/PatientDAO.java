package server.database;

import java.sql.*;

public class PatientDAO { // crear paciente en la base de datos

    // REGISTRAR AL PACIENTE
    public static boolean register(String name, String surname, String email, String pwd, String dob, String sex, String phone) {
        String sql = "INSERT INTO patients(name,surname,email,password,dob,sex,phone) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, surname); ps.setString(3, email);
            ps.setString(4, pwd);  ps.setString(5, dob);     ps.setString(6, sex);
            ps.setString(7, phone);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Register error: " + e.getMessage());
            return false;
        }
    }

    public static boolean validateLogin(String email, String password) {
        String sql = "SELECT 1 FROM patients WHERE email=? AND password=?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email); ps.setString(2, password);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("[DB] Login error: " + e.getMessage());
            return false;
        }
    }

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
}