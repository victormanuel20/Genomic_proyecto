package org.vicnata.almacenamiento;

import org.vicnata.utils.UtilFechas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class LogAuditoria {
    private final Path logFile;

    public LogAuditoria(String rutaLog) {
        this.logFile = Paths.get(rutaLog);
        init();
    }

    private void init() {
        try {
            if (logFile.getParent() != null) Files.createDirectories(logFile.getParent());
            if (Files.notExists(logFile)) {
                Files.createFile(logFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo preparar el archivo de auditoría: " + logFile, e);
        }
    }

    /**
     * Línea: datetime | op | status | patient_id | document_id | detalle
     */
    public void registrar(String operacion, String status, String patientId, String documentId, String detalle) {
        String line = String.join(" | ",
                UtilFechas.ahoraIsoBogota(),
                safe(operacion),
                safe(status),
                safe(patientId),
                safe(documentId),
                safe(detalle)
        );
        try (BufferedWriter w = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir en el archivo de auditoría", e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}

