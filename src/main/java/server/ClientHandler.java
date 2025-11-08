package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable { // cada cliente conectado tiene un propio hilo, su propio ClientHandler

    private final Socket socket;

    // constructor que recibe el socket del cliente
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() { // ejecuta los hilos
        System.out.println("[Server] Handling client: " + socket.getRemoteSocketAddress());

        // try with resources
        // cuando acaba el try se cierran directamente el bufferReader y el printWriter
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true))
        {
            String message; // variable para almacenar cada mensaje

            // lee cada mensaje del cliente
            while ((message = bufferedReader.readLine()) != null) { // si hay algo escrito
                System.out.println("[Server] Received: " + message); // imprime lo que recibe el server

                // Procesa el mensaje con la clase Protocol
                String response = Protocol.process(message);

                // Envía la respuesta de vuelta al cliente
                if (response != null) {
                    printWriter.println(response); // se lo envia al cliente porque printWriter es el canal de salida del socket
                    System.out.println("[Server] Replied : " + response);
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] Client disconnected: " + socket.getRemoteSocketAddress());
        } finally {
            try {
                socket.close(); // cierra el socket del cliente
            } catch (IOException ignored) {} // si hay algún error al cerrar lo ignora porque ya estamos saliendo del socket
        }
    }
}