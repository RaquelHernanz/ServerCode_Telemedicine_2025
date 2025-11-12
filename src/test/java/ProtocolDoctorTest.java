
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import server.Protocol;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests básicos del protocolo JSON para doctores:
 * - REGISTER_DOCTOR
 * - LOGIN
 * Con trazas por consola para ver todo el proceso.
 */
public class ProtocolDoctorTest {

    private final Gson gson = new Gson();

    @Test
    void registerDoctor_thenLogin_ok() {
        System.out.println("==== TEST: registerDoctor_thenLogin_ok ====");

        // === 1) Construir petición REGISTER_DOCTOR ===
        String email = "doc@test.com";
        String password = "secret123";

        JsonObject userJson = new JsonObject();
        userJson.addProperty("username", email);
        userJson.addProperty("password", password);
        userJson.addProperty("role", "DOCTOR");

        JsonObject doctorJson = new JsonObject();
        doctorJson.addProperty("name", "John");
        doctorJson.addProperty("surname", "Doe");
        doctorJson.addProperty("email", email);
        doctorJson.addProperty("phonenumber", "123456789");

        JsonObject payload = new JsonObject();
        payload.add("user", userJson);
        payload.add("doctor", doctorJson);

        String requestIdRegister = "doc-reg-test-1";

        JsonObject registerReq = new JsonObject();
        registerReq.addProperty("type", "REQUEST");
        registerReq.addProperty("action", "REGISTER_DOCTOR");
        registerReq.addProperty("role", "DOCTOR");
        registerReq.addProperty("requestId", requestIdRegister);
        registerReq.add("payload", payload);

        String registerJson = gson.toJson(registerReq);

        System.out.println("[TEST] Enviando REGISTER_DOCTOR al protocolo:");
        System.out.println(registerJson);

        // === 2) Llamar al protocolo del servidor ===
        String registerResponseJson = Protocol.process(registerJson);

        System.out.println("[TEST] Respuesta REGISTER_DOCTOR del protocolo:");
        System.out.println(registerResponseJson);

        assertNotNull(registerResponseJson, "La respuesta de REGISTER_DOCTOR no debería ser null");

        JsonObject registerResp = gson.fromJson(registerResponseJson, JsonObject.class);

        // === 3) Comprobar campos de la respuesta REGISTER_DOCTOR ===
        assertEquals("RESPONSE", registerResp.get("type").getAsString());
        assertEquals("REGISTER_DOCTOR", registerResp.get("action").getAsString());
        assertEquals("OK", registerResp.get("status").getAsString());
        assertEquals(requestIdRegister, registerResp.get("requestId").getAsString());

        JsonObject registerPayload = registerResp.getAsJsonObject("payload");
        assertNotNull(registerPayload, "La respuesta debe incluir payload");

        int doctorId = registerPayload.get("doctorId").getAsInt();
        System.out.println("[TEST] Doctor registrado con id = " + doctorId);
        assertTrue(doctorId > 0, "El doctorId debería ser > 0");

        // === 4) Construir petición LOGIN con las mismas credenciales ===

        JsonObject loginPayload = new JsonObject();
        loginPayload.addProperty("username", email);
        loginPayload.addProperty("password", password);

        String requestIdLogin = "doc-login-test-1";

        JsonObject loginReq = new JsonObject();
        loginReq.addProperty("type", "REQUEST");
        loginReq.addProperty("action", "LOGIN");
        loginReq.addProperty("role", "DOCTOR");
        loginReq.addProperty("requestId", requestIdLogin);
        loginReq.add("payload", loginPayload);

        String loginJson = gson.toJson(loginReq);

        System.out.println("\n[TEST] Enviando LOGIN al protocolo:");
        System.out.println(loginJson);

        // === 5) Llamar al protocolo del servidor para LOGIN ===
        String loginResponseJson = Protocol.process(loginJson);

        System.out.println("[TEST] Respuesta LOGIN del protocolo:");
        System.out.println(loginResponseJson);

        assertNotNull(loginResponseJson, "La respuesta de LOGIN no debería ser null");

        JsonObject loginResp = gson.fromJson(loginResponseJson, JsonObject.class);

        // === 6) Comprobar campos de la respuesta LOGIN ===
        assertEquals("RESPONSE", loginResp.get("type").getAsString());
        assertEquals("LOGIN", loginResp.get("action").getAsString());
        assertEquals("OK", loginResp.get("status").getAsString());
        assertEquals(requestIdLogin, loginResp.get("requestId").getAsString());

        JsonObject loginRespPayload = loginResp.getAsJsonObject("payload");
        assertNotNull(loginRespPayload, "La respuesta de LOGIN debe incluir payload");

        String role = loginRespPayload.get("role").getAsString();
        System.out.println("[TEST] Rol devuelto en login = " + role);
        assertEquals("DOCTOR", role, "El rol devuelto en login debería ser DOCTOR");

        System.out.println("==== FIN TEST: registerDoctor_thenLogin_ok ====\n");
    }
}
