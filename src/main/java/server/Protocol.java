package server;

import com.google.gson.*;
import pojos.Doctor;
import pojos.Patient;
import pojos.Symptoms;
import server.database.MeasurementDAO;
import server.database.PatientDAO;
import server.database.SymptomDAO;
import server.database.DoctorDAO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interpreta los mensajes JSON de los clientes (Patient/Doctor).
 * Formato esperado: {"action":"...","requestId":"...","payload":{...}}
 */
public class
Protocol {

    // Gson para parsear y construir JSON
    private static final Gson gson = new Gson();

    /**
     * Punto de entrada: recibe una línea de texto (JSON), devuelve otra (JSON).
     */
    public static String process(String message) {
        try {
            // Parseamos el texto a objeto JSON (o null si estaba vacío/incorrecto)
            JsonObject req = gson.fromJson(message, JsonObject.class);
            if (req == null) {
                return error(null, "UNKNOWN", "Empty message");
            }

            // Sacamos "action" (qué quiere hacer el cliente)
            String action = req.has("action") ? req.get("action").getAsString() : null;
            // requestId es opcional (para que el cliente correlacione la respuesta)
            String requestId = req.has("requestId") ? req.get("requestId").getAsString() : null;

            if (action == null) {
                return error(requestId, "UNKNOWN", "Missing 'action' field");
            }

            // Enrutamos por acción
            switch (action) {
                case "REGISTER_PATIENT":
                    return handleRegisterPatient(req, requestId);
                case "REGISTER_DOCTOR":
                    return handleRegisterDoctor(req, requestId);

                case "LOGIN":
                    return handleLogin(req, requestId);

                case "SEND_SYMPTOMS":
                    return handleSendSymptoms(req, requestId);

                case "SEND_MEASUREMENT":
                    return handleSendMeasurement(req, requestId);

                case "LIST_PATIENTS":
                    return handleListPatients(req, requestId);

                case "REQUEST_APPOINTMENT":
                case "LIST_APPOINTMENTS":
                case "LIST_MEASUREMENTS":
                case "LIST_SYMPTOMS":
                    return handleListSymptoms(req, requestId);
                case "LIST_DOCTORS":
                    return handleListDoctors(req, requestId);
                default:
                    return error(requestId, action, "Unknown action: " + action);
            }

        } catch (JsonSyntaxException e) {
            // JSON roto
            return error(null, "UNKNOWN", "Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            // Cualquier otra excepción
            return error(null, "UNKNOWN", "Internal error: " + e.getMessage());
        }
    }

    // ------------------------- HANDLERS -------------------------

    // REGISTRO de paciente -> inserta en BD y devuelve su id
    private static String handleRegisterPatient(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req); // coge payload o {}
        // Leemos campos
        String name     = getString(payload, "name",     "");
        String surname  = getString(payload, "surname",  "");
        String email    = getString(payload, "email",    "");
        String password = getString(payload, "password", "");
        String dob      = getString(payload, "dob",      ""); // "yyyy-MM-dd"
        String sex      = getString(payload, "sex",      "");
        String phone    = getString(payload, "phone",    "");

        // Validaciones mínimas
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            return error(requestId, "REGISTER_PATIENT", "Missing required fields (name, surname, email, password)");
        }

        // Insertar en BD
        boolean ok = PatientDAO.register(name, surname, email, password, dob, sex, phone);
        Integer patientId = ok ? PatientDAO.getIdByEmail(email) : null;
        if (!ok || patientId == null) {
            return error(requestId, "REGISTER_PATIENT", "Register failed (maybe duplicated email)");
        }

        // Respuesta OK con id
        JsonObject resp = baseResponse("REGISTER_PATIENT", requestId, "OK","Patient registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("patientId", patientId);
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }



    //REGISTRO de doctor -> inserta en BD y devuelve su id
    private static String handleRegisterDoctor(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        // Obtenemos los campos del JSON payload
        String name     = getString(payload, "name",     "");
        String surname  = getString(payload, "surname",  "");
        String email    = getString(payload, "email",    "");
        // Asumimos que la contraseña ya está hasheada por el cliente o la manejaremos en el DAO
        String password = getString(payload, "password", "");
        String phone    = getString(payload, "phone",    "");

        // Validación básica de campos
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            return error(requestId, "REGISTER_DOCTOR", "Missing required fields (name, surname, email, password)");
        }

        // Llama a DoctorDAO para insertar
        boolean ok = DoctorDAO.register(name, surname, email, password, phone);

        if (!ok) {
            return error(requestId, "REGISTER_DOCTOR", "Register failed (maybe duplicated email)");
        }

        // Buscamos el ID para devolverlo al cliente
        Integer doctorId = DoctorDAO.getIdByEmail(email);
        if (doctorId == null) {
            return error(requestId, "REGISTER_DOCTOR", "Doctor registered but ID not found. Internal error.");
        }

        // Construcción de la respuesta OK
        JsonObject resp = baseResponse("REGISTER_DOCTOR", requestId, "OK","Doctor registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("doctorId", doctorId); // Devolvemos el ID
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    // Archivo: Protocol.java (REEMPLAZAR el método handleLogin actual)

    /**
     * LOGIN -> valida credenciales del usuario y diferencia el rol (Patient o Doctor)
     */
    private static String handleLogin(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        String username = getString(payload, "username", ""); // email
        String password = getString(payload, "password", "");

        if (username.isBlank() || password.isBlank()) {
            return error(requestId, "LOGIN", "Missing username or password");
        }

        // --- 1. INTENTO DE VALIDACIÓN COMO PACIENTE ---
        if (PatientDAO.validateLogin(username, password)) {
            // Login exitoso como PACIENTE
            JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful (Patient)");
            JsonObject respPayload = new JsonObject();
            Integer pid = PatientDAO.getIdByEmail(username);

            respPayload.addProperty("userId", pid != null ? pid : 0);
            respPayload.addProperty("role", "PATIENT"); // <-- ROL
            respPayload.addProperty("token", "session-" + username);
            resp.add("payload", respPayload);
            return gson.toJson(resp);
        }

        // --- 2. INTENTO DE VALIDACIÓN COMO DOCTOR ---
        if (DoctorDAO.validateLogin(username, password)) {
            // Login exitoso como DOCTOR
            JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful (Doctor)");
            JsonObject respPayload = new JsonObject();
            Integer did = DoctorDAO.getIdByEmail(username);

            respPayload.addProperty("userId", did != null ? did : 0);
            respPayload.addProperty("role", "DOCTOR"); // <-- ROL
            respPayload.addProperty("token", "session-" + username);
            resp.add("payload", respPayload);
            return gson.toJson(resp);
        }

        //  3. FALLO TOTAL
        return error(requestId, "LOGIN", "Invalid username or password");
    }

    // SÍNTOMAS -> inserta en tabla symptoms
    private static String handleSendSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int patientId      = getInt(payload, "patientId", -1);
        String description = getString(payload, "description", "");
        String hourStr     = getString(payload, "hour", ""); // opcional (ISO-8601)

        if (patientId <= 0 || description.isBlank()) {
            return error(requestId, "SEND_SYMPTOMS", "Missing patientId or description");
        }

        // Validación blanda de fecha (si viene y está mal, ignoramos y usamos now() en el DAO)
        if (!hourStr.isBlank()) {
            try { LocalDateTime.parse(hourStr); } catch (Exception ignored) {}
        }

        boolean ok = SymptomDAO.insertSymptom(patientId, description); // timestamp se pone en el DAO (now())
        if (!ok) {
            return error(requestId, "SEND_SYMPTOMS", "DB error while inserting symptom");
        }

        JsonObject resp = baseResponse("SEND_SYMPTOMS", requestId, "OK","Symptoms stored");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("symptomsId", -1); // placeholder
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    // MEDICIÓN -> guarda CSV + metadatos en BD
    private static String handleSendMeasurement(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int patientId       = getInt(payload, "patientId", -1);
        String typeStr      = getString(payload, "type", "");
        String dateStr      = getString(payload, "date", ""); // ISO-8601 de inicio
        JsonArray valuesArr = payload.has("values") && payload.get("values").isJsonArray()
                ? payload.getAsJsonArray("values") : new JsonArray();

        if (patientId <= 0 || typeStr.isBlank() || dateStr.isBlank() || valuesArr.size() == 0) {
            return error(requestId, "SEND_MEASUREMENT", "Missing patientId/type/date/values");
        }

        // Convertimos valores a filas CSV (timestamp sintético: índice; valor; '-')
        JsonArray rows = new JsonArray();
        for (int i = 0; i < valuesArr.size(); i++) {
            String line = i + "," + valuesArr.get(i).getAsInt() + ",-"; // "0,523,-"
            rows.add(line);
        }

        // Guardamos CSV en carpeta por id: data/patient_<id>/signals_yyyy-MM-dd.csv
        String folder = "patient_" + patientId;
        String filePath = DataStorage.appendRowsToCsv(folder, rows);
        if (filePath == null) {
            return error(requestId, "SEND_MEASUREMENT", "CSV write failed");
        }

        // Guardar metadatos de la toma en BD
        boolean ok = MeasurementDAO.insertMeta(patientId, typeStr, dateStr, filePath);
        if (!ok) {
            return error(requestId, "SEND_MEASUREMENT", "DB insert failed (measurement meta)");
        }

        JsonObject resp = baseResponse("SEND_MEASUREMENT", requestId, "OK","Measurement stored");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("measurementId", -1); // placeholder
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    //LIST_PATIENTS -> Lista los pacientes asociados a un doctor que ha iniciado sesión.
    private static String handleListPatients(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        // El doctor cliente debe enviar su ID (userId) en el payload.
        int doctorId = getInt(payload, "userId", -1);

        if (doctorId <= 0) {
            return error(requestId, "LIST_PATIENTS", "Missing or invalid userId (doctorId) in request.");
        }

        // CRUCIAL: Llama al DAO para obtener la lista de objetos Patient que acabas de implementar.
        List<Patient> patients = DoctorDAO.getPatientsByDoctorId(doctorId);

        // Convertimos la lista de objetos Java (Patient) en un array JSON.
        // Gson se encarga de convertir cada objeto Patient a su formato JSON.
        JsonElement patientsJson = gson.toJsonTree(patients);

        // Construcción de la respuesta OK
        JsonObject resp = baseResponse("LIST_PATIENTS", requestId, "OK", "Patients retrieved successfully");
        JsonObject respPayload = new JsonObject();

        // Añadimos el array de pacientes al payload de la respuesta
        respPayload.add("patients", patientsJson);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    //LIST_SYMPTOMS -> Devuelve todos los síntomas de un paciente específico.

    private static String handleListSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int patientId = getInt(payload, "patientId", -1);

        if (patientId <= 0) {
            return error(requestId, "LIST_SYMPTOMS", "Missing or invalid patientId.");
        }

        // Llama al DAO para obtener la lista de objetos Symptoms.
        // NOTA: Asumiendo que SymptomDAO tiene este método de consulta.
        List<Symptoms> symptoms = SymptomDAO.getSymptomsByPatientId(patientId);

        JsonElement symptomsJson = gson.toJsonTree(symptoms);

        JsonObject resp = baseResponse("LIST_SYMPTOMS", requestId, "OK", "Symptoms retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("symptoms", symptomsJson);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    // Archivo: Protocol.java (Añadir este nuevo método)

    /**
     * LIST_DOCTORS -> Devuelve una lista de todos los doctores registrados.
     */
    private static String handleListDoctors(JsonObject req, String requestId) {
        // No necesitamos datos de entrada, solo llamamos al DAO

        // 1. Obtiene la lista de objetos Doctor del DAO.
        List<Doctor> doctors = DoctorDAO.getAllDoctors();

        // 2. Convierte la lista de objetos Java (Doctor) en un array JSON.
        JsonElement doctorsJson = gson.toJsonTree(doctors);

        // 3. Construcción de la respuesta OK
        JsonObject resp = baseResponse("LIST_DOCTORS", requestId, "OK", "Doctors retrieved successfully");
        JsonObject respPayload = new JsonObject();

        // Añadimos el array de doctores al payload de la respuesta
        respPayload.add("doctors", doctorsJson);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    // ------------------------- HELPERS JSON -------------------------

    // payload seguro ({} si no existe)
    private static JsonObject getPayload(JsonObject req) {
        return req.has("payload") && req.get("payload").isJsonObject()
                ? req.getAsJsonObject("payload")
                : new JsonObject();
    }

    // respuesta estándar base
    private static JsonObject baseResponse(String action, String requestId, String status, String msg) {
        JsonObject resp = new JsonObject();
        resp.addProperty("type", "RESPONSE");
        resp.addProperty("action", action);
        if (requestId != null) resp.addProperty("requestId", requestId);
        resp.addProperty("status", status);
        resp.addProperty("message", msg);
        return resp;
    }

    // error uniforme
    private static String error(String requestId, String action, String msg) {
        JsonObject resp = baseResponse(action != null ? action : "UNKNOWN", requestId, "ERROR", msg);
        resp.add("payload", new JsonObject());
        return gson.toJson(resp);
    }

    // “no implementado” uniforme
    private static String notImplemented(String requestId, String action, String msg) {
        JsonObject resp = baseResponse(action, requestId, "ERROR", msg);
        resp.add("payload", new JsonObject());
        return gson.toJson(resp);
    }

    // lecturas seguras de JSON
    private static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : def;
    }
    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
    }
}