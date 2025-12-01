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
        /*clearAllTables();*/
    }

    /*private static void clearAllTables() {
        try {
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
    }*/

    @Test
    void sendMessage_thenListConversation_ok() {

        // 1) Registrar doctor
        String docResp = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"REGISTER_DOCTOR",
          "requestId":"msg-doc-reg",
          "payload":{
            "name":"DocMsg",
            "surname":"One",
            "email":"doc@test.com",
            "password":"1234",
            "phone":"700000000"
          }
        }
        """);

        JsonObject joDoc = JsonParser.parseString(docResp).getAsJsonObject();
        int doctorId = joDoc.getAsJsonObject("payload").get("doctorId").getAsInt();

        // 2) Registrar paciente ASIGNANDO DOCTOR
        String patResp = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"REGISTER_PATIENT",
          "requestId":"msg-pat-reg",
          "payload":{
            "name":"PatMsg",
            "surname":"One",
            "email":"pat@test.com",
            "password":"1234",
            "dob":"1990-01-01",
            "sex":"F",
            "phone":"611111111",
            "doctorName":"DocMsg"
          }
        }
        """);

        JsonObject joPat = JsonParser.parseString(patResp).getAsJsonObject();
        int patientId = joPat.getAsJsonObject("payload").get("patientId").getAsInt();

        // 3) Enviar mensaje PACIENTE → DOCTOR
        String sendResp = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"SEND_MESSAGE",
          "requestId":"msg-send-1",
          "payload":{
            "doctorId": %d,
            "patientId": %d,
            "senderRole":"PATIENT",
            "text":"Hola doctor, tengo molestias por la noche."
          }
        }
        """.formatted(doctorId, patientId));

        JsonObject send = JsonParser.parseString(sendResp).getAsJsonObject();
        assertEquals("OK", send.get("status").getAsString());

        JsonObject payloadSend = send.getAsJsonObject("payload");
        assertTrue(payloadSend.get("messageId").getAsInt() > 0);

        // 4) Listar mensajes
        String listResp = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"LIST_MESSAGES",
          "requestId":"msg-list-1",
          "payload":{
            "doctorId": %d,
            "patientId": %d
          }
        }
        """.formatted(doctorId, patientId));

        JsonObject joList = JsonParser.parseString(listResp).getAsJsonObject();
        assertEquals("OK", joList.get("status").getAsString());

        JsonArray msgs = joList.getAsJsonObject("payload").getAsJsonArray("messages");

        assertFalse(msgs.isEmpty());
        JsonObject first = msgs.get(0).getAsJsonObject();

        assertEquals("PATIENT", first.get("senderRole").getAsString());
        assertEquals(doctorId, first.get("doctorId").getAsInt());
        assertEquals(patientId, first.get("patientId").getAsInt());
        assertTrue(first.get("text").getAsString().contains("Hola doctor"));
    }
}