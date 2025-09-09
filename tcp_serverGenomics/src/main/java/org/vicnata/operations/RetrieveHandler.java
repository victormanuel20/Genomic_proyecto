package org.vicnata.operations;

// Importa clases necesarias para auditoría, DTOs, configuración, acceso a CSV y protocolo
import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.GestorCSV;
import org.vicnata.negocio.DeteccionesCSV;     // ← NUEVO: leer enfermedades detectadas
import org.vicnata.red.ProtocolManager;

import java.util.List;
import java.util.Optional;

/**
 * Handler para la operación RETRIEVE\patient_id
 * Devuelve la información completa del paciente, incluyendo enfermedades detectadas.
 * Formato de respuesta:
 *  OK|RETRIEVE|FOUND|...|DISEASES=...
 *  ERROR|RETRIEVE|NOT_FOUND|...
 *  ERROR|RETRIEVE|INVALID|...
 *  ERROR|RETRIEVE|MISSING_CONFIG|...
 *  ERROR|RETRIEVE|INTERNAL_ERROR|...
 */
public class RetrieveHandler implements OperationHandler {
    private final LogAuditoria logger;

    public RetrieveHandler(LogAuditoria logger) { this.logger = logger; }

    @Override
    public String handle(String[] p) {
        // Verifica que el mensaje tenga al menos 2 partes (RETRIEVE y patient_id)
        if (p == null || p.length < 2) {
            logger.registrar("RETRIEVE", "INVALID", null, null, "Falta patient_id");
            return ProtocolManager.error("RETRIEVE", "INVALID|Falta patient_id");
        }

        // Limpia y valida el patient_id recibido
        String patientId = (p[1] != null) ? p[1].trim() : "";
        if (patientId.isEmpty() || "-".equals(patientId)) {
            logger.registrar("RETRIEVE", "INVALID", null, null, "patient_id vacío");
            return ProtocolManager.error("RETRIEVE", "INVALID|patient_id vacío");
        }

        try {
            // Carga rutas de archivos desde configuration.properties
            GestorPropiedades cfg = new GestorPropiedades();
            String pathCsvPacientes   = cfg.getProperty("PATH_CSV_PACIENTES");
            String pathCsvDetecciones = cfg.getProperty("PATH_CSV_DETECCIONES"); // ← NUEVO

            // Verifica que la ruta de pacientes esté configurada
            if (isBlank(pathCsvPacientes)) {
                logger.registrar("RETRIEVE", "MISSING_CONFIG", patientId, null, "PATH_CSV_PACIENTES");
                return ProtocolManager.error("RETRIEVE", "MISSING_CONFIG|PATH_CSV_PACIENTES no configurado");
            }

            // Busca el paciente en el archivo CSV
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            Optional<PacienteDTO> opt = repo.buscarPorPatientId(patientId);
            if (opt.isEmpty()) {
                logger.registrar("RETRIEVE", "NOT_FOUND", patientId, null, "No existe");
                return ProtocolManager.error("RETRIEVE", "NOT_FOUND|" + patientId);
            }

            // Reconstruye el DTO del paciente
            PacienteDTO pac = opt.get();
            String fullName = (safe(pac.getNombre()) + " " + safe(pac.getApellido())).trim();

            // === NUEVO: busca enfermedades detectadas en detecciones.csv ===
            String diseasesField = "DISEASES=NONE";
            try {
                if (!isBlank(pathCsvDetecciones)) {
                    DeteccionesCSV detRepo = new DeteccionesCSV(pathCsvDetecciones);
                    List<String> etiquetas = detRepo.listarEtiquetasPorPaciente(patientId); // ej: ["sarscov2(9)", "anemia_falciforme(8)"]
                    if (!etiquetas.isEmpty()) {
                        diseasesField = "DISEASES=" + String.join(";", etiquetas);
                    }
                } else {
                    // Si no hay ruta de detecciones, registra advertencia pero no rompe el flujo
                    logger.registrar("RETRIEVE", "WARN", patientId, pac.getDocumentId(), "PATH_CSV_DETECCIONES vacío");
                }
            } catch (Exception exDet) {
                // Si hay error al leer detecciones, registra advertencia pero continúa
                logger.registrar("RETRIEVE", "WARN", patientId, pac.getDocumentId(), "Detecciones no disponibles: " + exDet.getMessage());
            }

            // Construye la respuesta OK con todos los datos del paciente + enfermedades
            String payload = String.join("|",
                    "OK", "RETRIEVE", "FOUND",
                    safe(pac.getPatientId()),
                    fullName,
                    safe(pac.getDocumentId()),
                    String.valueOf(pac.getEdad()),
                    safe(pac.getSexo()),
                    safe(pac.getContactEmail()),
                    safe(pac.getRegistrationDate()),
                    safe(pac.getClinicalNotes()),
                    String.valueOf(pac.isActive()),
                    safe(pac.getChecksumFasta()),
                    String.valueOf(pac.getFileSizeBytes()),
                    diseasesField // ← NUEVO
            );

            // Registra en el log que se encontró el paciente y sus enfermedades
            logger.registrar("RETRIEVE", "FOUND", pac.getPatientId(), pac.getDocumentId(), diseasesField);
            return payload;

        } catch (Exception ex) {
            // Manejo de errores inesperados
            String msg = ex.getClass().getSimpleName() + (ex.getMessage()!=null?":"+ex.getMessage():"");
            logger.registrar("RETRIEVE", "ERROR", patientId, null, msg);
            return ProtocolManager.error("RETRIEVE", "INTERNAL_ERROR|" + msg);
        }
    }

    // Verifica si una cadena está vacía o nula
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    // Devuelve una cadena segura (no nula)
    private static String safe(String s) { return s == null ? "" : s; }
}