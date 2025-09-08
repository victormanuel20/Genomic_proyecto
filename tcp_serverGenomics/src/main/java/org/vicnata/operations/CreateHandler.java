package org.vicnata.operations;

import org.vicnata.dto.PacienteDTO;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.negocio.*;
import org.vicnata.red.ProtocolManager;
import org.vicnata.utils.IdGenerator;
import org.vicnata.validadores.validadorPaciente;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateHandler implements OperationHandler {

    @Override
    public String handle(String[] p) {
        // Esperado:
        // 0 CREATE | 1 NOMBRE | 2 APELLIDO | 3 DOC | 4 EDAD | 5 SEXO | 6 EMAIL | 7 NOTAS | 8 FASTA_ID | 9 SECUENCIA
        // 10 SIZE | 11 HASH_ALGO | 12 CHECKSUM | 13 BASE64
        if (p == null || p.length < 10) {
            return ProtocolManager.error("CREATE", "MISSING_FIELDS|min>=10 got=" + (p == null ? 0 : p.length));
        }

        String nombre      = p[1];
        String apellido    = p[2];
        String docId       = p[3];
        String edadStr     = p[4];
        String sexo        = p[5];
        String email       = p[6];
        String notas       = p[7];
        String fastaHeader = p[8]; // informativo
        String secuencia   = p[9];

        String sizeBytes = (p.length >= 11 ? p[10] : null);
        String hashAlgo  = (p.length >= 12 ? p[11] : null);
        String checksum  = (p.length >= 13 ? p[12] : null);
        String base64    = (p.length >= 14 ? p[13] : null);

        try {
            // 1) Validaciones de datos del paciente
            int edad = validadorPaciente.validarBasicos(nombre, apellido, docId, edadStr, sexo, email);

            // 2) Cargar propiedades y validar que existan TODAS las rutas necesarias
            GestorPropiedades cfg = new GestorPropiedades();
            String pathData           = cfg.getProperty("PATH_DATA");              // ./data
            String pathCsvPacientes   = cfg.getProperty("PATH_CSV_PACIENTES");     // ./data/pacientes.csv
            String pathFastaStorage   = cfg.getProperty("PATH_FASTA_STORAGE");     // ./data/fasta
            String pathEnfermedades   = cfg.getProperty("PATH_ENFERMEDADES");      // ./enfermedades
            String pathCatalogo       = cfg.getProperty("PATH_CATALOGO_ENF");      // ./enfermedades/catalog.csv
            String pathCsvDetecciones = cfg.getProperty("PATH_CSV_DETECCIONES");   // ./data/detecciones.csv

            // Logs de diagnóstico (opcional)
            System.out.println("[WD] " + System.getProperty("user.dir"));
            System.out.println("[CFG] PATH_DATA=" + pathData);
            System.out.println("[CFG] PATH_CSV_PACIENTES=" + pathCsvPacientes);
            System.out.println("[CFG] PATH_CSV_DETECCIONES=" + pathCsvDetecciones);
            System.out.println("[CFG] PATH_FASTA_STORAGE=" + pathFastaStorage);
            System.out.println("[CFG] PATH_ENFERMEDADES=" + pathEnfermedades);
            System.out.println("[CFG] PATH_CATALOGO_ENF=" + pathCatalogo);

            if (isBlank(pathData) || isBlank(pathCsvPacientes) || isBlank(pathFastaStorage)
                    || isBlank(pathEnfermedades) || isBlank(pathCatalogo) || isBlank(pathCsvDetecciones)) {
                return ProtocolManager.error("CREATE",
                        "MISSING_CONFIG|Revisa configuration.properties (PATH_DATA, PATH_CSV_PACIENTES, " +
                                "PATH_CSV_DETECCIONES, PATH_FASTA_STORAGE, PATH_ENFERMEDADES, PATH_CATALOGO_ENF)");
            }

            // 3) CSV (duplicados por document_id)
            GestorCSV repo = new GestorCSV(pathCsvPacientes);
            if (repo.existePorDocumento(docId)) {
                return ProtocolManager.error("CREATE", "DUPLICATE_DOCUMENT_ID|" + docId);
            }

            // 4) patient_id oficial (thread-safe)
            String patientId = IdGenerator.nextPatientId(pathData);

            // 5) Guardar paciente en CSV (con checksum/size si vienen)
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

            // 6) Guardar FASTA del paciente
            FastaStorage storage = new FastaStorage(pathFastaStorage);
            if (nonEmpty(base64)) {
                storage.guardarExactoDesdeBase64(patientId, base64);
            } else {
                // si secuencia viene vacía, mejor fallar explícito
                if (isBlank(secuencia)) {
                    return ProtocolManager.error("CREATE", "MISSING_SEQUENCE|No se recibió secuencia (p[9]) ni BASE64 (p[13])");
                }
                storage.guardarLegibleDesdeSecuencia(patientId, secuencia);
            }

            // 7) Cargar catálogo (severidad por id) + FASTA enfermedades (secuencias)
            CargadorCatalogo cat = new CargadorCatalogo(pathCatalogo);
            Map<String,Integer> sevPorId = cat.severidad();    // id -> severity (1..10)

            CargadorEnfermedades loader = new CargadorEnfermedades(pathEnfermedades);
            List<Enfermedad> catalogo = loader.cargar();

            // 8) Detectar enfermedades
            // Si guardaste exacto por BASE64, puedes volver a leer secuencia desde el archivo si quieres;
            // por simplicidad usamos 'secuencia' recibida (modo simple).
            List<EnfermedadDetector.Deteccion> hits =
                    EnfermedadDetector.detectar(secuencia, catalogo);

            // 9) Registrar detecciones y armar notificación
            String detectionsOut;
            if (!hits.isEmpty()) {
                DeteccionesCSV rep = new DeteccionesCSV(pathCsvDetecciones);

                detectionsOut = hits.stream().map(d -> {
                    int sev = sevPorId.getOrDefault(d.diseaseId, 0);
                    rep.append(patientId, d.diseaseId, sev, d.description);
                    return d.diseaseId + "(" + sev + ")";
                }).collect(Collectors.joining(";"));

                // Respuesta con notificación inmediata
                return ProtocolManager.ok("CREATE",  "CREATED|" + patientId + "|" + nombre + " " + apellido + "|" + docId + "|DETECTIONS=" + detectionsOut);
            } else {
                return ProtocolManager.ok("CREATE", "CREATED|" + patientId + "|" + nombre + " " + apellido + "|" + docId + "|DETECTIONS=NONE");
            }

        } catch (IllegalArgumentException ex) {
            // validaciones de entrada
            return ProtocolManager.error("CREATE", ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace(); // diagnóstico temporal
            String msg = ex.getClass().getSimpleName() + (ex.getMessage() != null ? (":" + ex.getMessage()) : "");
            return ProtocolManager.error("CREATE", "INTERNAL_ERROR|" + msg);
        }
    }

    // Helpers locales
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private static boolean nonEmpty(String s) {
        return s != null && !s.isBlank();
    }
}