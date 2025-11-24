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
 * Estructura: data/carpeta/signals_YYYY-MM-DD.csv
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
        // synchronized evita que dos hilos escriban en el mismo archivo a la vez
        // guarda las señales del bitalino en un archivo CSV por día y por paciente
        try {
            ensurePatientDir(folder); // asegura la carpeta

            // Nombre del fichero con fecha del día
            String today = LocalDate.now().format(DATE_FMT);
            Path file = patientDir(folder).resolve("signals_" + today + ".csv"); // nombre archivo

            boolean newFile = !Files.exists(file); // para escribir cabecera si es nuevo

            // Abrimos para APPEND (crear si no existe)
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                // si el archivo no existe, lo crea
                // si el archivo si que existe, añade texto al final --> APPEND --> sin borrar lo que ya hay escrito

                // Escribir cabecera la primera vez
                if (newFile) {
                    bw.write("timestamp,ecg,eda");
                    bw.newLine();
                }

                // Escribir cada fila
                for (var el : rows) { // var: java deduce por si solo el tipo de elemento que es
                    // var = JsonElement
                    String row = el.getAsString(); // JSON array
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
        // Lee el archivo CSV del día (las señales del BITalino) y lo convierte en un JSON con:
        try {
            String today = LocalDate.now().format(DATE_FMT);
            Path file = patientDir(folder).resolve("signals_" + today + ".csv");
            if (!Files.exists(file)) return null; // si el archivio no existe se devuelve null, el paciente no tiene señales guardadas hoy

            String header = null; // la primera línea del CSV
            JsonArray rows = new JsonArray(); // una lista con todas las filas de datos

            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { // lee linea por linea el archivo
                String line; boolean first = true;
                while ((line = br.readLine()) != null) { // lee hasta el final el archivo
                    if (line.isBlank()) continue; // si hay lineas en blanco sigue
                    if (first) { header = line.trim(); first = false; } // solo guarda el header, la primera linea
                    else { rows.add(line.trim()); } // el resto de lineas van al JSON array

                    // los headers permiten saber como se llama cada columna
                    //  las filas son muchas, y deben mantenerse en orden.
                    // Un JSON array:
                    //mantiene el orden original
                    //permite tantas líneas como quieras
                    //es la estructura natural para una lista
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
        // Lee un archivo CSV (cualquier CSV) y lo convierte en un JSON con la forma:
        //{
        //  "header": "timestamp,ecg,eda",
        //  "rows": ["fila1", "fila2", "fila3", ...]
        //}
        // Usar cuando
        // doctor quiere ver todos los CSV anteriores
        // servidor guarda diferentes días
        // visualizar otros archivos históricos
        try {
            Path file = Paths.get(filePath); // convierte en un path de java
            if (!Files.exists(file)) return null; // si no existe devuelve null

            String header = null; // primera linea del archivo
            JsonArray rows = new JsonArray(); // resto de lienas

            try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) { //lee hasta el final del archivo
                    if (line.isBlank()) continue; // salta lineas en blanco
                    if (first) {
                        header = line.trim(); // guarda la primera
                        first = false;
                    } else {
                        rows.add(line.trim()); // el resto de lineas al JSON array
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