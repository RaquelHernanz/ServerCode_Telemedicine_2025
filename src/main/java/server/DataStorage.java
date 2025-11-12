package server;

//La clase es solamente para las pruebas con archivos de que funcionan los sockets

import pojos.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStorage {

    // ==== IDs secuenciales ====
    private static final AtomicInteger doctorIdSeq = new AtomicInteger(1);
    private static final AtomicInteger patientIdSeq = new AtomicInteger(1);
    private static final AtomicInteger appointmentIdSeq = new AtomicInteger(1);
    private static final AtomicInteger measurementIdSeq = new AtomicInteger(1);
    private static final AtomicInteger symptomsIdSeq = new AtomicInteger(1);

    // ==== Credenciales (username = email normalmente) ====
    private static final Map<String, String> credentials = new HashMap<>();
    private static final Map<String, User.Role> rolesByUsername = new HashMap<>();

    // ==== Doctores / Pacientes ====
    private static final Map<Integer, Doctor> doctorsById = new HashMap<>();
    private static final Map<Integer, Patient> patientsById = new HashMap<>();
    private static final Map<String, Integer> doctorIdByEmail = new HashMap<>();
    private static final Map<String, Integer> patientIdByEmail = new HashMap<>();

    // ==== Citas, mediciones, síntomas ====
    private static final List<Appointment> appointments = new ArrayList<>();
    private static final List<Measurement> measurements = new ArrayList<>();
    private static final List<Symptoms> symptomsList = new ArrayList<>();

    // ===================== DOCTOR ======================

    public static synchronized int registerDoctor(Doctor doctor, String password) {
        String email = doctor.getEmail();

        if (credentials.containsKey(email)) {
            throw new IllegalArgumentException("There is already a user with email: " + email);
        }

        int id = doctorIdSeq.getAndIncrement();
        doctor.setId(id);

        credentials.put(email, password);
        rolesByUsername.put(email, User.Role.DOCTOR);
        doctorsById.put(id, doctor);
        doctorIdByEmail.put(email, id);

        return id;
    }

    public static Doctor getDoctorById(int id) {
        return doctorsById.get(id);
    }

    public static List<Doctor> listAllDoctors() {
        return new ArrayList<>(doctorsById.values());
    }

    // ===================== PATIENT ======================

    public static synchronized int registerPatient(Patient patient, String password) {
        String email = patient.getEmail();

        if (credentials.containsKey(email)) {
            throw new IllegalArgumentException("There is already a user with email: " + email);
        }

        int id = patientIdSeq.getAndIncrement();
        patient.setId(id);

        credentials.put(email, password);
        rolesByUsername.put(email, User.Role.PATIENT);
        patientsById.put(id, patient);
        patientIdByEmail.put(email, id);

        return id;
    }

    public static Patient getPatientById(int id) {
        return patientsById.get(id);
    }

    public static List<Patient> listAllPatients() {
        return new ArrayList<>(patientsById.values());
    }

    public static synchronized void assignDoctorToPatient(int patientId, int doctorId) {
        Patient p = patientsById.get(patientId);
        Doctor d = doctorsById.get(doctorId);
        if (p == null || d == null) {
            throw new IllegalArgumentException("Patient or doctor not found");
        }
        p.setDoctor(d);
        // Si quieres mantener lista de pacientes en Doctor:
        if (d.getPatients() != null && !d.getPatients().contains(p)) {
            d.getPatients().add(p);
        }
    }

    // ===================== LOGIN ======================

    public static synchronized User validateLogin(String username, String password) {
        if (!credentials.containsKey(username)) {
            return null;
        }
        if (!credentials.get(username).equals(password)) {
            return null;
        }
        User.Role role = rolesByUsername.get(username);
        if (role == null) {
            role = User.Role.PATIENT;
        }
        return new User(username, password, role);
    }

    // ===================== APPOINTMENTS ======================

    public static synchronized Appointment addAppointment(Patient patient, Doctor doctor,
                                                          LocalDateTime date, String message) {
        int id = appointmentIdSeq.getAndIncrement();
        Appointment appt = new Appointment(date, message, doctor, patient);
        appt.setId(id);
        appointments.add(appt);
        return appt;
    }

    public static List<Appointment> listAppointmentsForDoctor(int doctorId, Integer patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getDoctor() != null && a.getDoctor().getId() == doctorId) {
                if (patientId == null ||
                        (a.getPatient() != null && a.getPatient().getId() == patientId)) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    public static List<Appointment> listAppointmentsForPatient(int patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getPatient() != null && a.getPatient().getId() == patientId) {
                result.add(a);
            }
        }
        return result;
    }

    // ===================== MEASUREMENTS ======================

    public static synchronized Measurement addMeasurement(Patient patient,
                                                          Measurement.Type type,
                                                          List<Integer> values,
                                                          LocalDateTime date) {
        int id = measurementIdSeq.getAndIncrement();
        Measurement m = new Measurement(id, type, values, date, patient);
        measurements.add(m);
        return m;
    }

    public static List<Measurement> listMeasurementsForPatient(int patientId) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : measurements) {
            if (m.getPatient() != null && m.getPatient().getId() == patientId) {
                result.add(m);
            }
        }
        return result;
    }

    // ===================== SYMPTOMS ======================

    public static synchronized Symptoms addSymptoms(Patient patient,
                                                    String description,
                                                    LocalDateTime date_hour) {
        int id = symptomsIdSeq.getAndIncrement();
        Symptoms s = new Symptoms(description, date_hour, patient);
        s.setId(id);
        symptomsList.add(s);
        return s;
    }

    public static List<Symptoms> listSymptomsForPatient(int patientId) {
        List<Symptoms> result = new ArrayList<>();
        for (Symptoms s : symptomsList) {
            if (s.getPatient() != null && s.getPatient().getId() == patientId) {
                result.add(s);
            }
        }
        return result;
    }
}
