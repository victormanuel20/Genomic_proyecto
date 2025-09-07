package org.vicnata.Red;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TCPcliente {

    private String serverAddress;
    private int serverPort;

    private SSLSocket clientSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    public TCPcliente(String serverAddress, int serverPort){
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }


    public void connect() throws IOException {
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.clientSocket = (SSLSocket) sslSocketFactory.createSocket(serverAddress, serverPort);
        System.out.println("Connected to server: " + this.serverAddress + ":" + this.serverPort);

        this.dataInputStream  = new DataInputStream(this.clientSocket.getInputStream());
        this.dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
    }

    /** Envía un ÚNICO mensaje (payload completo del protocolo) y espera respuesta. */
    public String sendMessage(String payload) {
        String response = "ERROR";
        try {
            this.connect();
            System.out.println("Sending message (" + payload.length() + " bytes UTF):");
            System.out.println(payload);

            // writeUTF → el servidor debe leer con readUTF
            this.dataOutputStream.writeUTF(payload);
            this.dataOutputStream.flush();

            // espera respuesta del servidor (ej. "OK|CREATE_RECEIVED")
            response = this.dataInputStream.readUTF();
            System.out.println("Received response: " + response);
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        } finally {
            this.closeConnection();
        }
        return response;
    }

    public void closeConnection() {
        try {
            if (this.dataInputStream  != null) this.dataInputStream.close();
            if (this.dataOutputStream != null) this.dataOutputStream.close();
            if (this.clientSocket    != null) this.clientSocket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }


}
