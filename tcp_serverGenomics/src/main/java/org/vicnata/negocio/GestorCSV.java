package org.vicnata.negocio;

import org.vicnata.dto.PacienteDTO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;

public class GestorCSV {
    private final Path pacientesCsv;

    public GestorCSV(String rutaPacientesCsv) {
        this.pacientesCsv = Paths.get(rutaPacientesCsv);
        init();
    }

    private void init() {
        try {
            if (pacientesCsv.getParent() != null) Files.createDirectories(pacientesCsv.getParent());
            if (Files.notExists(pacientesCsv)) {
                try (BufferedWriter w = Files.newBufferedWriter(pacientesCsv, StandardCharsets.UTF_8)) {
                    w.write("patient_id,full_name,document_id,age,sex,contact_email,registration_date,clinical_notes,checksum_fasta,file_size_bytes,active\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo preparar pacientes.csv", e);
        }
    }

    public void appendPaciente(PacienteDTO p) {
        String line = String.join(",",
                csv(p.getPatientId()),
                csv(p.getFullName()),
                csv(p.getDocumentId()),
                String.valueOf(p.getEdad()),
                csv(p.getSexo()),
                csv(p.getContactEmail()),
                csv(Instant.now().toString()),
                csv(p.getClinicalNotes()),
                csv(""),                      // checksum_fasta (por ahora vacío)
                csv(""),                      // file_size_bytes (por ahora vacío)
                csv(String.valueOf(p.isActive()))
        );
        appendLine(line);
    }

    private void appendLine(String line) {
        try (BufferedWriter w = Files.newBufferedWriter(pacientesCsv, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir en pacientes.csv", e);
        }
    }

    private static String csv(String v) {
        if (v == null) return "";
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s + "\"";
        return s;
    }
}