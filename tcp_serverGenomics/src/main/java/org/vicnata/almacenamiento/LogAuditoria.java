package org.vicnata.almacenamiento;

import org.vicnata.utils.UtilFechas;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Clase responsable de registrar eventos en un archivo de auditoría.
 * Cada línea del log contiene:
 * fecha/hora | operación | estado | patient_id | document_id | detalle
 *
 * Ejemplo:
 * 2025-09-08T15:20:31Z | CREATE | CREATED | P-0001 | 1002593169 | Paciente almacenado
 */
public class LogAuditoria {
    private final Path logFile;

    /**
     * Constructor.
     * @param rutaLog Ruta del archivo de log. Ej: "./data/logs/auditoria.log"
     */
    public LogAuditoria(String rutaLog) {
        this.logFile = Paths.get(rutaLog);
        init(); // prepara la ruta y el archivo si no existen
    }

    /**
     * Inicializa el archivo de log:
     * - Crea las carpetas necesarias (ej: ./data/logs/)
     * - Crea el archivo si no existe
     */
    private void init() {
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent()); // crea carpetas padre
            }
            if (Files.notExists(logFile)) {
                Files.createFile(logFile); // crea archivo vacío
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo preparar el archivo de auditoría: " + logFile, e);
        }
    }

    /**
     * Registra un evento en el archivo de auditoría.
     *
     * @param operacion  Operación ejecutada (ej: CREATE, RETRIEVE, UPDATE, DELETE)
     * @param status     Estado de la operación (ej: CREATED, ERROR, DUPLICATE, FOUND, NONE)
     * @param patientId  ID interno del paciente (ej: P-0001) — puede ser null si aún no existe
     * @param documentId Documento original del paciente (ej: CC-1002593169)
     * @param detalle    Mensaje adicional (ej: "Paciente almacenado", "Checksum inválido")
     */
    public void registrar(String operacion, String status, String patientId, String documentId, String detalle) {
        // Construye una línea con todos los campos separados por '|'
        String line = String.join(" | ",
                UtilFechas.ahoraIsoBogota(), // fecha/hora actual en ISO
                safe(operacion),             // operación
                safe(status),                // estado
                safe(patientId),             // patient_id (puede estar vacío)
                safe(documentId),            // document_id
                safe(detalle)                // descripción
        );

        // Escribe en el archivo en modo append
        try (BufferedWriter w = Files.newBufferedWriter(
                logFile, StandardCharsets.UTF_8, StandardOpenOption.APPEND)) {
            w.write(line);
            w.newLine();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo escribir en el archivo de auditoría", e);
        }
    }

    /**
     * Convierte valores null en cadena vacía para evitar errores en el log.
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
