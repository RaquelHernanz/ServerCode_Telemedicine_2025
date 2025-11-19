package server;

public enum MessageType {
    REQUEST, RESPONSE,EVENT
}
//mensajes que el servidor envía al cliente por iniciativa propia, sin haber recibido una REQUEST explícita justo antes.
