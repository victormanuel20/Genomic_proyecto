package org.vicnata.red;

import org.vicnata.operations.CreateHandler;
import org.vicnata.operations.DeleteHandler;
import org.vicnata.operations.OperationHandler;
import org.vicnata.operations.RetrieveHandler;
import org.vicnata.operations.UpdateHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Locale;

public class TCPThread extends Thread {
    private final Socket socket;

    public TCPThread(Socket socket) { this.socket = socket; }

    @Override public void run() {
        try (var in  = new DataInputStream(socket.getInputStream());
             var out = new DataOutputStream(socket.getOutputStream())) {

            String wire = in.readUTF();
            System.out.println("\n=== RAW (Hilo " + getId() + ") ===\n" + wire);

            String[] parts = ProtocolManager.split(wire);
            String op = parts.length > 0 ? parts[0].toUpperCase(Locale.ROOT) : "";

            OperationHandler handler = switch (op) {
                case "CREATE"   -> new CreateHandler();
                case "RETRIEVE" -> new RetrieveHandler();
                case "UPDATE"   -> new UpdateHandler();
                case "DELETE"   -> new DeleteHandler();
                default         -> null;
            };

            String resp = (handler == null)
                    ? ProtocolManager.error("UNKNOWN_OPERATION", op)
                    : handler.handle(parts);

            out.writeUTF(resp);

        } catch (Exception e) {
            System.err.println("[THREAD " + getId() + "] " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (Exception ignore) {}
        }
    }
}
