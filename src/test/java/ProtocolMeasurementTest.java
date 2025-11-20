import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;
import server.database.MeasurementDAO;
import server.database.PatientDAO;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolMeasurementTest {

    @BeforeAll
    static void initDb() {
        DatabaseManager.connect();
        try {
            var conn = DatabaseManager.get();
            var st = conn.createStatement();
            st.execute("DELETE FROM messages");
            st.execute("DELETE FROM appointments");
            st.execute("DELETE FROM measurements");
            st.execute("DELETE FROM symptoms");
            st.execute("DELETE FROM patients");
            st.execute("DELETE FROM doctors");
        } catch (Exception ignored) {}
    }

    @Test
    void testSendMeasurementCreatesCSVAndDBEntry() throws Exception {

        // 1. Registrar doctor
        Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"0",
          "payload":{
            "name":"Pablo","surname":"Gomez",
            "email":"pablo@test.com","password":"1234","phone":"600111222"
          }
        }
        """);

        // 2. Registrar paciente
        Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_PATIENT","requestId":"1",
          "payload":{
            "name":"Ana","surname":"Lopez","email":"medtest@mail.com",
            "password":"1234","dob":"2000-05-01","sex":"F",
            "phone":"666222111","doctorName":"Pablo"
          }
        }
        """);

        Integer pid = PatientDAO.getIdByEmail("medtest@mail.com");
        assertNotNull(pid);

        // 3. Enviar measurement
        String response = Protocol.process("""
        {
          "type":"REQUEST","action":"SEND_MEASUREMENT","requestId":"2",
          "payload":{
            "patientId": %d,
            "type": "ECG",
            "date": "2025-11-20T10:00:00",
            "values": [523, 120, 350]
          }
        }
        """.formatted(pid));

        // 4. Verificar JSON respuesta
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertEquals("OK", json.get("status").getAsString());
        assertEquals("SEND_MEASUREMENT", json.get("action").getAsString());

        // 5. Comprobar ENTRADA en BD
        var meta = MeasurementDAO.listByPatientId(pid);
        assertEquals(1, meta.size());
        assertEquals("ECG", meta.get(0).getType());

        // 6. Comprobar que se generó el CSV
        String today = LocalDate.now().toString();
        File csv = new File("data/patient_" + pid + "/signals_" + today + ".csv");

        assertTrue(csv.exists(), "CSV file should exist");

        // 7. Contenido del CSV
        String content = Files.readString(csv.toPath());
        assertTrue(content.contains("523"));
        assertTrue(content.contains("120"));
        assertTrue(content.contains("350"));
    }
}