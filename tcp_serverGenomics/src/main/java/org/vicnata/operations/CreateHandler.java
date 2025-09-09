package org.vicnata.operations;

import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.*;
import org.vicnata.red.ProtocolManager;
import org.vicnata.utils.IdGenerator;
import org.vicnata.validadores.validadorPaciente;
import org.vicnata.almacenamiento.LogAuditoria;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateHandler implements OperationHandler {
    private final LogAuditoria logger;

    public CreateHandler(LogAuditoria logger) {
        this.logger = logger;
    }

    @Override
    public String handle(String[] p) {
        // 0 CREATE | 1 NOMBRE | 2 APELLIDO | 3 DOC | 4 EDAD | 5 SEXO | 6 EMAIL | 7 NOTAS | 8 FASTA_ID | 9 SECUENCIA
        // 10 SIZE | 11 HASH_ALGO | 12 CHECKSUM | 13 BASE64
        if (p == null || p.length < 10) {
            logger.registrar("CREATE", "MISSING_FIELDS", null, null, "min>=10");
            return ProtocolManager.error("CREATE", "MISSING_FIELDS|min>=10 got=" + (p == null ? 0 : p.length));
        }

        String nombre      = p[1];
        String apellido    = p[2];
        String docId       = p[3];
        String edadStr     = p[4];
        String sexo        = p[5];
        String email       = p[6];
        String notas       = p[7];
        String secuencia   = p[9];

        String sizeBytes = (p.length >= 11 ? p[10] : null);
        String hashAlgo  = (p.length >= 12 ? p[11] : null);
        String checksum  = (p.length >= 13 ? p[12] : null);
        String base64    = (p.length >= 14 ? p[13] : null);

        try {
            // 1) Validaciones
            int edad = validadorPaciente.validarBasicos(nombre, apellido, docId, edadStr, sexo, email);

            // 2) Rutas requeridas
            GestorPropiedades cfg = new GestorPropiedades();
            String pathData           = cfg.getProperty("PATH_DATA");
            String pathCsvPacientes   = cfg.getProperty("PATH_CSV_PACIENTES");
            String pathFastaStorage   = cfg.getProperty("PATH_FASTA_STORAGE");
            String pathEnfermedades   = cfg.getProperty("PATH_ENFERMEDADES");
            String pathCatalogo       = cfg.getProperty("PATH_CATALOGO_ENF");
            String pathCsvDetecciones = cfg.getProperty("PATH_CSV_DETECCIONES");

            if (isBlank(pathData) || isBlank(pathCsvPacientes) || isBlank(pathFastaStorage)
                    || isBlank(pathEnfermedades) || isBlank(pathCatalogo) || isBlank(pathCsvDetecciones)) {
                logger.registrar("CREATE", "MISSING_CONFIG", null, docId, "PATH_* faltantes");
                return ProtocolManager.error("CREATE",
                        "MISSING_CONFIG|Revisa configuration.properties (PATH_DATA, PATH_CSV_PACIENTES, " +
                                "PATH_CSV_DETECCIONES, PATH_FASTA_STORAGE, PATH_ENFERMEDADES, PATH_CATALOGO_ENF)");
            }

            // 3) Duplicados
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            if (repo.existePorDocumento(docId)) {
                logger.registrar("CREATE", "DUPLICATE", null, docId, "Documento ya existe");
                return ProtocolManager.error("CREATE", "DUPLICATE_DOCUMENT_ID|" + docId);
            }

            // 4) patient_id
            String patientId = IdGenerator.nextPatientId(pathData);

            // 5) Guardar paciente
            PacienteDTO pac = new PacienteDTO();
            pac.setPatientId(patientId);
            pac.setNombre(nombre);
            pac.setApellido(apellido);
            pac.setDocumentId(docId);
            pac.setEdad(edad);
            pac.setSexo(sexo.toUpperCase());
            pac.setContactEmail(email);
            pac.setClinicalNotes(notas);
            pac.setActive(true);

            if (nonEmpty(checksum) || nonEmpty(sizeBytes)) {
                repo.appendPaciente(pac, checksum, sizeBytes);
            } else {
                repo.appendPaciente(pac);
            }
            logger.registrar("CREATE", "CREATED", patientId, docId, "Paciente almacenado");

            // 6) Guardar FASTA
            FastaStorage storage = new FastaStorage(pathFastaStorage);
            if (nonEmpty(base64)) {
                storage.guardarExactoDesdeBase64(patientId, base64);
                logger.registrar("FASTA", "SAVED", patientId, docId, "Guardado exacto (BASE64)");
            } else {
                if (isBlank(secuencia)) {
                    logger.registrar("CREATE", "INVALID", patientId, docId, "Secuencia vacía");
                    return ProtocolManager.error("CREATE", "MISSING_SEQUENCE|No se recibió secuencia ni BASE64");
                }
                storage.guardarLegibleDesdeSecuencia(patientId, secuencia);
                logger.registrar("FASTA", "SAVED", patientId, docId, "Guardado legible (desde secuencia)");
            }

            // 7) Cargar catálogo y enfermedades
            CargadorCatalogo cat = new CargadorCatalogo(pathCatalogo);
            Map<String,Integer> sevPorId = cat.severidad();

            CargadorEnfermedades loader = new CargadorEnfermedades(pathEnfermedades);
            List<Enfermedad> catalogo = loader.cargar();

            // 8) Detectar
            String fullName = nombre + " " + apellido;
            List<EnfermedadDetector.Deteccion> hits = EnfermedadDetector.detectar(secuencia, catalogo);

            if (!hits.isEmpty()) {
                DeteccionesCSV rep = new DeteccionesCSV(pathCsvDetecciones);
                String detectionsOut = hits.stream().map(d -> {
                    int sev = sevPorId.getOrDefault(d.diseaseId, 0);
                    rep.append(patientId, fullName, docId, d.diseaseId, sev, d.description);
                    logger.registrar("DETECTION", "FOUND", patientId, docId,
                            d.diseaseId + "(" + sev + ") " + d.description);
                    return d.diseaseId + "(" + sev + ")";
                }).collect(Collectors.joining(";"));

                return ProtocolManager.ok("CREATE",
                        "CREATED|" + patientId + "|" + fullName + "|" + docId + "|DETECTIONS=" + detectionsOut);
            } else {
                logger.registrar("DETECTION", "NONE", patientId, docId, "Sin coincidencias");
                return ProtocolManager.ok("CREATE",
                        "CREATED|" + patientId + "|" + fullName + "|" + docId + "|DETECTIONS=NONE");
            }

        } catch (IllegalArgumentException ex) {
            logger.registrar("CREATE", "INVALID", null, docId, ex.getMessage());
            return ProtocolManager.error("CREATE", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = ex.getClass().getSimpleName() + (ex.getMessage()!=null?(":"+ex.getMessage()):"");
            logger.registrar("CREATE", "ERROR", null, docId, msg);
            return ProtocolManager.error("CREATE", "INTERNAL_ERROR|" + msg);
        }
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static boolean nonEmpty(String s) { return s != null && !s.isBlank(); }
}
