package server;

import com.google.gson.JsonArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Guarda y lee señales de BITalino en CSV por carpeta de paciente.
 * Estructura: data/<carpeta>/signals_YYYY-MM-DD.csv
 */
public class DataStorage {

    private static final Path BASE = Paths.get("data"); // carpeta base "data"
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Carpeta del "paciente" (puede ser el email o "patient_<id>")
    private static Path patientDir(String folder) {
        return BASE.resolve(folder);
    }

    // Crea la carpeta si no existe
    private static void ensurePatientDir(String folder) throws IOException {
        Files.createDirectories(patientDir(folder));
    }

    /**
     * Añade filas "timestamp,ecg,eda" al CSV del día. Devuelve la ruta al fichero.
     * @param folder carpeta (ej. "ana@demo.es" o "patient_42")
     * @param rows JsonArray con strings tipo "0.00,523,0.12" o "0,523,-"
     */
    public static synchronized String appendRowsToCsv(String folder, JsonArray rows) {
        try {
            ensurePatientDir(folder); // asegura la carpeta

            // Nombre del fichero con fecha del día
            String today = LocalDate.now().format(DATE_FMT);
            Path file = patientDir(folder).resolve("signals_" + today + ".csv");

            boolean newFile = !Files.exists(file); // para escribir cabecera si es nuevo

            // Abrimos para APPEND (crear si no existe)
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                // Escribir cabecera la primera vez
                if (newFile) {
                    bw.write("timestamp,ecg,eda");
                    bw.newLine();
                }

                // Escribir cada fila
                for (var el : rows) {
                    String row = el.getAsString();
                    bw.write(row);
                    bw.newLine();
                }
            }

            // Devolver ruta como String (se guarda en BD como meta)
            return file.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return null; // en caso de error
        }
    }

    /**
     * Lee el CSV del día y devuelve un trozo de JSON:
     * {"header":"timestamp,ecg,eda","rows":["...","..."]}
     */
    public static synchronized String loadTodayAsJsonPayload(String folder) {
        try {
            String today = LocalDate.now().format(DATE_FMT);
            Path file = patientDir(folder).resolve("signals_" + today + ".csv");
            if (!Files.exists(file)) return null;

            String header = null;
            JsonArray rows = new JsonArray();

            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line; boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    if (first) { header = line.trim(); first = false; }
                    else { rows.add(line.trim()); }
                }
            }

            if (header == null) return null;
            return "{\"header\":\"" + header + "\",\"rows\":" + rows.toString() + "}";

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lee un CSV cualquiera (por ruta absoluta o relativa) y devuelve:
     * {"header":"timestamp,ecg,eda","rows":["...","..."]}
     */
    public static synchronized String loadCsvAsJson(String filePath) {
        try {
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) return null;

            String header = null;
            JsonArray rows = new JsonArray();

            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) continue;
                    if (first) {
                        header = line.trim();
                        first = false;
                    } else {
                        rows.add(line.trim());
                    }
                }
            }

            if (header == null) return null;
            return "{\"header\":\"" + header + "\",\"rows\":" + rows.toString() + "}";

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}