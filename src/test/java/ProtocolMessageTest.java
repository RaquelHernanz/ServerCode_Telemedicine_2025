import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;


public class ProtocolMessageTest {
    @BeforeAll
    static void initDb() {
        DatabaseManager.connect();
        clearAllTables();
    }

    // Igual que en los otros tests: limpiamos todo
    private static void clearAllTables() {
        try {
            // NO usar try-with-resources con Connection, para no cerrarla
            Connection conn = DatabaseManager.get();
            try (Statement st = conn.createStatement()) {

                st.execute("DELETE FROM messages");
                st.execute("DELETE FROM appointments");
                st.execute("DELETE FROM measurements");
                st.execute("DELETE FROM symptoms");
                st.execute("DELETE FROM patients");
                st.execute("DELETE FROM doctors");

            }
        } catch (SQLException e) {
            System.err.println("[TEST] Error clearing tables: " + e.getMessage());
        }
    }

    @Test
    void sendMessage_thenListConversation_ok() {
        // 1) Registramos doctor y paciente
        String docEmail = "docmsg@test.com";
        String docPass  = "docMsg1!";
        String patEmail = "patmsg@test.com";
        String patPass  = "patMsg1!";

        String docReg = """
        {
          "type": "REQUEST",
          "action": "REGISTER_DOCTOR",
          "role": "DOCTOR",
          "requestId": "msg-doc-reg",
          "payload": {
            "name": "DocMsg",
            "surname": "One",
            "email": "%s",
            "password": "%s",
            "phone": "710000000"
          }
        }
        """.formatted(docEmail, docPass);

        String patReg = """
        {
          "type": "REQUEST",
          "action": "REGISTER_PATIENT",
          "role": "PATIENT",
          "requestId": "msg-pat-reg",
          "payload": {
            "name": "PatMsg",
            "surname": "One",
            "email": "%s",
            "password": "%s",
            "dob": "1992-02-02",
            "sex": "MALE",
            "phone": "610000000"
          }
        }
        """.formatted(patEmail, patPass);

        String docRegResp = Protocol.process(docReg);
        System.out.println("REGISTER_DOCTOR response = " + docRegResp);
        String patRegResp = Protocol.process(patReg);
        System.out.println("REGISTER_PATIENT response = " + patRegResp);

        JsonObject joDocReg = JsonParser.parseString(docRegResp).getAsJsonObject();
        JsonObject joPatReg = JsonParser.parseString(patRegResp).getAsJsonObject();

        int doctorId = joDocReg.getAsJsonObject("payload").get("doctorId").getAsInt();
        int patientId = joPatReg.getAsJsonObject("payload").get("patientId").getAsInt();

        // 2) SEND_MESSAGE desde el paciente al doctor
        String sendMsg = """
        {
          "type": "REQUEST",
          "action": "SEND_MESSAGE",
          "role": "PATIENT",
          "requestId": "msg-send-1",
          "payload": {
            "doctorId": %d,
            "patientId": %d,
            "senderRole": "PATIENT",
            "text": "Hola doctor, tengo molestias en el pecho por la noche."
          }
        }
        """.formatted(doctorId, patientId);

        String sendResp = Protocol.process(sendMsg);
        JsonObject joSend = JsonParser.parseString(sendResp).getAsJsonObject();
        System.out.println("SEND_MESSAGE response = " + sendResp);

        assertEquals("OK", joSend.get("status").getAsString());
        JsonObject payloadSend = joSend.getAsJsonObject("payload");
        assertNotNull(payloadSend);
        int msgId = payloadSend.get("messageId").getAsInt();
        assertTrue(msgId > 0);
        assertNotNull(payloadSend.get("timestamp").getAsString());

        // 3) LIST_MESSAGES (conversación doctor-paciente)
        String listMsg = """
        {
          "type": "REQUEST",
          "action": "LIST_MESSAGES",
          "role": "DOCTOR",
          "requestId": "msg-list-1",
          "payload": {
            "doctorId": %d,
            "patientId": %d
          }
        }
        """.formatted(doctorId, patientId);

        String listResp = Protocol.process(listMsg);
        JsonObject joList = JsonParser.parseString(listResp).getAsJsonObject();
        System.out.println("LIST_MESSAGES response = " + listResp);

        assertEquals("OK", joList.get("status").getAsString());
        JsonArray arr = joList.getAsJsonObject("payload").getAsJsonArray("messages");
        assertNotNull(arr);
        assertFalse(arr.isEmpty());

        JsonObject first = arr.get(0).getAsJsonObject();
        assertEquals("PATIENT", first.get("senderRole").getAsString());
        assertEquals(doctorId, first.get("doctorId").getAsInt());
        assertEquals(patientId, first.get("patientId").getAsInt());
        assertTrue(first.get("text").getAsString().contains("Hola doctor"));
    }
}
