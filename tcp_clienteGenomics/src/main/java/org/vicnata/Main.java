package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.enums.Operacion;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.helpers.ManejadorFasta;
import org.vicnata.helpers.ProtocolManager;
import org.vicnata.modelosDTO.ArchivoFastaDTO;
import org.vicnata.modelosDTO.Mensaje;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.vicnata.MenuCliente.MenuCliente;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

// TODO: cargar ConfigSSL, iniciar MenuCliente
public class Main {
    public static void main(String[] args) {

    //Prueba numero 1 se configura el gestor de propiedades
    //ConfigSSL.configurar("src/main/java/org/vicnata/configuration.properties");
      //ConfigSSL.configurar();



     //Prueba numero 2. Manejador fasta con ela rchivo fsta prueba del SHA-256. leer
      /*
        Scanner sc = new Scanner(System.in);
        System.out.print("Ruta del archivo FASTA: ");
        String ruta = sc.nextLine().trim();

        // Lee el fasta (sin idPaciente por ahora) usando SHA-256
        ArchivoFastaDTO fasta = ManejadorFasta.leerFasta(ruta, "SHA-256");

        System.out.println("\n--- FASTA leído correctamente ---");
        System.out.println("Nombre archivo : " + fasta.getNombreArchivo());
        System.out.println("Tamaño (bytes) : " + fasta.getTamanoBytes());
        System.out.println("Algoritmo hash : " + fasta.getAlgoritmoHash());
        System.out.println("Checksum (hex) : " + fasta.getChecksum());
        System.out.println("Base64 (primeros 60): " +
                fasta.getContenidoBase64().substring(0, Math.min(60, fasta.getContenidoBase64().length())) + "...");

       */

        /*
        // ruta  src/main/java/org/vicnata/archivo.fasta
        //PRUEBA LEER Y ESCRIBIR
        Scanner sc = new Scanner(System.in);

        System.out.print("Ruta del FASTA a leer: ");
        String ruta = sc.nextLine().trim();

        // 1) Leer
        ArchivoFastaDTO dto = ManejadorFasta.leerFasta(ruta, "SHA-256");

        System.out.println("\n--- FASTA leído ---");
        System.out.println("Nombre archivo : " + dto.getNombreArchivo());
        System.out.println("Tamaño (bytes) : " + dto.getTamanoBytes());
        System.out.println("Algoritmo hash : " + dto.getAlgoritmoHash());
        System.out.println("Checksum (hex) : " + dto.getChecksum());
        System.out.println("Base64 (primeros 60): " +
                dto.getContenidoBase64().substring(0, Math.min(60, dto.getContenidoBase64().length())) + "...");

        // 2) Escribir “exacto” (mismos bytes)
        String outExacto = "salidas/exacto_" + dto.getNombreArchivo();
        ManejadorFasta.escribirFasta(dto, outExacto, false);

        // 3) Escribir “legible” (header + wrap)
        dto.setIdPaciente("patient_temp_demo"); // aparecerá como cabecera
        String outLegible = "salidas/legible_" + dto.getNombreArchivo();
        ManejadorFasta.escribirFasta(dto, outLegible, true);

        System.out.println("\nArchivos escritos:");
        System.out.println(" - " + outExacto + "  (exacto, para integridad)");
        System.out.println(" - " + outLegible + " (legible con cabecera)");

        */


        /*
        //PRUEBA YA CON RUTAA Y QUE DEVUELVE
        // Simulación de datos de un paciente
        Map<String,String> datos = new LinkedHashMap<>();
        datos.put("NOMBRE","Ana");
        datos.put("APELLIDO","Ruiz");
        datos.put("ID","CC-9988");
        datos.put("EDAD","29");
        datos.put("SEXO","F");
        datos.put("CONTACT_EMAIL","ana@mail.com");
        datos.put("CLINICAL_NOTES","Control prenatal");

        // Ruta al archivo FASTA (ajusta según tu caso)
        // 🔹 opción A (ruta absoluta en tu PC):
         String rutaFasta = "C:/Users/User/Desktop/paciente1.fasta";

        // 🔹 opción B (archivo dentro del proyecto):
        //String rutaFasta = "data/fasta/paciente1.fasta";

        // Construcción del mensaje usando nuestro ProtocolManager
        ProtocolManager pm = new ProtocolManager();
        Mensaje msg = pm.buildMessage(Operacion.CREATE, datos, rutaFasta);

        // Imprime el string final que se enviaría al servidor
        System.out.println("Mensaje generado:");
        System.out.println(msg.getPayload());

        */

        //ConfigSSL.configurar();
        MenuCliente menuCliente = new MenuCliente();
        menuCliente.mostrar();


    }
}