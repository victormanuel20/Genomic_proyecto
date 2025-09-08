package org.vicnata.red;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class TCPServer {
    private final int port;

    public TCPServer(int port) { this.port = port; }

    public void start() {
        try {
            SSLServerSocketFactory sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            try (SSLServerSocket server = (SSLServerSocket) sf.createServerSocket(port)) {
                System.out.println("[SERVER] SSL escuchando en puerto " + port);
                while (true) {
                    SSLSocket client = (SSLSocket) server.accept();
                    new TCPThread(client).start(); // 🔹 un hilo por cliente
                }
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
