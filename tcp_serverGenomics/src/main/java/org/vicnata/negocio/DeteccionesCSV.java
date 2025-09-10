// Paquete donde está la clase que gestiona el archivo CSV de detecciones
package org.vicnata.negocio;

// Utilidad para obtener la fecha actual en formato ISO Bogotá
import org.vicnata.utils.UtilFechas;

// Librerías para manejo de archivos y texto
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.*;
import java.nio.file.Files;


// Clase que gestiona la creación y escritura de un archivo CSV con detecciones
public class DeteccionesCSV {

    // Ruta del archivo CSV como objeto Path
    private final Path csv;

    // Constructor que recibe la ruta del archivo y llama a init()
    public DeteccionesCSV(String rutaCsv) {
        this.csv = Paths.get(rutaCsv);
        init(); // Prepara el archivo si no existe
    }



    // Método privado que inicializa el archivo CSV y corrige la cabecera si es necesario
    private void init() {
        try {
            // Crea los directorios si no existen
            if (csv.getParent() != null) Files.createDirectories(csv.getParent());

            if (Files.notExists(csv)) {
                // Si el archivo no existe, lo crea con la cabecera correcta
                try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                    w.write("patient_id,full_name,document_id,disease_id,severity,datetime,description\n");
                }
            } else {
                // Si el archivo existe, verifica y actualiza la cabecera si está mal
                List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    String actualHeader = lines.get(0).trim().toLowerCase();
                    String headerEsperado = "patient_id,full_name,document_id,disease_id,severity,datetime,description";
                    if (!actualHeader.equals(headerEsperado)) {
                        lines.set(0, headerEsperado); // Corrige la cabecera
                        Files.write(csv, lines, StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (IOException e) {
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

    // ================== NUEVO #1: append sobrecargado ==================
// Útil si desde algún flujo solo tienes patientId/disease/severity/description.
// Escribe en las columnas de full_name y document_id una cadena vacía (“”).
    public void append(String patientId, String diseaseId, int severity, String description) {
        append(patientId, "", "", diseaseId, severity, description);
    }

// ================== NUEVO #2: listar detecciones por paciente ==================
    /**
     * Devuelve una lista de etiquetas "disease_id(severity)" para el patientId dado.
     * Lee el CSV por nombres de columna, así resiste cambios de orden en el header.
     * Si el archivo está vacío, no existe o el paciente no tiene detecciones, devuelve lista vacía.
     */
    public java.util.List<String> listarEtiquetasPorPaciente(String patientId) {
        if (patientId == null || patientId.isBlank()) return java.util.List.of();
        if (!java.nio.file.Files.exists(csv)) return java.util.List.of();

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(csv, java.nio.charset.StandardCharsets.UTF_8);
            if (lines.size() <= 1) return java.util.List.of(); // solo cabecera o vacío

            // --- Mapeo de índices por nombre de columna (robusto ante cambios de orden) ---
            String[] header = lines.get(0).split(",", -1);
            java.util.Map<String, Integer> idx = new java.util.HashMap<>();
            for (int i = 0; i < header.length; i++) {
                idx.put(header[i].trim().toLowerCase(), i);
            }

            // Tomamos los índices que nos interesan (si no existen, -1)
            int iPid = idx.getOrDefault("patient_id", -1);
            int iDis = idx.getOrDefault("disease_id", -1);
            int iSev = idx.getOrDefault("severity", -1);

            java.util.List<String> out = new java.util.ArrayList<>();
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] c = line.split(",", -1);
                String pid = col(c, iPid);
                if (!patientId.equals(pid)) continue;

                String disease = col(c, iDis);
                String sev     = col(c, iSev);
                if (!disease.isEmpty() && !sev.isEmpty()) {
                    out.add(disease + "(" + sev + ")");
                }
            }
            return out;

        } catch (java.io.IOException e) {
            throw new RuntimeException("No se pudo leer detecciones.csv", e);
        }
    }

    // ... otros métodos como append, listarEtiquetasPorPaciente, etc.

    // === LECTURA: obtener enfermedades existentes de un patient_id ===
    public static class DiseaseTag {
        public final String diseaseId;
        public final int severity;
        public DiseaseTag(String diseaseId, int severity) {
            this.diseaseId = diseaseId; this.severity = severity;
        }
    }

    /** Devuelve (enfermedad, severidad) ya registradas para el patient_id. */
    public List<DiseaseTag> enfermedadesDe(String patientId) {
        try {
            if (Files.notExists(csv)) return List.of();

            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return List.of();

            // Cabecera esperada: patient_id,full_name,document_id,disease_id,severity,datetime,description
            Map<String, Integer> maxSevPorEnf = new LinkedHashMap<>();

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 7) continue;

                String pid = cols[0];
                String diseaseId = cols[3];
                int sev;
                try { sev = Integer.parseInt(cols[4]); } catch (Exception e) { sev = 0; }

                if (patientId.equals(pid)) {
                    // Si hay varias filas de la misma enfermedad, nos quedamos con la mayor severidad
                    maxSevPorEnf.merge(diseaseId, sev, Math::max);
                }
            }

            return maxSevPorEnf.entrySet().stream()
                    .map(e -> new DiseaseTag(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer detecciones de " + patientId, e);
        }
    }

    /**
     * Actualiza full_name y/o document_id en TODAS las filas que tengan el patientId dado.
     * Si alguno de los nuevos valores es null o vacío, se deja el valor anterior.
     * Mantiene el resto de columnas (disease_id, severity, datetime, description) tal como están.
     */
    public void actualizarMetadataPaciente(String patientId, String nuevoFullName, String nuevoDocumentId) {
        try {
            if (Files.notExists(csv)) return; // no hay nada que actualizar

            List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;

            // Cabecera esperada:
            // patient_id,full_name,document_id,disease_id,severity,datetime,description
            List<String> out = new ArrayList<>(lines.size());
            String headerEsperado = "patient_id,full_name,document_id,disease_id,severity,datetime,description";
            String header = lines.get(0).trim();
            if (!header.equalsIgnoreCase(headerEsperado)) {
                // normalizamos cabecera por si quedó mal en algún momento
                out.add(headerEsperado);
            } else {
                out.add(lines.get(0));
            }

            // Reescribimos filas
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.trim().isEmpty()) { out.add(line); continue; }

                String[] cols = line.split(",", -1); // simple split (tus datos no usan comas internas)
                if (cols.length < 7) { out.add(line); continue; }

                String pid = cols[0];
                if (!patientId.equals(pid)) {
                    out.add(line); // fila de otro paciente
                    continue;
                }

                // cols[1] = full_name ; cols[2] = document_id
                if (nuevoFullName != null && !nuevoFullName.isBlank()) {
                    cols[1] = csv(nuevoFullName);
                }
                if (nuevoDocumentId != null && !nuevoDocumentId.isBlank()) {
                    cols[2] = csv(nuevoDocumentId);
                }

                // reconstruir la línea (respetando columnas)
                String nueva = String.join(",",
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5], cols[6]
                );
                out.add(nueva);
            }

            Files.write(csv, out, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo actualizar metadata en detecciones.csv para " + patientId, e);
        }
    }



// ================== NUEVO #3: helper de lectura seguro por índice ==================
    /** Extrae columna segura (devuelve "" si el índice no existe o está fuera de rango). */
    private static String col(String[] arr, int i) {
        if (i < 0 || i >= arr.length) return "";
        return arr[i] == null ? "" : arr[i].trim();
    }

}