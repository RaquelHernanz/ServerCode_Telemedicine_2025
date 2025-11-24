package utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

//Current version: PBKDF2WithHmacSHA256, more secure and harder to broke
public class Encryption {

    // Convertir una contraseña en un hash seguro usando PBKDF2 + HMAC + SHA-256.
    // estándar para guardar contraseñas en una base de datos.
    // servidor compara: hash(introducido) == hash(guardado)
    // evita guardar contraseñas en texto plano, lo cual sería INSEGURO.

    // cliente envía la contraseña en texto plano (por socket)
    // el servidor la recibe, la pasa por PBKDF2 y guarda el hash
    // servidor DEBE manejar la encriptación para controlar el proceso de seguridad.

    // PBKDF2 = Password-Based Key Derivation Function 2

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256"; // con el que estamos trabajando
    private static final int ITERATIONS = 65536; // repeticiones para hacerlo lento (pero seguro), cuantas más iteraciones más dificil romperlo
    private static final int KEY_LENGTH = 256; // tamaño final del hash en bits (256 bits = 32 bytes = 64 hex chars)

    // Salt fija: mantiene compatibilidad con tu flujo actual
    // SALT: valor que se mezcla con la contraseña para evitar: que dos usuarios puedan tener el mismo hash si tienen la misma contraseña, ataques
    private static final byte[] SALT =
            "Telemedicine_2025_SALT".getBytes(StandardCharsets.UTF_8);

    public static String encryptPassword(String password) {
        // recibe la contraseña introducida y la transforma en un hash seguro.
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), // contraseña
                    SALT, // salt value
                    ITERATIONS, // iterations
                    KEY_LENGTH // longitud del hash final
            );

            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM); // inicializa PBKDF2 con HMAC-SHA256.
            byte[] hash = skf.generateSecret(spec).getEncoded(); // genera el hash
            // 65 mil rondas de hashing
            // mezclando contraseña + salt
            // produciendo un hash final seguro
            return bytesToHex(hash); // Porque la base de datos guarda strings, no bytes.

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al encriptar la contraseña", e);
        }
    }

    private static String bytesToHex(byte[] bytes) { // pasa de bytes a string
        // una base de datos NO guarda bytes fácilmente,
        // JSON NO transporta bytes bien,
        // ver el hash como un string.
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }
}
