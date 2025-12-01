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


public class ProtocolLoginFailTest {
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
            System.err.println("[TEST] clear tables error: " + e.getMessage());
        }
    }*/

    @Test
    void loginFails_wrongPassword() {
        // registrar doctor
        Protocol.process("""
        {
          "type":"REQUEST","action":"REGISTER_DOCTOR","requestId":"1",
          "payload":{"name":"A","surname":"B","email":"test@doc.com","password":"abc","phone":"111"}
        }
        """);

        // login con password incorrecta
        String loginResp = Protocol.process("""
        {
          "type":"REQUEST","action":"LOGIN","requestId":"2",
          "payload":{"username":"test@doc.com","password":"WRONG"}
        }
        """);

        JsonObject jo = JsonParser.parseString(loginResp).getAsJsonObject();
        assertEquals("ERROR", jo.get("status").getAsString());
        assertEquals("LOGIN", jo.get("action").getAsString());
    }

    @Test
    void loginFails_unknownEmail() {
        String loginResp = Protocol.process("""
        {
          "type":"REQUEST","action":"LOGIN","requestId":"3",
          "payload":{"username":"noexist@test.com","password":"123"}
        }
        """);

        JsonObject jo = JsonParser.parseString(loginResp).getAsJsonObject();
        assertEquals("ERROR", jo.get("status").getAsString());
        assertEquals("LOGIN", jo.get("action").getAsString());
    }
}
