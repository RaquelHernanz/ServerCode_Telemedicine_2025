package utilities;


import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Utilities {

    // ========= Entrada por consola =========
    private static final Scanner scanner = new Scanner(System.in);

    public static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    public static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Error: Debes ingresar un número entero.");
            }
        }
    }

    // ========= Fecha/Hora ISO =========
    // ISO estándar internacional para representar fechas y horas.
    // DT: LOCAL DATE TIME
    // T: LOCAL TIME
    // D: LOCAL DATE
    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_D  = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_T  = DateTimeFormatter.ISO_LOCAL_TIME;

    public static String formatDateTime(LocalDateTime dt) { return dt != null ? ISO_DT.format(dt) : null; }
    public static String formatDate(LocalDate d)         { return d  != null ? ISO_D.format(d)  : null; }
    public static String formatTime(LocalTime t)         { return t  != null ? ISO_T.format(t)  : null; }
    // Si dt es null, devuelve null.
    //Si no, convierte el LocalDateTime en un string ISO.

    public static LocalDateTime parseDateTime(String s) {
        // leer una fecha
        if (s == null || s.isEmpty()) return null;
        try { return LocalDateTime.parse(s, ISO_DT); }
        catch (DateTimeParseException e) { return null; }
    }
}

