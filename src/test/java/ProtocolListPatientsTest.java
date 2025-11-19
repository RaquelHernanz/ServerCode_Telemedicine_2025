
import com.google.gson.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;

import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolListPatientsTest {
    @BeforeAll
    static void init() {
        DatabaseManager.connect();
        try (Statement st = DatabaseManager.get().createStatement()) {
            st.execute("DELETE FROM patients");
            st.execute("DELETE FROM doctors");
        } catch (Exception ignored) {}
    }

    @Test
    void listPatients_ok() {

        // registrar doctor
        String dreg = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"1",
          "payload":{"name":"D","surname":"X","email":"dx@test.com","password":"123","phone":"111"}
        }
        """);
        System.out.println("REGISTER_DOCTOR response = " + dreg);
        int did = JsonParser.parseString(dreg).getAsJsonObject()
                .getAsJsonObject("payload").get("doctorId").getAsInt();

        // registrar paciente asociado
        String preg = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_PATIENT","requestId":"2",
          "payload":{"name":"P","surname":"Y","email":"py@test.com",
                     "password":"123","dob":"2000","sex":"MALE","phone":"222"}
        }
        """);

        System.out.println("REGISTER_PATIENT response = " + preg);

        // listar pacientes
        String list = Protocol.process("""
        {
          "type":"REQUEST","action":"LIST_PATIENTS","requestId":"3",
          "payload":{"doctorId": %d}
        }
        """.formatted(did));

        System.out.println("LIST_PATIENTS response = " + list);

        JsonObject jo = JsonParser.parseString(list).getAsJsonObject();
        assertEquals("OK", jo.get("status").getAsString());

        JsonArray arr = jo.getAsJsonObject("payload").getAsJsonArray("patients");
        assertNotNull(arr);
    }
}
