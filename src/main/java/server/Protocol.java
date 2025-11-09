package server;

import java.io.IOException;
import pojos.User;
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
        int newDoctorId = 1;

        JsonObject resp = baseResponse("REGISTER_DOCTOR", requestId, "OK",
                "Doctor registered successfully");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("doctorId", newDoctorId);
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    private static String handleLogin(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        String username = payload.has("username") ? payload.get("username").getAsString() : "";
        String password = payload.has("password") ? payload.get("password").getAsString() : "";

        // Aquí validarías en la BD
        boolean valid = true;

        if (!valid) {
            return error(requestId, "LOGIN", "Invalid username or password");
        }

        JsonObject resp = baseResponse("LOGIN", requestId, "OK", "Login successful");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("userId", 123);     // ID real
        respPayload.addProperty("role", "PATIENT"); // o DOCTOR
        respPayload.addProperty("token", "session-xyz");
        resp.add("payload", respPayload);
        return gson.toJson(resp);
    }

    private static String handleSendSymptoms(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        // mapear a Symptoms, guardar, etc.
        JsonObject resp = baseResponse("SEND_SYMPTOMS", requestId, "OK", "Symptoms received");
        resp.add("payload", new JsonObject());
        return gson.toJson(resp);
    }

    private static String handleSendMeasurement(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        JsonObject resp = baseResponse("SEND_MEASUREMENT", requestId, "OK", "Measurement received");
        resp.add("payload", new JsonObject());
        return gson.toJson(resp);
    }

    private static String handleRequestAppointment(JsonObject req, String requestId) {
        JsonObject payload = getPayload(req);
        int appointmentId = 42;

        JsonObject resp = baseResponse("REQUEST_APPOINTMENT", requestId, "OK",
                "Appointment created");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("appointmentId", appointmentId);
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

        // TODO: aquí guardarías el mensaje en la BD, notificarías al paciente, etc.
        int messageId = 5001; // ID generado por la BD

        JsonObject resp = baseResponse("SEND_MESSAGE", requestId, "OK", "Message delivered");
        JsonObject respPayload = new JsonObject();
        respPayload.addProperty("messageId", messageId);
        resp.add("payload", respPayload);

        return gson.toJson(resp);
    }

}



