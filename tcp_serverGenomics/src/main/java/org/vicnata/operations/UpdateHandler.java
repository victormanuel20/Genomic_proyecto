// org.vicnata.operations.UpdateHandler (SERVIDOR)
package org.vicnata.operations;

import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.*;
import org.vicnata.red.ProtocolManager;
import org.vicnata.validadores.ValidadorSecuencia; // ← IMPORTANTE

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maneja:
 *  UPDATE\PATIENT_ID\EMAIL\CLINICAL_NOTES\FULL_NAME\AGE[\HEADER\SECUENCIA\SIZE\HASH\CHECKSUM\BASE64]
 *
 * Respuestas:
 *  OK|UPDATE|UPDATED|patient_id|DISEASES=influenza(7);sarscov2(9)
 *  OK|UPDATE|UPDATED|patient_id|DISEASES=NONE
 *  ERROR|UPDATE|INVALID|...
 *  ERROR|UPDATE|NOT_FOUND|patient_id
 *  ERROR|UPDATE|MISSING_CONFIG|...
 *  ERROR|UPDATE|INTERNAL_ERROR|...
 */
public class UpdateHandler implements OperationHandler {
    private final LogAuditoria logger;

    public UpdateHandler(LogAuditoria logger) { this.logger = logger; }

    @Override
    public String handle(String[] p) {
        // 0) Validación mínima
        if (p == null || p.length < 2) {
            logger.registrar("UPDATE", "INVALID", null, null, "Falta PATIENT_ID");
            return ProtocolManager.error("UPDATE", "INVALID|Falta PATIENT_ID");
        }
        final String patientId = safe(p[1]);
        if (patientId.isBlank() || "-".equals(patientId)) {
            logger.registrar("UPDATE", "INVALID", null, null, "PATIENT_ID vacío");
            return ProtocolManager.error("UPDATE", "INVALID|PATIENT_ID vacío");
        }

        try {
            // 1) Config
            GestorPropiedades cfg = new GestorPropiedades();
            String pathCsvPacientes   = cfg.getProperty("PATH_CSV_PACIENTES");
            String pathFastaStorage   = cfg.getProperty("PATH_FASTA_STORAGE");
            String pathEnfermedades   = cfg.getProperty("PATH_ENFERMEDADES");
            String pathCatalogo       = cfg.getProperty("PATH_CATALOGO_ENF");
            String pathCsvDetecciones = cfg.getProperty("PATH_CSV_DETECCIONES");
            if (isBlank(pathCsvPacientes) || isBlank(pathFastaStorage)
                    || isBlank(pathEnfermedades) || isBlank(pathCatalogo)
                    || isBlank(pathCsvDetecciones)) {
                logger.registrar("UPDATE", "MISSING_CONFIG", patientId, null, "Rutas incompletas");
                return ProtocolManager.error("UPDATE","MISSING_CONFIG|Revisa configuration.properties");
            }

            // 2) Buscar paciente
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            Optional<PacienteDTO> opt = repo.buscarPorPatientId(patientId);
            if (opt.isEmpty()) {
                logger.registrar("UPDATE", "NOT_FOUND", patientId, null, "No existe");
                return ProtocolManager.error("UPDATE", "NOT_FOUND|" + patientId);
            }
            PacienteDTO actual = opt.get();

            // 3) Campos opcionales metadata
            // 0=UPDATE | 1=PID | 2=EMAIL | 3=NOTAS | 4=FULL_NAME | 5=AGE
            String email    = (p.length >= 3 && !p[2].equals("-")) ? p[2] : null;
            String notas    = (p.length >= 4 && !p[3].equals("-")) ? p[3] : null;
            String fullName = (p.length >= 5 && !p[4].equals("-")) ? p[4] : null;
            Integer age     = null;
            if (p.length >= 6 && !p[5].equals("-")) {
                try { age = Integer.parseInt(p[5]); } catch (NumberFormatException ignored) {}
            }
            // Validaciones mínimas
            if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                logger.registrar("UPDATE", "INVALID", patientId, actual.getDocumentId(), "Email inválido");
                return ProtocolManager.error("UPDATE", "INVALID|Email inválido");
            }
            if (age != null && (age < 0 || age > 120)) {
                logger.registrar("UPDATE", "INVALID", patientId, actual.getDocumentId(), "Edad fuera de rango");
                return ProtocolManager.error("UPDATE", "INVALID|Edad fuera de rango");
            }

            // 4) Aplicar cambios de metadata
            repo.updateCamposMetadata(
                    patientId,
                    Optional.ofNullable(email),
                    Optional.ofNullable(notas),
                    Optional.ofNullable(fullName),
                    Optional.ofNullable(age)
            );

            // 4.1) Releer para metadata fresca
            actual = repo.buscarPorPatientId(patientId).orElse(actual);
            String fullNameActual = (safe(actual.getNombre()) + " " + safe(actual.getApellido())).trim();
            if (fullName != null) fullNameActual = fullName.trim();
            String nuevoDocumento = actual.getDocumentId();

            // 4.2) Sincronizar detecciones.csv (si cambió nombre/doc)
            if (!isBlank(pathCsvDetecciones)) {
                DeteccionesCSV repDet = new DeteccionesCSV(pathCsvDetecciones);
                repDet.actualizarMetadataPaciente(patientId, fullNameActual, nuevoDocumento);
                logger.registrar("UPDATE", "SYNC_DETECCIONES", patientId, nuevoDocumento,
                        "full_name/doc sincronizados en detecciones.csv");
            }

            // 5) Bloque FASTA opcional
            boolean traeFasta = (p.length >= 12);
            String detectionsOut = "NONE";

            if (traeFasta) {
                // Indices del bloque FASTA cuando viene:
                // p[6]=HEADER | p[7]=SECUENCIA | p[8]=SIZE | p[9]=HASH | p[10]=CHECKSUM | p[11]=BASE64
                String header   = safe(p[6]);
                String secuencia= safe(p[7]);
                Long   size     = null; try { size = Long.parseLong(p[8]); } catch (Exception ignored) {}
                String hashAlgo = safe(p[9]);
                String checksum = safe(p[10]);
                String base64   = safe(p[11]);

                // 5.a) VALIDAR FASTA (header + secuencia)
                try {
                    ValidadorSecuencia.validarHeaderYSecuencia(header, secuencia);
                    // 5.b) Si llegó archivo exacto: validar integridad
                    if (!base64.isBlank()) {
                        ValidadorSecuencia.validarTamanoYChecksum(base64, hashAlgo, checksum, size);
                    }
                } catch (IllegalArgumentException ve) {
                    logger.registrar("UPDATE", "INVALID", patientId, actual.getDocumentId(), ve.getMessage());
                    return ProtocolManager.error("UPDATE", "INVALID|" + ve.getMessage());
                }

                // 5.c) Guardar FASTA
                FastaStorage storage = new FastaStorage(pathFastaStorage);
                if (!base64.isBlank()) {
                    storage.guardarExactoDesdeBase64(patientId, base64);
                } else {
                    storage.guardarLegibleDesdeSecuencia(patientId, secuencia);
                }
                if (!isBlank(checksum) && size != null && size > 0) {
                    repo.updateChecksumYSize(patientId, checksum, size);
                }

                // 6) Re-evaluar enfermedades SOLO si llegó FASTA nuevo
                CargadorCatalogo cat = new CargadorCatalogo(pathCatalogo);
                Map<String,Integer> sevPorId = cat.severidad();

                CargadorEnfermedades loader = new CargadorEnfermedades(pathEnfermedades);
                List<Enfermedad> catalogo = loader.cargar();

                List<EnfermedadDetector.Deteccion> hits =
                        EnfermedadDetector.detectar(secuencia, catalogo);

                if (!hits.isEmpty()) {
                    DeteccionesCSV rep = new DeteccionesCSV(pathCsvDetecciones);
                    String full = fullNameActual;
                    String doc  = nuevoDocumento;

                    detectionsOut = hits.stream().map(d -> {
                        int sev = sevPorId.getOrDefault(d.diseaseId, 0);
                        rep.append(patientId, full, doc, d.diseaseId, sev, d.description);
                        logger.registrar("DETECTION", "FOUND", patientId, doc,
                                d.diseaseId + "(" + sev + ") " + d.description);
                        return d.diseaseId + "(" + sev + ")";
                    }).collect(Collectors.joining(";"));
                }
            } else {
                // Fallback: sin FASTA, devolvemos lo previo del reporte
                DeteccionesCSV rep = new DeteccionesCSV(pathCsvDetecciones);
                var prev = rep.enfermedadesDe(patientId);
                if (!prev.isEmpty()) {
                    detectionsOut = prev.stream()
                            .map(d -> d.diseaseId + "(" + d.severity + ")")
                            .collect(Collectors.joining(";"));
                }
            }

            logger.registrar("UPDATE", "UPDATED", patientId, actual.getDocumentId(),
                    (traeFasta ? "metadata+fasta" : "metadata"));
            return ProtocolManager.ok("UPDATE", "UPDATED|" + patientId + "|DISEASES=" + detectionsOut);

        } catch (Exception ex) {
            String msg = ex.getClass().getSimpleName() + (ex.getMessage()!=null?":"+ex.getMessage():"");
            logger.registrar("UPDATE", "ERROR", patientId, null, msg);
            return ProtocolManager.error("UPDATE", "INTERNAL_ERROR|" + msg);
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String safe(String s) { return s == null ? "" : s; }
}
