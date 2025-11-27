package server;

import server.database.DatabaseManager;
import utilities.Utilities;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Arranca el servidor y acepta conexiones en el puerto 9000.
 * Cada nuevo cliente se maneja por separado con ClientHandler.
 */

public class ServerMain {

    public static final int PORT = 9000; // puerto del servidor y al que se tienen que conectar el patient y el doctor

    // Contraseña de administrador fija para el requisito obligatorio
    private static final String ADMIN_PASSWORD = "admin";

    // Variables estáticas para control de la conexión y los hilos
    private static ServerSocket serverSocket;
    private static ExecutorService clientPool;

    // Bandera atómica para controlar el ciclo de vida de los bucles (seguro en entorno multihilo)
    private static final AtomicBoolean isRunning = new AtomicBoolean(true);

        // crea un "pool" de hilos para manejar varios clientes a la vez
        // permite manejar varios clientes A LA VEZ
        // si un cliente se desconecta, el hijo se puede reusar
        // el pool es como una bolsa con hilos y cuando se conecta un cliente se van sacando hilos del pool
        // se pueden manejar varios a la vez y se reutilizan
        // el server no se cierra a menos que haya un error o se pare manualmente,
        // está siempre activo escuchando a ver si se conectan nuevos clientes

   public static void main(String[] args) {
       // 1. Inicialización de Recursos
       DatabaseManager.connect(); // Abre la única conexión con la DB (telemedicina.db).

       // Asigna la referencia estática al pool de hilos.
       clientPool = Executors.newCachedThreadPool();

       // 2. Hilo de Consola (Interfaz de Administración)
       Thread adminConsoleThread = new Thread(ServerMain::adminConsoleLoop); // Crea el hilo para leer comandos.
       adminConsoleThread.setDaemon(true); // Permite que el programa termine aunque este hilo siga esperando entrada.
       adminConsoleThread.start(); // Inicia el hilo de la consola.

       try {
           // Asigna la referencia estática al ServerSocket
           serverSocket = new ServerSocket(PORT);
           System.out.println("[Server] Listening on port " + PORT);

           // 3. Bucle Principal de Escucha
           while (isRunning.get()) { // Bucle controlado por la bandera (se detiene cuando isRunning es 'false')
               Socket clientSocket = serverSocket.accept(); // Bloquea la ejecución hasta que llega un cliente.
               System.out.println("[Server] New client connected");

               // Asigna la gestión del nuevo cliente a un hilo libre del pool.
               clientPool.execute( new ClientHandler(clientSocket));
           }

       } catch (IOException e) {
           // Esto captura errores de red Y la excepción que lanza serverSocket.close() al apagar.
           if (isRunning.get()) {
               // Solo imprime el error si NO es un cierre intencional del administrador.
               e.printStackTrace();
           }
       } finally {
           // Se ejecuta siempre, asegurando el cierre limpio.
           shutdownServer(); // Llama al nuevo métdo para detener hilos y cerrar la DB.
       }
   }
    // Lógica del hilo de consola (Interfaz de Administración)
    private static void adminConsoleLoop() {
        System.out.println("-------------------------------------");
        System.out.println("Admin Console: Type 'shutdown' to stop.");
        System.out.println("-------------------------------------");

        while (isRunning.get()) {
            // Usa la utilidad para leer la entrada de la consola
            String command = Utilities.readString("Server Admin > ").trim();
            if (command.equalsIgnoreCase("shutdown")) {
                if (attemptShutdown()) {
                    break;
                }
            }
        }
    }

    // Lógica de apagado seguro con comprobación de contraseña
    private static boolean attemptShutdown() {
        String pwd = Utilities.readString("Enter admin password to confirm shutdown: ");
        if (!pwd.equals(ADMIN_PASSWORD)) {
            System.out.println("[Admin] Invalid password. Shutdown aborted.");
            return false;
        }

        System.out.println("[Admin] Shutting down server...");
        isRunning.set(false); // Detiene el bucle principal

        // Cerrar el ServerSocket para desbloquear serverSocket.accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.exit(0);
            } catch (IOException e) {
                System.err.println("[Admin] Error closing ServerSocket: " + e.getMessage());
            }
        }
        return true;
    }

    // Cierre final que limpia recursos (ejecutado en el bloque finally)
    private static void shutdownServer() {
        System.out.println("[Server] Shutting down thread pool and DB connection...");

        clientPool.shutdownNow(); // Detiene los hilos de clientes

        DatabaseManager.close(); // Cierra la conexión de la DB de forma segura

        System.out.println("-------------------------------------");
        System.out.println("[Server] Server stopped successfully.");
    }

}