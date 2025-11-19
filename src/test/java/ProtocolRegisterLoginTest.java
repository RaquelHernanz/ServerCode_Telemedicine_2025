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

public class ProtocolRegisterLoginTest {
    @BeforeAll
    static void initDb() {
        // Conecta a la BD "normal" del server.
        // Si tienes otra URL de test, cámbiala en DatabaseManager.
        DatabaseManager.connect();
        clearAllTables();
    }

    // Limpia tablas para que los tests sean reproducibles
    private static void clearAllTables() {
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
    }

    @Test
    void registerAndLoginPatient_ok() {
        String email = "patient@test.com";
        String password = "p4ssw0rd";

        // 1) REGISTER_PATIENT
        String registerJson = """
        {
          "type": "REQUEST",
          "action": "REGISTER_PATIENT",
          "role": "PATIENT",
          "requestId": "pat-reg-1",
          "payload": {
            "name": "Jane",
            "surname": "Doe",
            "email": "%s",
            "password": "%s",
            "dob": "1999-05-10",
            "sex": "FEMALE",
            "phone": "600600600"
          }
        }
        """.formatted(email, password);

        String regResp = Protocol.process(registerJson);
        JsonObject joReg = JsonParser.parseString(regResp).getAsJsonObject();

        assertEquals("RESPONSE", joReg.get("type").getAsString());
        assertEquals("REGISTER_PATIENT", joReg.get("action").getAsString());
        assertEquals("OK", joReg.get("status").getAsString());

        JsonObject payloadReg = joReg.getAsJsonObject("payload");
        assertNotNull(payloadReg);
        int patientId = payloadReg.get("patientId").getAsInt();
        assertTrue(patientId > 0);

        // 2) LOGIN con email y contraseña en claro (el server hashea)
        String loginJson = """
        {
          "type": "REQUEST",
          "action": "LOGIN",
          "role": "PATIENT",
          "requestId": "pat-login-1",
          "payload": {
            "username": "%s",
            "password": "%s"
          }
        }
        """.formatted(email, password);

        String loginResp = Protocol.process(loginJson);
        JsonObject joLogin = JsonParser.parseString(loginResp).getAsJsonObject();

        assertEquals("RESPONSE", joLogin.get("type").getAsString());
        assertEquals("LOGIN", joLogin.get("action").getAsString());
        assertEquals("OK", joLogin.get("status").getAsString());

        JsonObject payloadLogin = joLogin.getAsJsonObject("payload");
        assertNotNull(payloadLogin);
        assertEquals("PATIENT", payloadLogin.get("role").getAsString());
        assertEquals(patientId, payloadLogin.get("userId").getAsInt());
        assertTrue(payloadLogin.get("token").getAsString().startsWith("session-"));
    }

    @Test
    void registerAndLoginDoctor_ok() {
        String email = "doctor@test.com";
        String password = "d0ctorPass";

        // 1) REGISTER_DOCTOR
        String registerJson = """
        {
          "type": "REQUEST",
          "action": "REGISTER_DOCTOR",
          "role": "DOCTOR",
          "requestId": "doc-reg-1",
          "payload": {
            "name": "John",
            "surname": "Smith",
            "email": "%s",
            "password": "%s",
            "phone": "700700700"
          }
        }
        """.formatted(email, password);

        String regResp = Protocol.process(registerJson);
        JsonObject joReg = JsonParser.parseString(regResp).getAsJsonObject();

        System.out.println("REGISTER_DOCTOR response = " + regResp);

        assertEquals("RESPONSE", joReg.get("type").getAsString());
        assertEquals("REGISTER_DOCTOR", joReg.get("action").getAsString());
        assertEquals("OK", joReg.get("status").getAsString());

        JsonObject payloadReg = joReg.getAsJsonObject("payload");
        assertNotNull(payloadReg);
        int doctorId = payloadReg.get("doctorId").getAsInt();
        assertTrue(doctorId > 0);

        // 2) LOGIN como doctor
        String loginJson = """
        {
          "type": "REQUEST",
          "action": "LOGIN",
          "role": "DOCTOR",
          "requestId": "doc-login-1",
          "payload": {
            "username": "%s",
            "password": "%s"
          }
        }
        """.formatted(email, password);

        String loginResp = Protocol.process(loginJson);
        JsonObject joLogin = JsonParser.parseString(loginResp).getAsJsonObject();

        assertEquals("RESPONSE", joLogin.get("type").getAsString());
        assertEquals("LOGIN", joLogin.get("action").getAsString());
        assertEquals("OK", joLogin.get("status").getAsString());

        JsonObject payloadLogin = joLogin.getAsJsonObject("payload");
        assertNotNull(payloadLogin);
        assertEquals("DOCTOR", payloadLogin.get("role").getAsString());
        assertEquals(doctorId, payloadLogin.get("userId").getAsInt());
        assertTrue(payloadLogin.get("token").getAsString().startsWith("session-"));
    }
}
