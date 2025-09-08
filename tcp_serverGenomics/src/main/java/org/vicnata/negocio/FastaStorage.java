package org.vicnata.negocio;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;

public class FastaStorage {
    private final Path dir;

    public FastaStorage(String dirPath) {
        this.dir = Paths.get(dirPath);
        try { Files.createDirectories(dir); } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Escribe FASTA EXACTO desde base64 (mismos bytes) → útil si ya viene completo. */
    public Path guardarExactoDesdeBase64(String patientId, String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            Path out = dir.resolve(patientId + ".fasta");
            Files.write(out, bytes);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar FASTA (exacto) para " + patientId, e);
        }
    }

    /** Genera un FASTA legible: primera línea '>' + patientId y secuencia envuelta a 60 char/linea. */
    public Path guardarLegibleDesdeSecuencia(String patientId, String secuencia) {
        try {
            String header = ">" + patientId;
            StringBuilder body = new StringBuilder();

            int wrap = 60;
            for (int i = 0; i < secuencia.length(); i += wrap) {
                int end = Math.min(i + wrap, secuencia.length());
                body.append(secuencia, i, end).append("\n");
            }

            Path out = dir.resolve(patientId + ".fasta");
            String contenido = header + "\n" + body;
            Files.writeString(out, contenido, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return out;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar FASTA (legible) para " + patientId, e);
        }
    }
}