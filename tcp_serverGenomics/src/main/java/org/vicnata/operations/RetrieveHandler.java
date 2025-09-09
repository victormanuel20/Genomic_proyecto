package org.vicnata.operations;

import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.GestorCSV;
import org.vicnata.red.ProtocolManager;

import java.util.Optional;

/**
 * Maneja: RETRIEVE\patient_id
 * Respuestas:
 *  OK|RETRIEVE|FOUND|patient_id|full_name|document_id|age|sex|contact_email|registration_date|clinical_notes|active|checksum|file_size
 *  ERROR|RETRIEVE|NOT_FOUND|patient_id
 *  ERROR|RETRIEVE|INVALID|reason
 *  ERROR|RETRIEVE|MISSING_CONFIG|...
 *  ERROR|RETRIEVE|INTERNAL_ERROR|...
 */
public class RetrieveHandler implements OperationHandler {
    private final LogAuditoria logger;

    public RetrieveHandler(LogAuditoria logger) { this.logger = logger; }

    @Override
    public String handle(String[] p) {
        // Esperado: ["RETRIEVE", "P-0001"]
        if (p == null || p.length < 2) {
            logger.registrar("RETRIEVE", "INVALID", null, null, "Falta patient_id");
            return ProtocolManager.error("RETRIEVE", "INVALID|Falta patient_id");
        }

        String patientId = (p[1] != null) ? p[1].trim() : "";
        if (patientId.isEmpty() || "-".equals(patientId)) {
            logger.registrar("RETRIEVE", "INVALID", null, null, "patient_id vacío");
            return ProtocolManager.error("RETRIEVE", "INVALID|patient_id vacío");
        }

        try {
            // Cargar ruta CSV desde properties
            GestorPropiedades cfg = new GestorPropiedades();
            String pathCsvPacientes = cfg.getProperty("PATH_CSV_PACIENTES");
            if (isBlank(pathCsvPacientes)) {
                logger.registrar("RETRIEVE", "MISSING_CONFIG", patientId, null, "PATH_CSV_PACIENTES");
                return ProtocolManager.error("RETRIEVE",
                        "MISSING_CONFIG|PATH_CSV_PACIENTES no configurado");
            }

            // Buscar en CSV por patient_id
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            Optional<PacienteDTO> opt = repo.buscarPorPatientId(patientId);

            if (opt.isEmpty()) {
                logger.registrar("RETRIEVE", "NOT_FOUND", patientId, null, "No existe");
                return ProtocolManager.error("RETRIEVE", "NOT_FOUND|" + patientId);
            }

            PacienteDTO pac = opt.get();
            String fullName = (safe(pac.getNombre()) + " " + safe(pac.getApellido())).trim();

            // Armamos respuesta OK (usa '|' como en tus respuestas actuales)
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
                    String.valueOf(pac.getFileSizeBytes())
            );

            logger.registrar("RETRIEVE", "FOUND", pac.getPatientId(), pac.getDocumentId(), "OK");
            return payload;

        } catch (Exception ex) {
            String msg = ex.getClass().getSimpleName() + (ex.getMessage()!=null?":"+ex.getMessage():"");
            logger.registrar("RETRIEVE", "ERROR", patientId, null, msg);
            return ProtocolManager.error("RETRIEVE", "INTERNAL_ERROR|" + msg);
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return s == null ? "" : s; }
}
