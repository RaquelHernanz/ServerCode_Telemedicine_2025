package server;

import com.google.gson.*;
import pojos.*;
import server.database.*;
import utilities.Encryption;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interpreta los mensajes JSON de los clientes (Patient/Doctor).
 * Formato esperado: {"action":"...","requestId":"...","payload":{...}}
 */
public class Protocol {

    // Gson para parsear y construir JSON
    private static final Gson gson = new Gson();

    /**
     * Punto de entrada: recibe una línea de texto (JSON), devuelve otra (JSON).
     */
    public static String process(String message) {
        // Recibe un mensaje JSON enviado por el cliente (por socket).
        // Lo convierte a un objeto JSON.
        // Mira qué acción quiere hacer el cliente (login, registrar, enviar señal, pedir doctores…).
        // Llama al métdo adecuado (handleRegisterPatient(), handleLogin(), etc.).
        // Devuelve un JSON con la respuesta que el servidor enviará al cliente.

        try {
            // Parseamos el texto a objeto JSON (o null si estaba vacío/incorrecto)
            JsonObject req = gson.fromJson(message, JsonObject.class); // Convierte el string recibido por el socket en un JsonObject.
            if (req == null) {
                return error(null, "UNKNOWN", "Empty message");
            }

            // Sacamos "action" (qué quiere hacer el cliente)
            String action = req.has("action") ? req.get("action").getAsString() : null;
            // requestId es opcional (para que el cliente correlacione la respuesta. el servidor se lo envia para que el cliente sepa relacionar)
            String requestId = req.has("requestId") ? req.get("requestId").getAsString() : null;

            if (action == null) {
                return error(requestId, "UNKNOWN", "Missing 'action' field");
            }

            // Enrutamos por acción
            switch (action) { // router
                // según la acción envía al métdo correcto
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
                    return handleRequestAppointment(req, requestId);
                case "LIST_APPOINTMENTS":
                    return handleListAppointments(req, requestId);
                case "LIST_MEASUREMENTS":
                    return handleListMeasurements(req, requestId);
                case "GET_MEASUREMENT_VALUES":
                    return handleGetMeasurementValues(req, requestId);
                case "LIST_SYMPTOMS":
                    return handleListSymptoms(req, requestId);
                case "LIST_DOCTORS":
                    return handleListDoctors(req, requestId);
                case "SEND_MESSAGE":
                    return handleSendMessage(req, requestId);
                case "LIST_MESSAGES":
                    return handleListMessages(req, requestId);
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
        JsonObject payload = getPayload(req); // req = JSON completo enviado por el cliente, payload solo contiene los datos reales
        // Leemos datos del JSON
        String name     = getString(payload, "name",     "");
        String surname  = getString(payload, "surname",  "");
        String email    = getString(payload, "email",    "");
        String password = getString(payload, "password", "");
        String dob      = getString(payload, "dob",      ""); // "yyyy-MM-dd"
        String sex      = getString(payload, "sex",      "");
        String phone    = getString(payload, "phone",    "");

        // === DOCTOR (acepta id, email o nombre) ===
        int doctorId = payload.has("doctorId") ? payload.get("doctorId").getAsInt() : -1;
        String doctorEmail = getString(payload, "doctorEmail", "");
        String doctorName  = getString(payload, "doctorName", "");


        // Validaciones mínimas y si no devuelve error
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            return error(requestId, "REGISTER_PATIENT", "Missing required fields (name, surname, email, password)");
        }

        // === Resolver doctor ===
        Doctor doctor = null;

        if (doctorId > 0) {
            doctor = DoctorDAO.getDoctorById(doctorId);
        }
        if (doctor == null && !doctorEmail.isBlank()) {
            doctor = DoctorDAO.getDoctorByEmail(doctorEmail);
        }
        if (doctor == null && !doctorName.isBlank()) {
            doctor = DoctorDAO.getDoctorByName(doctorName);
        }

        if (doctor == null) {
            System.err.println("[DB] Doctor not found: " +
                    (doctorId > 0 ? "id=" + doctorId :
                            !doctorEmail.isBlank() ? doctorEmail : doctorName));
            return error(requestId, "REGISTER_PATIENT", "Doctor not found");
        }

        String passwordHash = Encryption.encryptPassword(password);
        // hashear la contraseña, el servidor nunca guarda la contraseña en texto plano siempre su hash

        // Insertar en BD
        boolean ok = PatientDAO.registerPatient(name, surname, email, passwordHash, dob, sex, phone, doctor.getId());
        // en la tabla
        Integer patientId = ok ? PatientDAO.getIdByEmail(email) : null; // si la inserción fue bien, los ID de los emails coinciden
        if (!ok || patientId == null) { // si algo falla, da error
            return error(requestId, "REGISTER_PATIENT", "Register failed (maybe duplicated email)");
        }

        //Recuperar el paciente completo de la BD
        Patient patient = PatientDAO.getPatientById(patientId);
        if (patient == null) {
            return error(requestId, "REGISTER_PATIENT", "Patient registered but not found");
        }

        // Respuesta OK del servidor al paciente con id para asegurarle que se ha realizado correctamente la acción
        JsonObject resp = baseResponse("REGISTER_PATIENT", requestId, "OK","Patient registered successfully");
        JsonObject respPayload = new JsonObject(); // crea un espacio vacío para escribir respuesta
        respPayload.addProperty("patientId", patientId); // añade el id del paciente al JSON

        // Datos extra para que el cliente pueda construir el POJO completo
        respPayload.addProperty("name", patient.getName());
        respPayload.addProperty("surname", patient.getSurname());
        respPayload.addProperty("email", patient.getEmail());
        respPayload.addProperty("dob", patient.getDob());
        if (patient.getSex() != null) {
            respPayload.addProperty("sex", patient.getSex().toString());
        }
        respPayload.addProperty("phone", patient.getPhonenumber());
        respPayload.addProperty("doctorId", doctor.getId());

        resp.add("payload", respPayload); // añade la respuesta correcta al JSON
        return gson.toJson(resp); // devuelve la respuesta como string
    }


    //REGISTRO de doctor -> inserta en BD y devuelve su id
    private static String handleRegisterDoctor(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req); //req = JSON completo enviado por el cliente, payload solo contiene los datos reales

        // Obtenemos los campos del JSON payload
        String name     = getString(payload, "name",     "");
        String surname  = getString(payload, "surname",  "");
        String email    = getString(payload, "email",    "");
        // La contraseña viene en claro desde el cliente; se hashea aquí antes de ir al DAO
        String password = getString(payload, "password", "");
        String phone    = getString(payload, "phone",    "");

        // Validación básica de campos
        if (name.isBlank() || surname.isBlank() || email.isBlank() || password.isBlank()) {
            return error(requestId, "REGISTER_DOCTOR", "Missing required fields (name, surname, email, password)");
        }

        String passwordHash = Encryption.encryptPassword(password);
        // hashear la contraseña, el servidor nunca guarda la contraseña en texto plano siempre su hash

        // Llama a DoctorDAO para insertar en la tabla
        boolean ok = DoctorDAO.register(name, surname, email, passwordHash, phone);

        if (!ok) { // si no se inserta bien da error
            return error(requestId, "REGISTER_DOCTOR", "Register failed (maybe duplicated email)");
        }

        // Buscamos el ID para guardar su sesión y poder identificar al doctor
        Integer doctorId = DoctorDAO.getIdByEmail(email);
        if (doctorId == null) {
            return error(requestId, "REGISTER_DOCTOR", "Doctor registered but ID not found. Internal error.");
        }

        Doctor doctor = DoctorDAO.getDoctorById(doctorId);
        if (doctor == null) {
            return error(requestId, "REGISTER_DOCTOR", "Doctor registered but not found");
        }

        // Construcción de la respuesta OK COMO ANTERIOR MÉTDO
        JsonObject resp = baseResponse("REGISTER_DOCTOR", requestId, "OK","Doctor registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("doctorId", doctorId); // Devolvemos el ID

        // Datos extra para el cliente
        respPayload.addProperty("name", doctor.getName());
        respPayload.addProperty("surname", doctor.getSurname());
        respPayload.addProperty("email", doctor.getEmail());
        respPayload.addProperty("phone", doctor.getPhonenumber());

        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    // Archivo: Protocol.java (REEMPLAZAR el métdo handleLogin actual)

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

        String passwordHash = Encryption.encryptPassword(password);

        // --- 1. INTENTO DE VALIDACIÓN COMO PACIENTE ---
        if (PatientDAO.validateLogin(username, passwordHash)) {

            Integer pid = PatientDAO.getIdByEmail(username);
            Patient patient = (pid != null) ? PatientDAO.getPatientById(pid) : null;

            if (patient == null) {
                return error(requestId, "LOGIN", "Patient not found after login");
            }

            // Login exitoso como PACIENTE
            JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful (Patient)");
            JsonObject respPayload = new JsonObject();

            respPayload.addProperty("userId", patient.getId());
            respPayload.addProperty("role", "PATIENT"); // <-- ROL
            respPayload.addProperty("token", "session-" + username);

            // Datos básicos del paciente y del doctor asignado
            respPayload.addProperty("name", patient.getName());
            respPayload.addProperty("surname", patient.getSurname());
            respPayload.addProperty("email", patient.getEmail());
            respPayload.addProperty("dob", patient.getDob());
            respPayload.addProperty("phone", patient.getPhonenumber());
            if (patient.getSex() != null) {
                respPayload.addProperty("sex", patient.getSex().toString());
            }
            respPayload.addProperty("doctor_id", patient.getDoctor().getId());//AÑADIDO
            respPayload.addProperty("doctorName", patient.getDoctor().getName());
            respPayload.addProperty("doctorSurname", patient.getDoctor().getSurname());
            respPayload.addProperty("doctorEmail", patient.getDoctor().getEmail());
            respPayload.addProperty("doctorPhone", patient.getDoctor().getPhonenumber());



            resp.add("payload", respPayload);
            return gson.toJson(resp);
        }

        // --- 2. INTENTO DE VALIDACIÓN COMO DOCTOR ---
        if (DoctorDAO.validateLogin(username, passwordHash)) {

            Integer did = DoctorDAO.getIdByEmail(username);
            Doctor doctor = (did != null) ? DoctorDAO.getDoctorById(did) : null;

            if (doctor == null) {
                return error(requestId, "LOGIN", "Doctor not found after login");
            }

            // Login exitoso como DOCTOR
            JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful (Doctor)");
            JsonObject respPayload = new JsonObject();


            respPayload.addProperty("userId", doctor.getId());
            respPayload.addProperty("role", "DOCTOR"); // <-- ROL
            respPayload.addProperty("token", "session-" + username);

            // Datos básicos del doctor
            respPayload.addProperty("name", doctor.getName());
            respPayload.addProperty("surname", doctor.getSurname());
            respPayload.addProperty("email", doctor.getEmail());
            respPayload.addProperty("phone", doctor.getPhonenumber());
            //respPayload.addProperty("patient_id", doctor.getPatients());


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

    // GET_MEASUREMENT_VALUES -> devuelve los valores (array de enteros) de una medición concreta
    private static String handleGetMeasurementValues(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int measurementId = getInt(payload, "measurementId", -1);

        if (measurementId <= 0) {
            return error(requestId, "GET_MEASUREMENT_VALUES", "Missing or invalid measurementId.");
        }

        // 1) Buscar metadatos en BD
        MeasurementDAO.MeasurementMeta meta = MeasurementDAO.getById(measurementId);
        if (meta == null) {
            return error(requestId, "GET_MEASUREMENT_VALUES", "Measurement not found.");
        }

        // 2) Leer el CSV asociado
        String csvJson = DataStorage.loadCsvAsJson(meta.getFilePath());
        if (csvJson == null) {
            return error(requestId, "GET_MEASUREMENT_VALUES", "CSV file not found or empty.");
        }

        // csvJson tiene la forma: {"header":"timestamp,ecg,eda","rows":["0,523,-","1,510,-",...]}
        JsonObject csvObj = gson.fromJson(csvJson, JsonObject.class);
        JsonArray rows = csvObj.getAsJsonArray("rows");

        // 3) Convertir las filas a un array de valores (enteros)
        JsonArray values = new JsonArray();
        for (JsonElement rowEl : rows) {
            String row = rowEl.getAsString();      // "0,523,-"
            String[] parts = row.split(",");
            if (parts.length >= 2) {
                try {
                    int val = Integer.parseInt(parts[1].trim()); // la columna del valor
                    values.add(val);
                } catch (NumberFormatException ignored) {
                    // si hay una fila rara, la saltamos
                }
            }
        }

        // 4) Construir la respuesta
        JsonObject resp = baseResponse("GET_MEASUREMENT_VALUES", requestId, "OK", "Values loaded");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("measurementId", meta.getId());
        respPayload.addProperty("type", meta.getType());
        respPayload.addProperty("date", meta.getStartedAt());
        respPayload.add("values", values);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }


    //LIST_PATIENTS -> Lista los pacientes asociados a un doctor que ha iniciado sesión.
    private static String handleListPatients(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        // El doctor cliente debe enviar su ID (userId) en el payload.
        int doctorId = getInt(payload, "doctorId", -1);

        if (doctorId <= 0) {
            return error(requestId, "LIST_PATIENTS", "Missing or invalid userId (doctorId) in request.");
        }

        // CRUCIAL: Llama al DAO para obtener la lista de objetos Patient que acabas de implementar.
        List<Patient> patients = DoctorDAO.getPatientsByDoctorId(doctorId);
        //System.out.println(patients);

        // --- Construir el JSON manualmente para evitar LocalDateTime ---
        // convertimos de JSON a objeto para mandar a doctor la lista de pacientes
        JsonArray arrayPatients = new JsonArray();
        for (Patient p : patients) {
            JsonObject jo = new JsonObject();
            jo.addProperty("id", p.getId());
            jo.addProperty("name", p.getName());
            jo.addProperty("surname", p.getSurname());
            jo.addProperty("email", p.getEmail());
            jo.addProperty("phone", p.getPhonenumber());
            jo.addProperty("dob", p.getDob());
            if (p.getSex() != null) {
                jo.addProperty("sex", p.getSex().toString());
            }
            arrayPatients.add(jo);
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("requestId", requestId);
        resp.addProperty("action", "LIST_PATIENTS");
        resp.addProperty("status", "OK");
        resp.add("patients", arrayPatients);

        return gson.toJson(resp);
    }

    //LIST_SYMPTOMS -> Devuelve todos los síntomas de un paciente específico.

    private static String handleListSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int patientId = getInt(payload, "patientId", -1);

        if (patientId <= 0) {
            return error(requestId, "LIST_SYMPTOMS", "Missing or invalid patientId.");
        }

        List<Symptoms> symptoms = SymptomDAO.getSymptomsByPatientId(patientId);

        JsonArray arr = new JsonArray();

        for (Symptoms s : symptoms) {
            JsonObject jo = new JsonObject();
            jo.addProperty("symptomsId", s.getId());
            jo.addProperty("description", s.getDescription());
            jo.addProperty("timestamp", s.getDateTime().toString()); // convertir a String
            arr.add(jo);
        }

        JsonObject resp = baseResponse("LIST_SYMPTOMS", requestId, "OK", "Symptoms retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("symptoms", arr);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    // Archivo: Protocol.java (Añadir este nuevo métdo)

    /**
     * LIST_DOCTORS -> Devuelve una lista de todos los doctores registrados.
     */
    private static String handleListDoctors(JsonObject req, String requestId) {
        // No necesitamos datos de entrada, solo llamamos al DAO

        // 1. Obtiene la lista de objetos Doctor del DAO.
        List<Doctor> doctors = DoctorDAO.getAllDoctors();

        /*
        // 2. Convierte la lista de objetos Java (Doctor) en un array JSON.
        JsonElement doctorsJson = gson.toJsonTree(doctors);
         */

        JsonArray arr = new JsonArray();
        for (Doctor d : doctors) {
            JsonObject jo = new JsonObject();
            jo.addProperty("doctorId", d.getId());
            jo.addProperty("name", d.getName());
            jo.addProperty("surname", d.getSurname());
            jo.addProperty("email", d.getEmail());
            arr.add(jo);
        }

        // 3. Construcción de la respuesta OK
        JsonObject resp = baseResponse("LIST_DOCTORS", requestId, "OK", "Doctors retrieved successfully");
        JsonObject respPayload = new JsonObject();

        // Añadimos el array de doctores al payload de la respuesta
        respPayload.add("doctors",arr);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    // LIST_MEASUREMENTS -> Lista las mediciones asociadas a un paciente.
    private static String handleListMeasurements(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int patientId = getInt(payload, "patientId", -1);

        if (patientId <= 0) {
            return error(requestId, "LIST_MEASUREMENTS", "Missing or invalid patientId.");
        }

        // Llama al DAO para obtener la lista de metadatos de medición
        java.util.List<server.database.MeasurementDAO.MeasurementMeta> measurements =
                server.database.MeasurementDAO.listByPatientId(patientId);

        // Convertimos a JSON sencillo
        JsonArray arr = new JsonArray();
        for (server.database.MeasurementDAO.MeasurementMeta m : measurements) {
            JsonObject jo = new JsonObject();
            jo.addProperty("id", m.getId());
            jo.addProperty("type", m.getType());
            jo.addProperty("date", m.getStartedAt());
            jo.addProperty("filePath", m.getFilePath());
            arr.add(jo);
        }

        JsonObject resp = baseResponse("LIST_MEASUREMENTS", requestId, "OK", "Measurements retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("measurements", arr);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    /**
     * REQUEST_APPOINTMENT
     * Espera un payload así:
     * {
     *   "doctorId": 1,
     *   "patientId": 5,
     *   "datetime": "2025-11-20T10:30:00",
     *   "message": "Dolor en el pecho desde hace dos días"
     * }
     */

    private static String handleRequestAppointment(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int doctorId  = getInt(payload, "doctorId",  -1);
        int patientId = getInt(payload, "patientId", -1);
        String datetime = getString(payload, "datetime", null);
        String message  = getString(payload, "message", "");

        if (doctorId <= 0 || patientId <= 0 || datetime == null || datetime.isBlank()) {
            return error(requestId, "REQUEST_APPOINTMENT",
                    "Missing or invalid doctorId / patientId / datetime");
        }

        // COMPROBACIÓN DE DISPONIBILIDAD (¡EL PASO CRÍTICO!)
        if (AppointmentDAO.isSlotTaken(doctorId, datetime)) {
            // Si está ocupado, devolvemos un ERROR explícito al cliente.
            return error(requestId, "REQUEST_APPOINTMENT",
                    "Appointment slot already taken. Please choose another date or time.");
        }

        // Insertar solo si el slot está libre.
        Integer appId = AppointmentDAO.insert(doctorId, patientId, datetime, message);
        if (appId == null) {
            return error(requestId, "REQUEST_APPOINTMENT", "DB insert failed (appointment)");
        }

        JsonObject resp = baseResponse("REQUEST_APPOINTMENT", requestId, "OK", "Appointment created");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("appointmentId", appId);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    /**
     * LIST_APPOINTMENTS
     * Admite dos variantes de payload:
     * - { "doctorId": 1 }
     * - { "patientId": 5 }
     * Si vienen ambos, priorizamos doctorId.
     */
    private static String handleListAppointments(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int doctorId  = getInt(payload, "doctorId",  -1);
        int patientId = getInt(payload, "patientId", -1);

        java.util.List<AppointmentDAO.AppointmentMeta> appointments;
        String scope;

        if (doctorId > 0) {
            appointments = AppointmentDAO.listByDoctor(doctorId);
            scope = "DOCTOR";
        } else if (patientId > 0) {
            appointments = AppointmentDAO.listByPatient(patientId);
            scope = "PATIENT";
        } else {
            return error(requestId, "LIST_APPOINTMENTS",
                    "You must provide doctorId or patientId in payload");
        }

        JsonArray arr = new JsonArray();
        for (AppointmentDAO.AppointmentMeta a : appointments) {
            JsonObject jo = new JsonObject();
            jo.addProperty("id", a.getId());
            jo.addProperty("doctorId", a.getDoctorId());
            jo.addProperty("patientId", a.getPatientId());
            jo.addProperty("datetime", a.getDatetime());
            jo.addProperty("message", a.getMessage());
            arr.add(jo);
        }

        JsonObject resp = baseResponse("LIST_APPOINTMENTS", requestId, "OK",
                "Appointments retrieved for " + scope);
        JsonObject respPayload = new JsonObject();
        respPayload.add("appointments", arr);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    /**
     * SEND_MESSAGE
     * Payload esperado:
     * {
     *   "doctorId": 1,
     *   "patientId": 5,
     *   "senderRole": "PATIENT",   // o "DOCTOR"
     *   "text": "Hola doctor, me duele el pecho"
     * }
     */
    private static String handleSendMessage(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int doctorId  = getInt(payload, "doctorId",  -1);
        int patientId = getInt(payload, "patientId", -1);
        String senderRole = getString(payload, "senderRole", null);
        String text       = getString(payload, "text", null);

        if (doctorId <= 0 || patientId <= 0) {
            return error(requestId, "SEND_MESSAGE",
                    "Missing or invalid doctorId / patientId");
        }
        if (senderRole == null || (!senderRole.equals("DOCTOR") && !senderRole.equals("PATIENT"))) {
            return error(requestId, "SEND_MESSAGE",
                    "senderRole must be 'DOCTOR' or 'PATIENT'");
        }
        if (text == null || text.isBlank()) {
            return error(requestId, "SEND_MESSAGE",
                    "Message text cannot be empty");
        }

        String timestamp = LocalDateTime.now().toString();

        Integer msgId = MessageDAO.insert(doctorId, patientId, senderRole, timestamp, text);
        if (msgId == null) {
            return error(requestId, "SEND_MESSAGE", "DB insert failed (message)");
        }

        JsonObject resp = baseResponse("SEND_MESSAGE", requestId, "OK", "Message stored");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("messageId", msgId);
        respPayload.addProperty("timestamp", timestamp);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    /**
     * LIST_MESSAGES
     * Payload esperado:
     * {
     *   "doctorId": 1,
     *   "patientId": 5
     * }
     */
    private static String handleListMessages(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int doctorId  = getInt(payload, "doctorId",  -1);
        int patientId = getInt(payload, "patientId", -1);

        if (doctorId <= 0 || patientId <= 0) {
            return error(requestId, "LIST_MESSAGES",
                    "You must provide doctorId and patientId in payload");
        }

        List<MessageDAO.MessageMeta> msgs =
                MessageDAO.listConversation(doctorId, patientId);

        JsonArray arr = new JsonArray();

        for (MessageDAO.MessageMeta m : msgs) {
            JsonObject jo = new JsonObject();

            // 🔥 CAMBIO NECESARIO
            jo.addProperty("messageId", m.getId());

            jo.addProperty("doctorId", m.getDoctorId());
            jo.addProperty("patientId", m.getPatientId());
            jo.addProperty("senderRole", m.getSenderRole());
            jo.addProperty("timestamp", m.getTimestamp());
            jo.addProperty("text", m.getText());

            arr.add(jo);
        }

        JsonObject resp = baseResponse("LIST_MESSAGES", requestId, "OK",
                "Conversation messages retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("messages", arr);
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