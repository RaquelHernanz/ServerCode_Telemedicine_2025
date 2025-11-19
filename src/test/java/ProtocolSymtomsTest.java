
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
            st.execute("DELETE FROM symptoms");
            st.execute("DELETE FROM patients");
            st.execute("DELETE FROM doctors");
        } catch (Exception ignored) {}
    }

    @Test
    void sendSymptoms_thenList_ok() {

        // registrar paciente
        String reg = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_PATIENT","requestId":"1",
          "payload":{"name":"P","surname":"S","email":"sym@test.com","password":"123",
                     "dob":"1999","sex":"FEMALE","phone":"111"}
        }
        """);

        System.out.println("REGISTER_PATIENT response = " + reg);
        int pid = JsonParser.parseString(reg)
                .getAsJsonObject().getAsJsonObject("payload").get("patientId").getAsInt();

        // enviar síntomas
        String send = Protocol.process("""
        {
          "type":"REQUEST","action":"SEND_SYMPTOMS","requestId":"2",
          "payload":{
              "patientId": %d,
              "description":"Dolor de cabeza fuerte"
          }
        }
        """.formatted(pid));
        System.out.println("SEND_SYMPTOMS response = " + send);

        JsonObject joSend = JsonParser.parseString(send).getAsJsonObject();
        assertEquals("OK", joSend.get("status").getAsString());

        // listar síntomas
        String list = Protocol.process("""
        {
          "type":"REQUEST","action":"LIST_SYMPTOMS","requestId":"3",
          "payload":{"patientId": %d}
        }
        """.formatted(pid));

        System.out.println("LIST_SYMPTOMS response = " + list);

        JsonObject joList = JsonParser.parseString(list).getAsJsonObject();
        assertEquals("OK", joList.get("status").getAsString());

        JsonArray arr = joList.getAsJsonObject("payload").getAsJsonArray("symptoms");
        assertFalse(arr.isEmpty());
        assertTrue(arr.get(0).getAsJsonObject().get("description")
                .getAsString().contains("Dolor"));
    }
}
