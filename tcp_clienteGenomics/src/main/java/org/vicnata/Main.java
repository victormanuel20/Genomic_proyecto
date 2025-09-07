package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.helpers.ManejadorFasta;
import org.vicnata.modelosDTO.ArchivoFastaDTO;

import java.util.Scanner;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

// TODO: cargar ConfigSSL, iniciar MenuCliente
public class Main {
    public static void main(String[] args) {

    //Prueba numero 1 se configura el gestor de propiedades
    //ConfigSSL.configurar("src/main/java/org/vicnata/configuration.properties");


     //Prueba numero 2
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


    }
}