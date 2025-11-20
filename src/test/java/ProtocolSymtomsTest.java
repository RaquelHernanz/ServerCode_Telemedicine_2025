import com.google.gson.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolSymtomsTest {

    @BeforeAll
    static void init() {
        DatabaseManager.connect();
        try (Statement st = DatabaseManager.get().createStatement()) {
            st.execute("DELETE FROM messages");
            st.execute("DELETE FROM appointments");
            st.execute("DELETE FROM measurements");
            st.execute("DELETE FROM symptoms");
            st.execute("DELETE FROM patients");
            st.execute("DELETE FROM doctors");
        } catch (Exception ignored) {}
    }

    @Test
    void sendSymptoms_thenList_ok() {

        // Registrar primero un doctor (lo necesita REGISTER_PATIENT)
        Protocol.process("""
        {
          "type":"REQUEST",
          "action":"REGISTER_DOCTOR",
          "requestId":"doc1",
          "payload":{
            "name":"DocSym",
            "surname":"One",
            "email":"docsym@test.com",
            "password":"1111",
            "phone":"600000000"
          }
        }
        """);

        // Registrar paciente con DOCTOR NAME y fecha válida
        String reg = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"REGISTER_PATIENT",
          "requestId":"1",
          "payload":{
            "name":"P",
            "surname":"S",
            "email":"sym@test.com",
            "password":"123",
            "dob":"1999-01-01",
            "sex":"F",
            "phone":"111",
            "doctorName":"DocSym"
          }
        }
        """);

        System.out.println("REGISTER_PATIENT response = " + reg);

        JsonObject joReg = JsonParser.parseString(reg).getAsJsonObject();
        int pid = joReg.getAsJsonObject("payload").get("patientId").getAsInt();

        // Enviar síntomas
        String send = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"SEND_SYMPTOMS",
          "requestId":"2",
          "payload":{
            "patientId": %d,
            "description":"Dolor de cabeza fuerte"
          }
        }
        """.formatted(pid));

        JsonObject joSend = JsonParser.parseString(send).getAsJsonObject();
        assertEquals("OK", joSend.get("status").getAsString());

        // Listar síntomas
        String list = Protocol.process("""
        {
          "type":"REQUEST",
          "action":"LIST_SYMPTOMS",
          "requestId":"3",
          "payload":{"patientId": %d}
        }
        """.formatted(pid));

        JsonObject joList = JsonParser.parseString(list).getAsJsonObject();
        assertEquals("OK", joList.get("status").getAsString());

        JsonArray arr = joList.getAsJsonObject("payload").getAsJsonArray("symptoms");

        assertFalse(arr.isEmpty());
        assertTrue(arr.get(0).getAsJsonObject().get("description")
                .getAsString().contains("Dolor"));
    }
}