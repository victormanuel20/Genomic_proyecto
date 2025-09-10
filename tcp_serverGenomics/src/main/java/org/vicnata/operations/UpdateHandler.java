// org.vicnata.operations.UpdateHandler (SERVIDOR)
package org.vicnata.operations;

import org.vicnata.almacenamiento.LogAuditoria;
import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.*;
import org.vicnata.red.ProtocolManager;
import org.vicnata.negocio.DeteccionesCSV;

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
        // Validación básica del mínimo: UPDATE\PATIENT_ID
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
            // 1) Cargar configuración requerida
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
                return ProtocolManager.error("UPDATE",
                        "MISSING_CONFIG|Revisa configuration.properties");
            }

            // 2) Buscar paciente
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            Optional<PacienteDTO> opt = repo.buscarPorPatientId(patientId);
            if (opt.isEmpty()) {
                logger.registrar("UPDATE", "NOT_FOUND", patientId, null, "No existe");
                return ProtocolManager.error("UPDATE", "NOT_FOUND|" + patientId);
            }
            PacienteDTO actual = opt.get();

            // 3) Leer campos opcionales de metadata
            // Indices: 0 UPDATE | 1 PID | 2 EMAIL | 3 NOTAS | 4 FULL_NAME | 5 AGE
            String email = (p.length >= 3 && !p[2].equals("-")) ? p[2] : null;
            String notas = (p.length >= 4 && !p[3].equals("-")) ? p[3] : null;
            String fullName = (p.length >= 5 && !p[4].equals("-")) ? p[4] : null;
            Integer age = null;
            if (p.length >= 6 && !p[5].equals("-")) {
                try { age = Integer.parseInt(p[5]); } catch (NumberFormatException ignored) {}
            }

            // Validaciones mínimas si se actualiza email/edad (opcional):
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



                // --- 4.1) Releer paciente para tener la metadata "fresca" (nombre/doc actualizados) ---
                actual = repo.buscarPorPatientId(patientId).orElse(actual);

            // Construimos full_name actual (o el nuevo si llegó FULL_NAME en el UPDATE)
                String fullNameActual = (safe(actual.getNombre()) + " " + safe(actual.getApellido())).trim();

        // Si en tu UPDATE permites CAMBIAR documento, aquí léelo.
        // En tu protocolo actual no mandas nuevo documento, así que usamos el vigente:
                String nuevoDocumento = actual.getDocumentId();

            // Si tu UPDATE puede traer FULL_NAME (p[4]) y quieres priorizarlo si llegó:
                if (p.length >= 5 && !p[4].equals("-")) {
                    fullNameActual = p[4].trim();
                }

                // --- 4.2) Sincronizar detecciones.csv con el full_name/documento vigentes ---
                String pathCsvDeteccioness = cfg.getProperty("PATH_CSV_DETECCIONES");
                if (!isBlank(pathCsvDeteccioness)) {
                    DeteccionesCSV repDet = new DeteccionesCSV(pathCsvDeteccioness);
                    repDet.actualizarMetadataPaciente(patientId, fullNameActual, nuevoDocumento);
                    logger.registrar("UPDATE", "SYNC_DETECCIONES", patientId, nuevoDocumento,
                            "full_name/doc sincronizados en detecciones.csv");
                }

            // 5) Bloque FASTA opcional
            boolean traeFasta = (p.length >= 12); // si vienen los 6 campos (3..8) + indices base
            String secuencia = null;
            String checksum  = null;
            Long   size      = null;

            if (traeFasta) {
                // p[6] HEADER (ignoramos), p[7] SECUENCIA, p[8] SIZE, p[9] HASH, p[10] CHECKSUM, p[11] BASE64
                secuencia = safe(p[7]);
                try { size = Long.parseLong(p[8]); } catch (Exception ignored) {}
                checksum = safe(p[10]);
                String base64 = safe(p[11]);

                FastaStorage storage = new FastaStorage(pathFastaStorage);
                if (!base64.isBlank()) {
                    storage.guardarExactoDesdeBase64(patientId, base64);
                } else {
                    storage.guardarLegibleDesdeSecuencia(patientId, secuencia);
                }
                if (checksum != null && !checksum.isBlank() && size != null && size > 0) {
                    repo.updateChecksumYSize(patientId, checksum, size);
                }
            }

            // 6) Re-evaluar si llegó FASTA; si NO llegó FASTA, leemos detecciones previas
            String detectionsOut = "NONE";
            if (traeFasta && secuencia != null && !secuencia.isBlank()) {
                CargadorCatalogo cat = new CargadorCatalogo(pathCatalogo);
                Map<String,Integer> sevPorId = cat.severidad();

                CargadorEnfermedades loader = new CargadorEnfermedades(pathEnfermedades);
                List<Enfermedad> catalogo = loader.cargar();

                List<EnfermedadDetector.Deteccion> hits = EnfermedadDetector.detectar(secuencia, catalogo);

                if (!hits.isEmpty()) {
                    DeteccionesCSV rep = new DeteccionesCSV(pathCsvDeteccioness);
                    String full = (actual.getNombre() + " " + actual.getApellido()).trim();
                    String doc  = actual.getDocumentId();

                    detectionsOut = hits.stream().map(d -> {
                        int sev = sevPorId.getOrDefault(d.diseaseId, 0);
                        rep.append(patientId, full, doc, d.diseaseId, sev, d.description);
                        logger.registrar("DETECTION", "FOUND", patientId, doc, d.diseaseId + "(" + sev + ") " + d.description);
                        return d.diseaseId + "(" + sev + ")";
                    }).collect(Collectors.joining(";"));
                }
            } else {
                // F A L L B A C K: sin FASTA, mostrar lo que YA tiene en detecciones.csv
                DeteccionesCSV rep = new DeteccionesCSV(pathCsvDeteccioness);
                var prev = rep.enfermedadesDe(patientId); // List<DiseaseTag>
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
