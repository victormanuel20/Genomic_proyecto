// Paquete donde se encuentra la clase TCPThread
package org.vicnata.red;

// Importación de clases necesarias para auditoría, configuración y manejo de operaciones
import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.operations.CreateHandler;
import org.vicnata.operations.DeleteHandler;
import org.vicnata.operations.OperationHandler;
import org.vicnata.operations.RetrieveHandler;
import org.vicnata.operations.UpdateHandler;

// Importación de clases para comunicación por sockets
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Locale;

// Clase que extiende Thread para manejar una conexión TCP de forma concurrente
public class TCPThread extends Thread {

    // Socket que representa la conexión con el cliente
    private final Socket socket;

    // Constructor que recibe el socket y lo guarda
    public TCPThread(Socket socket) {
        this.socket = socket;
    }

    // Método que se ejecuta cuando el hilo inicia
    @Override
    public void run() {
        // Uso de try-with-resources para abrir y cerrar automáticamente los streams
        try (var in  = new DataInputStream(socket.getInputStream());   // Entrada de datos del cliente
             var out = new DataOutputStream(socket.getOutputStream())) // Salida de datos hacia el cliente
        {
            // Lectura del mensaje enviado por el cliente en formato UTF
            String wire = in.readUTF();
            System.out.println("\n=== RAW (Hilo " + getId() + ") ===\n" + wire); // Log del mensaje recibido

            // Separación del mensaje en partes usando el protocolo definido
            String[] parts = ProtocolManager.split(wire);

            // Extracción de la operación (ej. CREATE, RETRIEVE, etc.) en mayúsculas
            String op = parts.length > 0 ? parts[0].toUpperCase(Locale.ROOT) : "";

            // Carga de configuración desde archivo de propiedades
            GestorPropiedades cfg = new GestorPropiedades();

            // Inicialización del logger de auditoría con la ruta definida en propiedades
            LogAuditoria audit = new LogAuditoria(cfg.getProperty("PATH_LOG_AUDITORIA"));

            // Selección del handler correspondiente según la operación recibida
            OperationHandler handler = switch (op) {
                case "CREATE"   -> new CreateHandler(audit);   // Handler para operación CREATE
                case "RETRIEVE" -> new RetrieveHandler(audit); // Handler para operación RETRIEVE (comentado por ahora)
                case "UPDATE"   -> new UpdateHandler(audit);   // Handler para operación UPDATE (comentado por ahora)
                case "DELETE"   -> new DeleteHandler(audit);   // Handler para operación DELETE (comentado por ahora)
                default         -> null;                        // Si no se reconoce la operación, se asigna null
            };

            // Generación de respuesta: si no hay handler, se devuelve error; si lo hay, se ejecuta
            String resp = (handler == null)
                    ? ProtocolManager.error("UNKNOWN_OPERATION", op) // Mensaje de error si la operación no es válida
                    : handler.handle(parts);                         // Ejecución del handler si es válido

            // Envío de la respuesta al cliente
            out.writeUTF(resp);

        } catch (Exception e) {
            // Manejo de errores: impresión del mensaje y stack trace
            System.err.println("[THREAD " + getId() + "] " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cierre del socket para liberar recursos
            try {
                socket.close();
            } catch (Exception ignore) {
                // Ignora cualquier excepción al cerrar el socket
            }
        }
    }
}