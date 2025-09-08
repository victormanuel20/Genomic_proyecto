package org.vicnata.negocio;

import org.vicnata.utils.UtilFechas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class DeteccionesCSV {
    private final Path csv;

    public DeteccionesCSV(String rutaCsv) {
        this.csv = Paths.get(rutaCsv);
        init();
    }

    private void init() {
        try {
            if (csv.getParent() != null) Files.createDirectories(csv.getParent());
            if (Files.notExists(csv)) {
                try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8)) {
                    // NUEVOS CAMPOS: full_name y document_id
                    w.write("patient_id,full_name,document_id,disease_id,severity,datetime,description\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo preparar detecciones.csv", e);
        }
    }

    public void append(String patientId, String fullName, String documentId,
                       String diseaseId, int severity, String description) {
        String line = String.join(",",
                csv(patientId),
                csv(fullName),
                csv(documentId),
                csv(diseaseId),
                String.valueOf(severity),
                csv(UtilFechas.ahoraIsoBogota()),
                csv(description)
        );
        appendLine(line);
    }

    private void appendLine(String line) {
        try (BufferedWriter w = Files.newBufferedWriter(csv, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir en detecciones.csv", e);
        }
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"","\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s + "\"";
        return s;
    }
}