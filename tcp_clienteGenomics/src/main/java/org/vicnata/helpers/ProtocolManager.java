package org.vicnata.helpers;

import org.vicnata.enums.Operacion;
import org.vicnata.modelosDTO.ArchivoFastaDTO;
import org.vicnata.modelosDTO.Mensaje;

import java.util.Map;

/**
 * ProtocolManager (cliente) – estilo profesor.
 *
 * Estructura:
 *  - buildMessage(...) con switch por operación.
 *  - buildCreatePersonMessage(...) concatena un String con "\" en orden fijo.
 *  - buildRetrieveMessage(...) arma "RETRIEVE\patient_id".
 *
 * Formato propuesto:
 *  CREATE\NOMBRE\APELLIDO\ID\EDAD\SEXO\CONTACT_EMAIL\CLINICAL_NOTES
 *        \FASTA_HEADER_ID\SECUENCIA\FILE_SIZE_BYTES\HASH_ALGO\CHECKSUM\BASE64
 *
 *  RETRIEVE\PATIENT_ID
 *
 *
 */
public class ProtocolManager {

    // Separador y marcador nulo
    private static final String SEP = "\\";
    private static final String NIL = "-";

    // Algoritmo por defecto de hash (puedes leerlo de properties si quieres)
    private static final String HASH_DEF = "SHA-256";

    // Claves esperadas en payload
    private static final String K_PATIENT_ID = "PATIENT_ID";

    /**
     * Router principal de construcción de mensajes.
     */
    public Mensaje buildMessage(Operacion operacion, Map<String, String> payload, String fastaPath) {
        switch (operacion) {
            case CREATE:
                return this.buildCreatePersonMessage(payload, fastaPath);

            case RETRIEVE:
                return this.buildRetrieveMessage(payload);

            case UPDATE:
                // TODO: implementar (ej. UPDATE\patientId\campos...)
                return null;

            case DELETE:
                // TODO: implementar (ej. DELETE\patientId)
                return this.buildDeleteMessage(payload);

            default:
                System.out.println("Operación no soportada: " + operacion);
                return null;
        }
    }

    /**
     * CREATE:
     *  - Primero metadata en orden fijo.
     *  - Luego bloque FASTA (header id, secuencia, tamaño, hash, checksum, base64).
     */
    private Mensaje buildCreatePersonMessage(Map<String, String> payload, String fastaPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(Operacion.CREATE.name()); // "CREATE"

        // 1) METADATA en orden fijo
        final String[] CAMPOS_CREATE = {
                "NOMBRE", "APELLIDO", "ID", "EDAD", "SEXO", "CONTACT_EMAIL", "CLINICAL_NOTES"
        };
        for (String clave : CAMPOS_CREATE) {
            sb.append(SEP).append(val(payload.get(clave)));
        }

        // 2) FASTA (usando tu ManejadorFasta)
        ArchivoFastaDTO fasta = ManejadorFasta.leerFasta(fastaPath, HASH_DEF);

        // HeaderId (>linea sin '>') y secuencia plana
        String headerId  = ManejadorFasta.extraerHeaderId(fastaPath);
        String secuencia = ManejadorFasta.extraerSoloSecuencia(fastaPath);

        sb.append(SEP).append(val(headerId));                          // 8
        sb.append(SEP).append(val(secuencia));                         // 9
        sb.append(SEP).append(String.valueOf(fasta.getTamanoBytes())); // 10
        sb.append(SEP).append(val(fasta.getAlgoritmoHash()));          // 11
        sb.append(SEP).append(val(fasta.getChecksum()));               // 12
        sb.append(SEP).append(val(fasta.getContenidoBase64()));        // 13

        // Devuelve como "acción + payload" (estilo profe)
        return new Mensaje(Operacion.CREATE, sb.toString());
    }

    /**
     * RETRIEVE:
     *  - Estructura simple: "RETRIEVE\PATIENT_ID"
     *  - Lee el patient_id desde payload[K_PATIENT_ID]
     */
    private Mensaje buildRetrieveMessage(Map<String, String> payload) {
        String patientId = payload != null ? payload.get(K_PATIENT_ID) : null;
        if (patientId == null || patientId.isBlank()) {
            // Si lo mandan vacío, lo marcamos con NIL para no romper el protocolo
            patientId = NIL;
        }
        String wire = Operacion.RETRIEVE.name() + SEP + patientId;
        return new Mensaje(Operacion.RETRIEVE, wire);
    }


    // --- método nuevo en ProtocolManager ---
    /** Formato: DELETE\PATIENT_ID */
    private Mensaje buildDeleteMessage(Map<String,String> payload) {
        String pid = payload.getOrDefault("PATIENT_ID", "-");
        String wire = Operacion.DELETE.name() + SEP + val(pid);
        return new Mensaje(Operacion.DELETE, wire);
    }

    // ----------------- Helpers -----------------

    /** Convierte null/vacíos en marcador NIL para no romper el protocolo. */
    private static String val(String s) {
        return (s == null || s.isBlank()) ? NIL : s.trim();
    }
}
