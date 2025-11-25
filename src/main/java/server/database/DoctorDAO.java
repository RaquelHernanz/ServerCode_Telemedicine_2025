package server.database;
import pojos.Patient;
import pojos.Sex;
import java.util.ArrayList;
import java.util.List;
import pojos.Doctor;

import java.sql.*;

public class DoctorDAO {

    // 1. REGISTRAR DOCTOR: Inserta un nuevo doctor en la base de datos
    public static boolean register(String name, String surname, String email, String pwd, String phone) {

        // La sentencia SQL para la inserción.
        String sql = "INSERT INTO doctors(name,surname,email,password,phone) VALUES(?,?,?,?,?)";

        // Uso de try-with-resources para asegurar el cierre automático del PreparedStatement.
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {

            // Asigna los valores a los placeholders (?) de la consulta SQL.
            ps.setString(1, name);
            ps.setString(2, surname);
            ps.setString(3, email);
            ps.setString(4, pwd); // Nota: En un sistema real, 'pwd' debe ser hasheado (como en tu Encryption.java)
            ps.setString(5, phone);

            // Ejecuta la consulta (INSERT)
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Captura errores SQL. Verifica si el email es duplicado (restricción UNIQUE en la DB).
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                System.err.println("[DB] Doctor email already registered: " + email);
            } else {
                System.err.println("[DB] Doctor register error: " + e.getMessage());
            }
            return false;
        }
    }

    // 2. VALIDAR LOGIN: Comprueba si las credenciales coinciden
    public static boolean validateLogin(String email, String password) {

        // La consulta busca cualquier fila que coincida con el email y la contraseña.
        String sql = "SELECT 1 FROM doctors WHERE email=? AND password=?";

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);

            // Ejecuta la consulta (SELECT)
            ResultSet rs = ps.executeQuery();

            // rs.next() devuelve true si se encontró al menos una fila (login exitoso).
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] Doctor login error: " + e.getMessage());
            return false;
        }
    }

    // 3. OBTENER ID: Devuelve el ID del doctor por email (necesario después del login)
    public static Integer getIdByEmail(String email) {
        String sql = "SELECT id FROM doctors WHERE email = ?";

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            // Si rs.next() es true, devuelve el valor entero de la primera columna (el ID).
            return rs.next() ? rs.getInt(1) : null;
        } catch (SQLException e) {
            System.err.println("[DB] Doctor getIdByEmail error: " + e.getMessage());
            return null;
        }
    }
    public static List<Patient> getPatientsByDoctorId(int doctorId) {
        // Consulta SQL: Selecciona todos los campos (*) de la tabla patients
        // DONDE el campo doctor_id sea igual al ID del doctor proporcionado.
        String sql = "SELECT * FROM patients WHERE doctor_id = ?";
        List<Patient> patients = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, doctorId); // Asigna el ID del doctor a la consulta
            ResultSet rs = ps.executeQuery(); // Ejecuta la consulta

            // Itera sobre cada paciente encontrado en el ResultSet
            while (rs.next()) {
                // Crea un nuevo objeto Patient con los datos de la fila actual
                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("email"),
                        Sex.valueOf(rs.getString("sex")), // Convierte el String 'sex' a la enumeración Sex
                        rs.getString("phone"),
                        rs.getString("dob"),
                        new ArrayList<>(), // appointments (se cargan aparte si fuera necesario)
                        new ArrayList<>(), // measurements (se cargan aparte si fuera necesario)
                        new ArrayList<>(), // symptoms (se cargan aparte si fuera necesario)
                        null,              // doctor (se puede dejar en null para evitar bucle infinito)
                        new ArrayList<>()  // messages
                );
                patients.add(patient);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error getting patients by doctor ID: " + e.getMessage());
        }
        return patients; // Devuelve la lista de objetos Patient
    }
    //Obtiene una lista de todos los doctores registrados.

    public static List<Doctor> getAllDoctors() {
        // Consulta SQL: Selecciona el ID, nombre, apellido y email de todos los doctores.
        // Omitimos la contraseña por seguridad.
        String sql = "SELECT id, name, surname, email FROM doctors";
        List<Doctor> doctorsList = new ArrayList<>();

        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                // Creamos un objeto Doctor por cada fila. Las listas de pacientes/citas van vacías.
                Doctor doctor = new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        null, // phonenumber (no lo necesitamos en esta consulta)
                        rs.getString("email"),
                        new ArrayList<>(), // patients
                        new ArrayList<>(), // appointments
                        new ArrayList<>()  // messages
                );
                doctorsList.add(doctor);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error getting all doctors: " + e.getMessage());
        }
        return doctorsList;
    }

    // Obtener un doctor por su ID
    public static Doctor getDoctorById(int id) {
        String sql = "SELECT id, name, surname, email, phone FROM doctors WHERE id = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phone"),   // phonenumber
                        rs.getString("email"),
                        new ArrayList<>(),       // patients
                        new ArrayList<>(),       // appointments
                        new ArrayList<>()        // messages
                );
            }
        } catch (SQLException e) {
            System.err.println("[DB] getDoctorById error: " + e.getMessage());
        }
        return null;
    }

    // Obtener un doctor por su email
    public static Doctor getDoctorByEmail(String email) {
        String sql = "SELECT id, name, surname, email, phone FROM doctors WHERE email = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            }
        } catch (SQLException e) {
            System.err.println("[DB] getDoctorByEmail error: " + e.getMessage());
        }
        return null;
    }

    // Obtener un doctor por su nombre (solo como último recurso)
    public static Doctor getDoctorByName(String name) {
        String sql = "SELECT id, name, surname, email, phone FROM doctors WHERE name = ?";
        try (PreparedStatement ps = DatabaseManager.get().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Doctor(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                );
            }
        } catch (SQLException e) {
            System.err.println("[DB] getDoctorByName error: " + e.getMessage());
        }
        return null;
    }
}
