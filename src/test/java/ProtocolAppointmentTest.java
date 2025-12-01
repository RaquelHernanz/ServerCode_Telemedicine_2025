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

public class ProtocolAppointmentTest {

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
            System.err.println("[TEST] error clearing tables: " + e.getMessage());
        }
    }*/

    @Test
    void requestAppointment_thenList_ok() {

        // --- Registrar doctor ---
        Protocol.process("""
        {
          "type": "REQUEST",
          "action": "REGISTER_DOCTOR",
          "requestId": "appt-doc-reg",
          "payload": {
            "name": "DocAppt",
            "surname": "One",
            "email": "docappt@test.com",
            "password": "docPass1!",
            "phone": "700000000"
          }
        }
        """);

        // --- Registrar paciente con doctorName obligatorio ---
        String patRegResp = Protocol.process("""
        {
          "type": "REQUEST",
          "action": "REGISTER_PATIENT",
          "requestId": "appt-pat-reg",
          "payload": {
            "name": "PatAppt",
            "surname": "One",
            "email": "patappt@test.com",
            "password": "patPass1!",
            "dob": "1990-01-01",
            "sex": "FEMALE",
            "phone": "600000000",
            "doctorName": "DocAppt"
          }
        }
        """);

        JsonObject joPatReg = JsonParser.parseString(patRegResp).getAsJsonObject();
        int patientId = joPatReg.getAsJsonObject("payload").get("patientId").getAsInt();

        // Obtener doctorId
        JsonObject joDoc = JsonParser.parseString(
                Protocol.process("""
            {
             "type":"REQUEST",
             "action":"LIST_DOCTORS",
             "requestId":"list-doc"
            }
            """)
        ).getAsJsonObject();

        int doctorId = joDoc
                .getAsJsonObject("payload")
                .getAsJsonArray("doctors")
                .get(0).getAsJsonObject().get("doctorId").getAsInt();

        // --- Crear cita ---
        String apptResp = Protocol.process("""
        {
          "type": "REQUEST",
          "action": "REQUEST_APPOINTMENT",
          "requestId": "appt-req-1",
          "payload": {
            "doctorId": %d,
            "patientId": %d,
            "datetime": "2025-11-20T10:30:00",
            "message": "Dolor torácico intermitente"
          }
        }
        """.formatted(doctorId, patientId));

        JsonObject joAppt = JsonParser.parseString(apptResp).getAsJsonObject();
        assertEquals("OK", joAppt.get("status").getAsString());
        int appointmentId = joAppt.getAsJsonObject("payload").get("appointmentId").getAsInt();
        assertTrue(appointmentId > 0);

        // --- Listar citas ---
        String listResp = Protocol.process("""
        {
          "type": "REQUEST",
          "action": "LIST_APPOINTMENTS",
          "requestId": "appt-list-doc",
          "payload": {
            "doctorId": %d
          }
        }
        """.formatted(doctorId));

        JsonObject joList = JsonParser.parseString(listResp).getAsJsonObject();

        assertEquals("OK", joList.get("status").getAsString());
        JsonArray arr = joList.getAsJsonObject("payload").getAsJsonArray("appointments");

        assertNotNull(arr);
        assertFalse(arr.isEmpty());

        JsonObject first = arr.get(0).getAsJsonObject();
        assertEquals(doctorId, first.get("doctorId").getAsInt());
        assertEquals(patientId, first.get("patientId").getAsInt());
        assertEquals("2025-11-20T10:30:00", first.get("datetime").getAsString());
    }
}