
import com.google.gson.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import server.Protocol;
import server.database.DatabaseManager;

import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolListDoctorsTest {
    @BeforeAll
    static void init() {
        DatabaseManager.connect();
        try (Statement st = DatabaseManager.get().createStatement()) {
            st.execute("DELETE FROM doctors");
        } catch (Exception ignored) {}
    }

    @Test
    void listDoctors_ok() {

        String dreg = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"1",
          "payload":{"name":"A","surname":"B","email":"ld1@test.com","password":"1","phone":"111"}
        }
        """);

        System.out.println("REGISTER_DOCTOR response = " + dreg);

        String preg = Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"2",
          "payload":{"name":"C","surname":"D","email":"ld2@test.com","password":"1","phone":"222"}
        }
        """);

        System.out.println("REGISTER_PATIENT response = " + preg);

        String list = Protocol.process("""
        {
          "type":"REQUEST","action":"LIST_DOCTORS","requestId":"3",
          "payload":{}
        }
        """);

        System.out.println("LIST_PATIENTS response = " + list);

        JsonObject jo = JsonParser.parseString(list).getAsJsonObject();
        assertEquals("OK", jo.get("status").getAsString());

        JsonArray arr = jo.getAsJsonObject("payload").getAsJsonArray("doctors");
        assertTrue(arr.size() >= 2);
    }
}
