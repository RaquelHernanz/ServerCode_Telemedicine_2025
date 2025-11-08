package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Arranca el servidor y acepta conexiones en el puerto 9000.
 * Cada nuevo cliente se maneja por separado con ClientHandler.
 */

public class ServerMain {

    public static final int PORT = 9000; // puerto del servidor y al que se tienen que conectar el patient y el doctor

    public static void main(String[] args) {
        ExecutorService clientPool = Executors.newCachedThreadPool(); // crea un "pool" de hilos para manejar varios clientes a la vez
        // permite manejar varios clientes A LA VEZ
        // si un cliente se desconecta, el hijo se puede reusar
        // el pool es como una bolsa con hilos y cuando se conecta un cliente se van sacando hilos del pool
        // se pueden manejar varios a la vez y se reutilizan

        try (ServerSocket serverSocket = new ServerSocket(PORT)) { // se abre el socket, escucha
            System.out.println("[Server] Listening on port " + PORT);

            while (true) {
                // Espera a que un cliente se conecte
                Socket clientSocket = serverSocket.accept(); // cuando un doctor o paciente abre el socket, se conectan
                System.out.println("[Server] New client connected");

                // Crea un nuevo hilo para manejar a ese cliente
                clientPool.execute( new ClientHandler(clientSocket));
            }

        } catch (IOException e) { // si ocurre error al abrir el puerto o crear el socket
            e.printStackTrace(); // imprime la pila de errores
        } finally {
            // el server no se cierra a menos que haya un error o se pare manualmente,
            // está siempre activo escuchando a ver si se conectan nuevos clientes
            clientPool.shutdown(); // limpia la bolsa, el pool. se terminan los hilos
            System.out.println("[Server] Server stopped."); // se para el server
        }
    }
}