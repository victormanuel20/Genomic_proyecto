// Paquete donde está la clase que gestiona el archivo CSV de detecciones
package org.vicnata.negocio;

// Utilidad para obtener la fecha actual en formato ISO Bogotá
import org.vicnata.utils.UtilFechas;

// Librerías para manejo de archivos y texto
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

// Clase que gestiona la creación y escritura de un archivo CSV con detecciones
public class DeteccionesCSV {

    // Ruta del archivo CSV como objeto Path
    private final Path csv;

    // Constructor que recibe la ruta del archivo y llama a init()
    public DeteccionesCSV(String rutaCsv) {
        this.csv = Paths.get(rutaCsv);
        init(); // Prepara el archivo si no existe
    }

    // Método privado que inicializa el archivo CSV
    private void init() {
        try {
            // Crea los directorios si no existen
            if (csv.getParent() != null) Files.createDirectories(csv.getParent());

            // Si el archivo no existe, lo crea y escribe la cabecera
            if (Files.notExists(csv)) {
                try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                    // Cabecera con los campos que se van a registrar
                    w.write("patient_id,full_name,document_id,disease_id,severity,datetime,description\n");
                }
            }
        } catch (IOException e) {
            // Si hay error, lanza una excepción con mensaje claro
            throw new RuntimeException("No se pudo preparar detecciones.csv", e);
        }
    }

    // Método público para agregar una nueva línea de detección al CSV
    public void append(String patientId, String fullName, String documentId,
                       String diseaseId, int severity, String description) {

        // Construye la línea CSV con los datos, usando formato seguro
        String line = String.join(",",
                csv(patientId),
                csv(fullName),
                csv(documentId),
                csv(diseaseId),
                String.valueOf(severity),
                csv(UtilFechas.ahoraIsoBogota()), // Fecha actual en formato ISO Bogotá
                csv(description)
        );

        // Escribe la línea en el archivo
        appendLine(line);
    }

    // Método privado que escribe una línea en el archivo CSV
    private void appendLine(String line) {
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);     // Escribe la línea
            w.newLine();       // Salto de línea
        } catch (IOException e) {
            // Si hay error al escribir, lanza excepción
            throw new RuntimeException("No se pudo escribir en detecciones.csv", e);
        }
    }

    // Método auxiliar para formatear valores CSV de forma segura
    private static String csv(String v) {
        if (v == null) return ""; // Si es nulo, devuelve vacío

        // Escapa comillas dobles
        String s = v.replace("\"", "\"\"");

        // Si contiene coma, comillas o salto de línea, lo encierra entre comillas
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s + "\"";

        return s; // Si no, devuelve el valor tal cual
    }
}