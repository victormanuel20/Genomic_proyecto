package org.vicnata.negocio;

import org.vicnata.dto.PacienteDTO;
import org.vicnata.utils.UtilFechas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.nio.file.Files;

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

    // -------------------------------------------------------------
// BÚSQUEDA POR patient_id (para RETRIEVE)
// CSV esperado (tu header actual):
// patient_id,full_name,document_id,age,sex,contact_email,registration_date,clinical_notes,checksum_fasta,file_size_bytes,active
// -------------------------------------------------------------
    public java.util.Optional<PacienteDTO> buscarPorPatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return java.util.Optional.empty();
        }

        if (!java.nio.file.Files.exists(pacientesCsv)) {
            return java.util.Optional.empty();
        }

        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(pacientesCsv, java.nio.charset.StandardCharsets.UTF_8);
            if (lines.isEmpty()) return java.util.Optional.empty();

            // Mapear índices por nombre de columna (por si cambia el orden en el futuro)
            String[] header = lines.get(0).split(",", -1);
            java.util.Map<String,Integer> idx = new java.util.HashMap<>();
            for (int i = 0; i < header.length; i++) {
                idx.put(header[i].trim().toLowerCase(), i);
            }

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] c = line.split(",", -1);

                String pid = col(c, idx.get("patient_id"));
                if (!patientId.equals(pid)) continue;

                // Reconstruir DTO (usando las columnas disponibles)
                PacienteDTO p = new PacienteDTO();
                p.setPatientId(pid);

                String fullName = col(c, idx.get("full_name"));
                p.setNombre(extraNombre(fullName));
                p.setApellido(extraApellido(fullName));

                p.setDocumentId(col(c, idx.get("document_id")));
                p.setEdad(parseInt(col(c, idx.get("age"))));
                p.setSexo(col(c, idx.get("sex")));
                p.setContactEmail(col(c, idx.get("contact_email")));
                p.setRegistrationDate(col(c, idx.get("registration_date")));
                p.setClinicalNotes(col(c, idx.get("clinical_notes")));
                p.setChecksumFasta(col(c, idx.get("checksum_fasta")));
                p.setFileSizeBytes(parseLong(col(c, idx.get("file_size_bytes"))));
                p.setActive(parseBool(col(c, idx.get("active"))));

                return java.util.Optional.of(p);
            }

            return java.util.Optional.empty();

        } catch (java.io.IOException e) {
            throw new RuntimeException("No se pudo leer pacientes.csv: " + pacientesCsv, e);
        }
    }

    // Marca al paciente como inactivo (active=false). Devuelve true si lo encontró y actualizó.
    public boolean marcarInactivo(String patientId) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(pacientesCsv, java.nio.charset.StandardCharsets.UTF_8);
            if (lines.isEmpty()) return false;

            String header = lines.get(0);
            java.util.List<String> out = new java.util.ArrayList<>();
            out.add(header);

            boolean actualizado = false;

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) { out.add(line); continue; }

                String[] c = line.split(",", -1);
                String pid = col(c, 0);
                if (patientId.equals(pid)) {
                    // columnas (por tu header actual):
                    // 0 patient_id, 1 full_name, 2 document_id, 3 age, 4 sex, 5 contact_email,
                    // 6 registration_date, 7 clinical_notes, 8 checksum_fasta, 9 file_size_bytes, 10 active
                    c[10] = "false";
                    String newLine = String.join(",",
                            c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10]
                    );
                    out.add(newLine);
                    actualizado = true;
                } else {
                    out.add(line);
                }
            }

            java.nio.file.Files.write(pacientesCsv, out, java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.CREATE);

            return actualizado;
        } catch (java.io.IOException e) {
            throw new RuntimeException("No se pudo actualizar pacientes.csv", e);
        }
    }


    public void updateCamposMetadata(String patientId,
                                     Optional<String> email,
                                     Optional<String> notas,
                                     Optional<String> fullName,
                                     Optional<Integer> age) {
        try {
            List<String> lines = Files.readAllLines(pacientesCsv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;

            String header = lines.get(0); // conserva cabecera
            List<String> out = new ArrayList<>();
            out.add(header);

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;

                String[] cols = line.split(",", -1);
                // Esperado: patient_id, full_name, document_id, age, sex, contact_email,
                //           registration_date, clinical_notes, checksum_fasta, file_size_bytes, active
                if (cols.length < 11) { out.add(line); continue; }

                if (cols[0].equals(patientId)) {
                    // full_name
                    if (fullName.isPresent() && !fullName.get().isBlank()) {
                        cols[1] = csv(fullName.get());
                    }
                    // age
                    if (age.isPresent()) {
                        cols[3] = String.valueOf(age.get());
                    }
                    // contact_email
                    if (email.isPresent() && !email.get().isBlank()) {
                        cols[5] = csv(email.get());
                    }
                    // clinical_notes
                    if (notas.isPresent()) {
                        cols[7] = csv(notas.get() == null ? "" : notas.get());
                    }
                    line = String.join(",", cols);
                }
                out.add(line);
            }
            Files.write(pacientesCsv, out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo actualizar metadata en pacientes.csv", e);
        }
    }

    public void updateChecksumYSize(String patientId, String checksum, long size) {
        try {
            List<String> lines = Files.readAllLines(pacientesCsv, StandardCharsets.UTF_8);
            if (lines.isEmpty()) return;

            String header = lines.get(0);
            List<String> out = new ArrayList<>();
            out.add(header);

            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.trim().isEmpty()) continue;

                String[] cols = line.split(",", -1);
                if (cols.length < 11) { out.add(line); continue; }

                if (cols[0].equals(patientId)) {
                    cols[8]  = csv(checksum == null ? "" : checksum);
                    cols[9]  = String.valueOf(size);
                    line = String.join(",", cols);
                }
                out.add(line);
            }
            Files.write(pacientesCsv, out, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo actualizar checksum/size en pacientes.csv", e);
        }
    }


    // ------------------ helpers locales de parsing ------------------
    private static String col(String[] arr, Integer i) {
        if (i == null || i < 0 || i >= arr.length) return "";
        return arr[i] == null ? "" : arr[i].trim();
    }
    private static String extraNombre(String full) {
        if (full == null || full.isBlank()) return "";
        String[] p = full.trim().split("\\s+", 2);
        return p[0];
    }
    private static String extraApellido(String full) {
        if (full == null || full.isBlank()) return "";
        String[] p = full.trim().split("\\s+", 2);
        return p.length > 1 ? p[1] : "";
    }
    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
    private static long parseLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return 0L; }
    }
    private static boolean parseBool(String s) {
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }




}