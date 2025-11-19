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
        clearAllTables();
    }

    // Limpia tablas para que los tests sean reproducibles
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
    void requestAppointment_thenList_ok() {
        // 1) Registramos doctor y paciente
        String docEmail = "docappt@test.com";
        String docPass  = "docPass1!";
        String patEmail = "patappt@test.com";
        String patPass  = "patPass1!";

        String docReg = """
        {
          "type": "REQUEST",
          "action": "REGISTER_DOCTOR",
          "role": "DOCTOR",
          "requestId": "appt-doc-reg",
          "payload": {
            "name": "DocAppt",
            "surname": "One",
            "email": "%s",
            "password": "%s",
            "phone": "700000000"
          }
        }
        """.formatted(docEmail, docPass);

        String patReg = """
        {
          "type": "REQUEST",
          "action": "REGISTER_PATIENT",
          "role": "PATIENT",
          "requestId": "appt-pat-reg",
          "payload": {
            "name": "PatAppt",
            "surname": "One",
            "email": "%s",
            "password": "%s",
            "dob": "1990-01-01",
            "sex": "FEMALE",
            "phone": "600000000"
          }
        }
        """.formatted(patEmail, patPass);

        String docRegResp = Protocol.process(docReg);
        String patRegResp = Protocol.process(patReg);

        JsonObject joDocReg = JsonParser.parseString(docRegResp).getAsJsonObject();
        JsonObject joPatReg = JsonParser.parseString(patRegResp).getAsJsonObject();

        int doctorId = joDocReg.getAsJsonObject("payload").get("doctorId").getAsInt();
        int patientId = joPatReg.getAsJsonObject("payload").get("patientId").getAsInt();

        // 2) REQUEST_APPOINTMENT
        String reqAppt = """
        {
          "type": "REQUEST",
          "action": "REQUEST_APPOINTMENT",
          "role": "PATIENT",
          "requestId": "appt-req-1",
          "payload": {
            "doctorId": %d,
            "patientId": %d,
            "datetime": "2025-11-20T10:30:00",
            "message": "Dolor torácico intermitente"
          }
        }
        """.formatted(doctorId, patientId);

        String apptResp = Protocol.process(reqAppt);
        JsonObject joAppt = JsonParser.parseString(apptResp).getAsJsonObject();

        assertEquals("OK", joAppt.get("status").getAsString());
        JsonObject payloadAppt = joAppt.getAsJsonObject("payload");
        assertNotNull(payloadAppt);
        int appointmentId = payloadAppt.get("appointmentId").getAsInt();
        assertTrue(appointmentId > 0);

        // 3) LIST_APPOINTMENTS por doctor
        String listByDoctor = """
        {
          "type": "REQUEST",
          "action": "LIST_APPOINTMENTS",
          "role": "DOCTOR",
          "requestId": "appt-list-doc",
          "payload": {
            "doctorId": %d
          }
        }
        """.formatted(doctorId);

        String listResp = Protocol.process(listByDoctor);
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
