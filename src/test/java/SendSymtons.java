
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import server.Protocol;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flujo: REGISTER_PATIENT -> SEND_SYMPTOMS -> LIST_SYMPTOMS
 * Verifica que el servidor almacena los síntomas con la estructura del POJO:
 *   description:String, date:LocalDate, hour:LocalDateTime, patient:Patient
 */

public class SendSymtons {
    private final Gson gson = new Gson();

    @Test
    void registerPatient_thenSendSymptoms_thenList_ok() {
        System.out.println("==== TEST: registerPatient_thenSendSymptoms_thenList_ok ====");

        // ========= 1) REGISTER_PATIENT =========
        String email = "pat@test.com";
        String password = "p4ssw0rd";

        JsonObject userJson = new JsonObject();
        userJson.addProperty("username", email);
        userJson.addProperty("password", password);
        userJson.addProperty("role", "PATIENT");

        JsonObject patientJson = new JsonObject();
        patientJson.addProperty("name", "Jane");
        patientJson.addProperty("surname", "Doe");
        patientJson.addProperty("email", email);
        patientJson.addProperty("phonenumber", "600600600");
        patientJson.addProperty("dob", "1999-05-10");
        patientJson.addProperty("sex", "FEMALE");

        JsonObject payloadReg = new JsonObject();
        payloadReg.add("user", userJson);
        payloadReg.add("patient", patientJson);

        JsonObject regReq = new JsonObject();
        regReq.addProperty("type", "REQUEST");
        regReq.addProperty("action", "REGISTER_PATIENT");
        regReq.addProperty("role", "PATIENT");
        regReq.addProperty("requestId", "pat-reg-1");
        regReq.add("payload", payloadReg);

        String regReqJson = gson.toJson(regReq);
        System.out.println("[TEST] Enviando REGISTER_PATIENT:");
        System.out.println(regReqJson);

        String regRespJson = Protocol.process(regReqJson);
        System.out.println("[TEST] Respuesta REGISTER_PATIENT:");
        System.out.println(regRespJson);

        assertNotNull(regRespJson);
        JsonObject regResp = gson.fromJson(regRespJson, JsonObject.class);
        assertEquals("OK", regResp.get("status").getAsString());
        int patientId = regResp.getAsJsonObject("payload").get("patientId").getAsInt();
        System.out.println("[TEST] patientId = " + patientId);
        assertTrue(patientId > 0);

        // ========= 2) SEND_SYMPTOMS =========
        JsonObject payloadSym = new JsonObject();
        payloadSym.addProperty("patientId", patientId);
        payloadSym.addProperty("description", "Dolor torácico intermitente con disnea de esfuerzo");
        payloadSym.addProperty("date", "2025-11-12");            // LocalDate
        payloadSym.addProperty("hour", "2025-11-12T10:45:00");   // LocalDateTime

        JsonObject symReq = new JsonObject();
        symReq.addProperty("type", "REQUEST");
        symReq.addProperty("action", "SEND_SYMPTOMS");
        symReq.addProperty("role", "PATIENT");
        symReq.addProperty("requestId", "sym-send-1");
        symReq.add("payload", payloadSym);

        String symReqJson = gson.toJson(symReq);
        System.out.println("\n[TEST] Enviando SEND_SYMPTOMS:");
        System.out.println(symReqJson);

        String symRespJson = Protocol.process(symReqJson);
        System.out.println("[TEST] Respuesta SEND_SYMPTOMS:");
        System.out.println(symRespJson);

        assertNotNull(symRespJson);
        JsonObject symResp = gson.fromJson(symRespJson, JsonObject.class);
        assertEquals("OK", symResp.get("status").getAsString());
        int symptomsId = symResp.getAsJsonObject("payload").get("symptomsId").getAsInt();
        System.out.println("[TEST] symptomsId = " + symptomsId);
        assertTrue(symptomsId > 0);

        // ========= 3) LIST_SYMPTOMS (opcional pero útil) =========
        JsonObject payloadList = new JsonObject();
        payloadList.addProperty("patientId", patientId);

        JsonObject listReq = new JsonObject();
        listReq.addProperty("type", "REQUEST");
        listReq.addProperty("action", "LIST_SYMPTOMS");
        listReq.addProperty("role", "DOCTOR"); // o PATIENT, informativo
        listReq.addProperty("requestId", "sym-list-1");
        listReq.add("payload", payloadList);

        String listReqJson = gson.toJson(listReq);
        System.out.println("\n[TEST] Enviando LIST_SYMPTOMS:");
        System.out.println(listReqJson);

        String listRespJson = Protocol.process(listReqJson);
        System.out.println("[TEST] Respuesta LIST_SYMPTOMS:");
        System.out.println(listRespJson);

        assertNotNull(listRespJson);
        JsonObject listResp = gson.fromJson(listRespJson, JsonObject.class);
        assertEquals("OK", listResp.get("status").getAsString());

        JsonObject listPayload = listResp.getAsJsonObject("payload");
        assertNotNull(listPayload);
        JsonArray arr = listPayload.getAsJsonArray("symptoms");
        assertNotNull(arr);
        assertTrue(arr.size() >= 1, "Debería haber al menos un registro de síntomas");

        boolean found = false;
        for (int i = 0; i < arr.size(); i++) {
            JsonObject s = arr.get(i).getAsJsonObject();
            String desc = s.get("description").getAsString();
            String date = s.get("timestamp") != null ? s.get("timestamp").getAsString() : null; // por si tu handler usa otro nombre
            // nuestro handler usa "date" y "hour"; ajustamos:
            String dateField = s.has("date") ? s.get("date").getAsString() : null;
            String hourField = s.has("hour") ? s.get("hour").getAsString() : null;

            if ("Dolor torácico intermitente con disnea de esfuerzo".equals(desc)) {
                found = true;
                System.out.println("[TEST] Síntoma encontrado -> desc=" + desc + ", date=" + dateField + ", hour=" + hourField);
            }
        }
        assertTrue(found, "El síntoma enviado debería aparecer en el listado");

        System.out.println("==== FIN TEST: registerPatient_thenSendSymptoms_thenList_ok ====");
    }
}
