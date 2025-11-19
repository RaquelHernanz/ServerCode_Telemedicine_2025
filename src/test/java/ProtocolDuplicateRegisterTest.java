
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolDuplicateRegisterTest {
    @BeforeAll
    static void init() {
        DatabaseManager.connect();
        try (Statement st = DatabaseManager.get().createStatement()) {
            st.execute("DELETE FROM doctors");
            st.execute("DELETE FROM patients");
        } catch (Exception ignored) {}
    }

    @Test
    void duplicateDoctorEmail() {
        String reg1 = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"1",
          "payload":{"name":"A","surname":"B","email":"dup@test.com","password":"123","phone":"111"}
        }
        """);

        String reg2 = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"2",
          "payload":{"name":"C","surname":"D","email":"dup@test.com","password":"456","phone":"999"}
        }
        """);

        JsonObject jo2 = JsonParser.parseString(reg2).getAsJsonObject();
        assertEquals("ERROR", jo2.get("status").getAsString());
    }

    @Test
    void duplicatePatientEmail() {
        String reg1 = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_PATIENT","requestId":"1",
          "payload":{"name":"A","surname":"B","email":"p@test.com","password":"123","dob":"1990","sex":"MALE","phone":"111"}
        }
        """);

        String reg2 = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_PATIENT","requestId":"2",
          "payload":{"name":"C","surname":"D","email":"p@test.com","password":"456","dob":"1991","sex":"FEMALE","phone":"555"}
        }
        """);

        JsonObject jo = JsonParser.parseString(reg2).getAsJsonObject();
        assertEquals("ERROR", jo.get("status").getAsString());
    }
}
