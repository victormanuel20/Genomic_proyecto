package org.vicnata.red;




import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TCPServer {
    private final int serverPort;

    public TCPServer(int serverPort){
        this.serverPort = serverPort;
    }

    public void start(){
        try {
            SSLServerSocketFactory sf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            try (SSLServerSocket serverSocket = (SSLServerSocket) sf.createServerSocket(serverPort)) {
                System.out.println("Server started on port: " + serverPort);

                while (true) {
                    try (SSLSocket client = (SSLSocket) serverSocket.accept();
                         DataInputStream in = new DataInputStream(client.getInputStream());
                         DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                        String message = in.readUTF();
                        System.out.println("\n=== RAW ===");
                        System.out.println(message);

                        String[] parts = message.split("\\\\"); // <- separador '\'
                        if (parts.length == 0) {
                            out.writeUTF("ERROR|EMPTY_MESSAGE");
                            continue;
                        }

                        String op = parts[0].trim().toUpperCase();
                        switch (op) {
                            case "CREATE" -> {
                                String resp = handleCreate(parts);
                                out.writeUTF(resp);
                            }
                            case "RETRIEVE" -> {
                                // TODO: parsear RETRIEVE\patientId
                                out.writeUTF("OK|RETRIEVE|PENDING");
                            }
                            case "UPDATE" -> {
                                // TODO
                                out.writeUTF("OK|UPDATE|PENDING");
                            }
                            case "DELETE" -> {
                                // TODO
                                out.writeUTF("OK|DELETE|PENDING");
                            }
                            default -> out.writeUTF("ERROR|UNKNOWN_OPERATION|" + op);
                        }
                    } catch (IOException e) {
                        System.out.println("Client error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    // parts esperado para CREATE (índices):
    // 0: CREATE
    // 1: NOMBRE
    // 2: APELLIDO
    // 3: ID
    // 4: EDAD
    // 5: SEXO
    // 6: CONTACT_EMAIL
    // 7: CLINICAL_NOTES
    // 8: FASTA_HEADER_ID   (ej. "patient001")
    // 9: SECUENCIA         (ej. "ACGT...")
    // 10: (opcional) FILE_SIZE_BYTES
    // 11: (opcional) HASH_ALGO
    // 12: (opcional) CHECKSUM_HEX
    // 13: (opcional) BASE64 (full)
    private String handleCreate(String[] parts) {
        if (parts.length < 10) {
            return "ERROR|CREATE|MISSING_FIELDS|min_expected>=10 got=" + parts.length;
        }

        String nombre   = parts[1];
        String apellido = parts[2];
        String docId    = parts[3];
        String edad     = parts[4];
        String sexo     = parts[5];
        String email    = parts[6];
        String notas    = parts[7];

        String fastaId  = parts[8];
        String secuencia= parts[9];

        // Opcionales según modo
        Long   size     = null;
        String hashAlgo = null;
        String checksum = null;
        String base64   = null;

        if (parts.length >= 12) {
            // min: size, hash, checksum
            try { size = Long.parseLong(parts[10]); } catch (Exception ignored) {}
            hashAlgo = parts[11];
        }
        if (parts.length >= 13) {
            checksum = parts[12];
        }
        if (parts.length >= 14) {
            base64 = parts[13];
        }

        // Logueo bonito para ver que llegó:
        System.out.println("=== PARSED CREATE ===");
        System.out.println("Paciente: " + nombre + " " + apellido + " (" + docId + ")");
        System.out.println("Edad: " + edad + "  Sexo: " + sexo + "  Email: " + email);
        System.out.println("Notas: " + notas);
        System.out.println("FASTA  ID: " + fastaId);
        System.out.println("Secuencia (len): " + (secuencia != null ? secuencia.length() : 0));
        if (size != null)     System.out.println("File size: " + size);
        if (hashAlgo != null) System.out.println("Hash algo: " + hashAlgo);
        if (checksum != null) System.out.println("Checksum : " + checksum);
        if (base64 != null)   System.out.println("Base64 len: " + base64.length());

        // Aquí podrías:
        // - Validar checksum si recibiste base64 (recomiendo dejarlo para tu server "real")
        // - Guardar en log/Excel
        // - Responder OK con algún ID generado
        return "OK|CREATE|RECEIVED|" + fastaId;
    }
}

