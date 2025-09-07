package org.vicnata.MenuCliente;

import org.vicnata.Red.TCPcliente;
import org.vicnata.enums.Operacion;
import org.vicnata.enums.sexo;
import org.vicnata.helpers.ManejadorFasta;
import org.vicnata.helpers.ProtocolManager;
import org.vicnata.modelosDTO.ArchivoFastaDTO;
import org.vicnata.modelosDTO.Mensaje;
import org.vicnata.modelosDTO.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;


import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MenuCliente {

    private TCPcliente tpcCliente;
    private String SERVER_IP;
    private int SERVER_PORT;

    public MenuCliente() {
        GestorPropiedades gestor = new GestorPropiedades();

        // Obtener las propiedades desde el archivo
        SERVER_IP = gestor.getProperty("SERVER_HOST");
        SERVER_PORT = Integer.parseInt(gestor.getProperty("SERVER_PORT"));
        // Mostrar en consola para verificar
        System.out.println("IP del servidor cargada: " + SERVER_IP);
        System.out.println("Puerto del servidor cargado: " + SERVER_PORT);


        // Crear el cliente TCP con los valores obtenidos
        this.tpcCliente = new TCPcliente(SERVER_IP, SERVER_PORT);


    }

    private final Scanner in = new Scanner(System.in);
    private static final String HASH_DEF = "SHA-256"; // algoritmo por defecto

    public void mostrar() {
        int opcion;
        do {
            System.out.println("\n=== MENÚ CLIENTE ===");
            System.out.println("1. Crear persona");
            System.out.println("2. Retrieve persona");
            System.out.println("3. Update persona");
            System.out.println("4. Delete persona");
            System.out.println("5. Salir");
            System.out.print("Elige una opción: ");

            opcion = leerEnteroSeguro();

            switch (opcion) {
                case 1 -> {
                    System.out.println("\n[CREATE] Ingresa los datos:");
                    PacienteDTO paciente = readPacienteInfo();
                    System.out.println("Paciente creado:");
                    //System.out.println(paciente); // luego lo pasas al ProtocolManager

                    // Armamos el payload
                    Map<String, String> payload = new HashMap<>();
                    payload.put("NOMBRE", paciente.getNombre());
                    payload.put("APELLIDO", paciente.getApellido());
                    payload.put("ID", paciente.getId());
                    payload.put("EDAD", String.valueOf(paciente.getEdad()));
                    payload.put("SEXO", paciente.getSexo() != null ? paciente.getSexo().name() : "");
                    payload.put("CONTACT_EMAIL", paciente.getContactEmail());
                    payload.put("CLINICAL_NOTES", paciente.getClinicalNotes());

                    // Obtenemos la ruta fasta (del DTO que ya leímos antes)
                    String primeralinea = paciente.getGenoma().getNombreArchivo();
                    System.out.println(primeralinea);
                    String ruta = paciente.getGenoma().getRuta();
                    System.out.println(ruta);
                    ProtocolManager pm = new ProtocolManager(); // o new ProtocolManager("min","SHA-256")
                    Mensaje msg = pm.buildMessage(Operacion.CREATE, payload,ruta);
                    System.out.println("Lo que se enviara al servidor");
                    System.out.println(msg.getPayload());
                    tpcCliente.sendMessage(msg.getPayload());


                }
                case 2 -> System.out.println("[RETRIEVE] (pendiente)");
                case 3 -> System.out.println("[UPDATE] (pendiente)");
                case 4 -> System.out.println("[DELETE] (pendiente)");
                case 5 -> System.out.println("Saliendo...");
                default -> System.out.println("Opción inválida.");
            }
        } while (opcion != 5);
    }

    /** Pide datos al usuario y construye un PacienteDTO con su ArchivoFastaDTO */
    private PacienteDTO readPacienteInfo() {
        PacienteDTO p = new PacienteDTO();

        System.out.print("Nombre: ");
        p.setNombre(leerNoVacio());

        System.out.print("Apellido: ");
        p.setApellido(leerNoVacio());

        System.out.print("Documento/ID: ");
        p.setId(leerNoVacio());

        System.out.print("Edad: ");
        p.setEdad(leerEnteroSeguro());

        System.out.print("Sexo (M/F): ");
        String s = leerNoVacio().toUpperCase();
        try {
            p.setSexo(sexo.valueOf(s));
        } catch (Exception e) {
            System.out.println("Sexo inválido, se deja vacío.");
            p.setSexo(null);
        }

        System.out.print("Email de contacto: ");
        p.setContactEmail(leerNoVacio());

        System.out.print("Notas clínicas: ");
        p.setClinicalNotes(leerLinea());

        System.out.print("Ruta archivo FASTA: ");
        String rutaFasta = leerNoVacio();

        // Usamos tu manejador de FASTA para crear el DTO con checksum y base64
        ArchivoFastaDTO fasta = ManejadorFasta.leerFasta(rutaFasta, HASH_DEF);
        fasta.setRuta(rutaFasta); // ← aquí guardas la ruta completa
        p.setGenoma(fasta);

        return p;
    }

    // ========= Helpers de entrada =========

    private String leerLinea() {
        return in.nextLine().trim();
    }

    private String leerNoVacio() {
        while (true) {
            String s = leerLinea();
            if (!s.isBlank()) return s;
            System.out.print("No puede estar vacío, intenta de nuevo: ");
        }
    }

    private int leerEnteroSeguro() {
        while (true) {
            try {
                return Integer.parseInt(leerLinea());
            } catch (NumberFormatException e) {
                System.out.print("Número inválido, intenta de nuevo: ");
            }
        }
    }
}
