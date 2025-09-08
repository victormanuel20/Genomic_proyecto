package org.vicnata.negocio;

import org.vicnata.dto.PacienteDTO;
import org.vicnata.utils.UtilFechas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;

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

    /** checa docId ya existente (parser simple) */
    public boolean existePorDocumento(String documentId) {
        try (Stream<String> lines = Files.lines(pacientesCsv, StandardCharsets.UTF_8)) {
            return lines.skip(1)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .anyMatch(line -> {
                        String[] cols = line.split(",", -1);
                        return cols.length >= 3 && documentId.equals(cols[2]);
                    });
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer pacientes.csv", e);
        }
    }

    public void appendPaciente(PacienteDTO p) {
        appendPaciente(p, null, null);
    }

    public void appendPaciente(PacienteDTO p, String checksum, String sizeBytes) {
        String line = String.join(",",
                csv(p.getPatientId()),
                csv(p.getFullName()),
                csv(p.getDocumentId()),
                String.valueOf(p.getEdad()),
                csv(p.getSexo()),
                csv(p.getContactEmail()),
                csv(UtilFechas.ahoraIsoBogota()),
                csv(p.getClinicalNotes()),
                csv(checksum == null ? "" : checksum),
                csv(sizeBytes == null ? "" : sizeBytes),
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
        String s = v.replace("\"","\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s + "\"";
        return s;
    }
}