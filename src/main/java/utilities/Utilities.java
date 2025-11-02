package utilities;

import java.util.Scanner;
import java.util.regex.Pattern;


public class Utilities {

    private static final Scanner scanner = new Scanner(System.in);

    public static String readString(String string) {
        System.out.print(string);
        return scanner.nextLine();
    }

    public static int readInt(String integer) {
        int value;
        while (true) {
            System.out.print(integer);
            String input = scanner.nextLine();
            try {
                value = Integer.parseInt(input);
                break; // entrada válida
            } catch (NumberFormatException e) {
                System.out.println("Error: Debes ingresar un número entero.");
            }
        }
        return value;
    }

    public static float readFloat(String floatIntroduced) {
        float value;
        while (true) {
            System.out.print(floatIntroduced);
            String input = scanner.nextLine().trim();
            try {
                value = Float.parseFloat(input);
                break; // entrada válida
            } catch (NumberFormatException e) {
                System.out.println("Error: Debes ingresar un número decimal válido.");
            }
        }
        return value;
    }

    public static boolean readEmail(String emailIntroduced) {
        String emailPattern = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$";
        while (true) {
            System.out.print(emailIntroduced);
            String email = scanner.nextLine().trim();
            if (Pattern.matches(emailPattern, email)) {
                return true;
            } else {
                System.out.println("Error: Ingresa un email válido (ej: usuario@dominio.com).");
            }
        }
    }

    public static int readId(String idIntroduced) {
        int id;
        while (true) {
            id = readInt(idIntroduced);
            if (id > 0) {
                return id;
            } else {
                System.out.println("Error: El ID debe ser un número positivo.");
            }
        }
    }

    public static boolean validateDateOfBirth(String dateOfBirthIntroduced) {
        // Regex para validar formato yyyy-MM-dd
        String datePattern = "\\d{4}-\\d{2}-\\d{2}";

        while (true) {
            if (Pattern.matches(datePattern, dateOfBirthIntroduced)) {
                return true; // formato válido
            } else {
                System.out.println("Invalid date format. Please enter the date in yyyy-MM-dd format.");
            }
        }
    }

}
