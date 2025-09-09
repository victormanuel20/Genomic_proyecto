package org.vicnata.operations;

import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.GestorCSV;
import org.vicnata.red.ProtocolManager;

import java.util.Optional;

/**
 * Maneja: DELETE\patient_id
 * Respuestas:
 *  OK|DELETE|INACTIVATED|patient_id
 *  ERROR|DELETE|NOT_FOUND|patient_id
 *  ERROR|DELETE|ALREADY_INACTIVE|patient_id
 *  ERROR|DELETE|INVALID|reason
 *  ERROR|DELETE|MISSING_CONFIG|...
 *  ERROR|DELETE|INTERNAL_ERROR|...
 */
public class DeleteHandler implements OperationHandler {
    private final LogAuditoria logger;
    public DeleteHandler(LogAuditoria logger) { this.logger = logger; }

    @Override
    public String handle(String[] p) {
        // Esperado: ["DELETE", "P-0001"]
        if (p == null || p.length < 2) {
            logger.registrar("DELETE","INVALID", null, null, "Falta patient_id");
            return ProtocolManager.error("DELETE","INVALID|Falta patient_id");
        }
        String patientId = p[1] == null ? "" : p[1].trim();
        if (patientId.isEmpty() || "-".equals(patientId)) {
            logger.registrar("DELETE","INVALID", null, null, "patient_id vacío");
            return ProtocolManager.error("DELETE","INVALID|patient_id vacío");
        }

        try {
            GestorPropiedades cfg = new GestorPropiedades();
            String pathCsvPacientes = cfg.getProperty("PATH_CSV_PACIENTES");
            if (isBlank(pathCsvPacientes)) {
                logger.registrar("DELETE","MISSING_CONFIG",patientId,null,"PATH_CSV_PACIENTES");
                return ProtocolManager.error("DELETE","MISSING_CONFIG|PATH_CSV_PACIENTES no configurado");
            }

            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            Optional<PacienteDTO> opt = repo.buscarPorPatientId(patientId);
            if (opt.isEmpty()) {
                logger.registrar("DELETE","NOT_FOUND",patientId,null,"No existe");
                return ProtocolManager.error("DELETE","NOT_FOUND|" + patientId);
            }
            PacienteDTO pac = opt.get();
            if (!pac.isActive()) {
                logger.registrar("DELETE","ALREADY_INACTIVE",patientId,pac.getDocumentId(),"No-op");
                return ProtocolManager.error("DELETE","ALREADY_INACTIVE|" + patientId);
            }

            boolean ok = repo.marcarInactivo(patientId);
            if (!ok) {
                logger.registrar("DELETE","ERROR",patientId,pac.getDocumentId(),"No se pudo marcar inactivo");
                return ProtocolManager.error("DELETE","INTERNAL_ERROR|No se pudo inactivar");
            }

            logger.registrar("DELETE","INACTIVATED",patientId,pac.getDocumentId(),"OK");
            return String.join("|", "OK","DELETE","INACTIVATED", patientId);

        } catch (Exception e) {
            String msg = e.getClass().getSimpleName() + (e.getMessage()!=null?":"+e.getMessage():"");
            logger.registrar("DELETE","ERROR",patientId,null,msg);
            return ProtocolManager.error("DELETE","INTERNAL_ERROR|" + msg);
        }
    }

    private static boolean isBlank(String s){ return s==null || s.trim().isEmpty(); }
}

