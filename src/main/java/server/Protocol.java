package server;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import pojos.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

//Parte de los métodos siguen vacíos, antes hay que configurar al paciente y el

public class Protocol {

    private static final Gson gson = new Gson();

    public static String process(String message) {
        try {
            JsonObject req = gson.fromJson(message, JsonObject.class);
            if (req == null) {
                return error(null, "UNKNOWN", "Empty message");
            }

            String action = req.has("action") ? req.get("action").getAsString() : null;
            String requestId = req.has("requestId") ? req.get("requestId").getAsString() : null;

            if (action == null) {
                return error(requestId, "UNKNOWN", "Missing 'action' field");
            }

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
                case "REQUEST_APPOINTMENT":
                    return handleRequestAppointment(req, requestId);
                case "LIST_APPOINTMENTS":
                    return handleListAppointments(req, requestId);
                case "LIST_MEASUREMENTS":
                    return handleListMeasurements(req, requestId);
                case "LIST_SYMPTOMS":
                    return handleListSymptoms(req, requestId);
                case "LIST_DOCTORS":
                    return handleListDoctors(req, requestId);
                default:
                    return error(requestId, action, "Unknown action: " + action);
            }

        } catch (JsonSyntaxException e) {
            return error(null, "UNKNOWN", "Invalid JSON: " + e.getMessage());
        } catch (Exception e) {
            return error(null, "UNKNOWN", "Internal error: " + e.getMessage());
        }
    }

    private static String handleRegisterPatient(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        // Aquí conectarías con tu lógica/DAO y tus POJOs Patient/User
        int newPatientId = 1; // sustituir por el de la BD

        JsonObject resp = baseResponse("REGISTER_PATIENT", requestId, "OK",
                "Patient registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("patientId", newPatientId);
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    private static String handleRegisterDoctor(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        // payload debe venir con dos objetos:
        // - user   (username/password/role)
        // - doctor (name/surname/email/phonenumber)
        JsonObject userJson = payload.getAsJsonObject("user");
        JsonObject doctorJson = payload.getAsJsonObject("doctor");

        String username = userJson.get("username").getAsString(); // normalmente email
        String password = userJson.get("password").getAsString();
        // String roleStr = userJson.get("role").getAsString(); // aquí sabes que será "DOCTOR"

        // Crear el POJO Doctor a partir del JSON
        pojos.Doctor doctor = new pojos.Doctor(
                doctorJson.get("name").getAsString(),
                doctorJson.get("surname").getAsString(),
                doctorJson.get("email").getAsString(),
                doctorJson.get("phonenumber").getAsString(),
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>(),
                new java.util.ArrayList<>()
        );

        int newDoctorId;
        try {
            // Registramos al doctor y guardamos credenciales
            newDoctorId = DataStorage.registerDoctor(doctor, password);
        } catch (IllegalArgumentException e) {
            // Por ejemplo, email duplicado
            return error(requestId, "REGISTER_DOCTOR", e.getMessage());
        }

        JsonObject resp = baseResponse("REGISTER_DOCTOR", requestId, "OK",
                "Doctor registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("doctorId", newDoctorId);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    private static String handleListDoctors(JsonObject req, String requestId) {
        java.util.List<Doctor> doctors = DataStorage.listAllDoctors();

        JsonObject resp = baseResponse("LIST_DOCTORS", requestId, "OK",
                "Doctor list retrieved");

        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        for (Doctor d : doctors) {
            JsonObject dJson = new JsonObject();
            dJson.addProperty("id", d.getId());
            dJson.addProperty("name", d.getName());
            dJson.addProperty("surname", d.getSurname());
            dJson.addProperty("email", d.getEmail());
            dJson.addProperty("phonenumber", d.getPhonenumber());
            arr.add(dJson);
        }

        JsonObject payload = new JsonObject();
        payload.add("doctors", arr);
        resp.add("payload", payload);

        return gson.toJson(resp);
    }


    private static String handleLogin(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        String username = payload.has("username") ? payload.get("username").getAsString() : "";
        String password = payload.has("password") ? payload.get("password").getAsString() : "";

        User user = DataStorage.validateLogin(username, password);
        if (user == null) {
            return error(requestId, "LOGIN", "Invalid username or password");
        }

        JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful");
        JsonObject respPayload = new JsonObject();

        // De momento no tenemos userId real; puedes poner 0 o generar uno si extiendes DataStorage
        respPayload.addProperty("userId", 0);
        respPayload.addProperty("role", user.getRole().name());
        respPayload.addProperty("token", "session-" + username);

        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }


    private static String handleSendSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int patientId = payload.get("patientId").getAsInt();
        String description = payload.get("description").getAsString();
        String hourStr = payload.get("hour").getAsString();   // "2025-11-09T10:30:00"

        Patient p = DataStorage.getPatientById(patientId);
        if (p == null) {
            return error(requestId, "SEND_SYMPTOMS", "Patient not found");
        }
        LocalDateTime date_hour = LocalDateTime.parse(hourStr);

        Symptoms s = DataStorage.addSymptoms(p, description, date_hour);

        JsonObject resp = baseResponse("SEND_SYMPTOMS", requestId, "OK",
                "Symptoms stored");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("symptomsId", s.getId());
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    // En el switch:
// case "SEND_MEASUREMENT":
//     return handleSendMeasurement(request, requestId);

    private static String handleSendMeasurement(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int patientId = payload.get("patientId").getAsInt();
        String typeStr = payload.get("type").getAsString();
        String dateStr = payload.get("date").getAsString();

        JsonArray valuesJson = payload.getAsJsonArray("values");
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < valuesJson.size(); i++) {
            values.add(valuesJson.get(i).getAsInt());
        }

        Patient p = DataStorage.getPatientById(patientId);
        if (p == null) {
            return error(requestId, "SEND_MEASUREMENT", "Patient not found");
        }

        LocalDateTime date = LocalDateTime.parse(dateStr);
        Measurement.Type type = Measurement.Type.valueOf(typeStr);

        Measurement m = DataStorage.addMeasurement(p, type, values, date);

        JsonObject resp = baseResponse("SEND_MEASUREMENT", requestId, "OK",
                "Measurement stored");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("measurementId", m.getId());
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }


    private static String handleRequestAppointment(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int patientId = payload.get("patientId").getAsInt();
        int doctorId = payload.get("doctorId").getAsInt();
        String dateStr = payload.get("date").getAsString();
        String message = payload.get("message").getAsString();

        Patient p = DataStorage.getPatientById(patientId);
        Doctor d = DataStorage.getDoctorById(doctorId);
        if (p == null || d == null) {
            return error(requestId, "REQUEST_APPOINTMENT", "Patient or doctor not found");
        }

        LocalDateTime date = LocalDateTime.parse(dateStr);

        Appointment appt = DataStorage.addAppointment(p, d, date, message);

        JsonObject resp = baseResponse("REQUEST_APPOINTMENT", requestId, "OK",
                "Appointment created");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("appointmentId", appt.getId());
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

    private static String handleListAppointments(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        JsonObject resp = baseResponse("LIST_APPOINTMENTS", requestId, "OK",
                "Appointments retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("appointments", gson.toJsonTree(new Object[0]));
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    private static String handleListMeasurements(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        JsonObject resp = baseResponse("LIST_MEASUREMENTS", requestId, "OK",
                "Measurements retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("measurements", gson.toJsonTree(new Object[0]));
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    private static String handleListSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        JsonObject resp = baseResponse("LIST_SYMPTOMS", requestId, "OK",
                "Symptoms retrieved");
        JsonObject respPayload = new JsonObject();
        respPayload.add("symptoms", gson.toJsonTree(new Object[0]));
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    // ========= Helpers =========

    private static JsonObject getPayload(JsonObject req) {
        return req.has("payload") && req.get("payload").isJsonObject()
                ? req.getAsJsonObject("payload")
                : new JsonObject();
    }

    private static JsonObject baseResponse(String action, String requestId,
                                           String status, String msg) {
        JsonObject resp = new JsonObject();
        resp.addProperty("type", "RESPONSE");
        resp.addProperty("action", action);
        if (requestId != null) resp.addProperty("requestId", requestId);
        resp.addProperty("status", status);   // OK / ERROR
        resp.addProperty("message", msg);
        return resp;
    }

    private static String error(String requestId, String action, String msg) {
        JsonObject resp = baseResponse(
                action != null ? action : "UNKNOWN",
                requestId,
                "ERROR",
                msg
        );
        resp.add("payload", new JsonObject());
        return gson.toJson(resp);
    }

    private static String handleSendMessage(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);

        int fromDoctorId = payload.get("fromDoctorId").getAsInt();
        int toPatientId  = payload.get("toPatientId").getAsInt();
        String content   = payload.get("content").getAsString();
        String timestamp = payload.get("timestamp").getAsString();

        // Aquí se guardará el mensaje en la BD, notificar al paciente, etc.
        int messageId = 5001; // ID generado por la BD

        JsonObject resp = baseResponse("SEND_MESSAGE", requestId, "OK", "Message delivered");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("messageId", messageId);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

}



