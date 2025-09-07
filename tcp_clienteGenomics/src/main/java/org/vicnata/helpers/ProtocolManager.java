package org.vicnata.helpers;

import org.vicnata.enums.Operacion;
import org.vicnata.modelosDTO.ArchivoFastaDTO;
import org.vicnata.modelosDTO.Mensaje;

import java.util.Map;

/**
 * ProtocolManager (cliente) – estilo profesor.
 *
 * - buildMessage(...) con switch por operación.
 * - buildCreatePersonMessage(...) concatena un String plano con "\" siguiendo
 *   un orden fijo de campos (¡el orden importa en el protocolo!).
 * - Usa ManejadorFasta.leerFasta(...) para armar el bloque del archivo.
 *
 * Formato propuesto para CREATE:
 *   CREATE\NOMBRE\APELLIDO\ID\EDAD\SEXO\CONTACT_EMAIL\CLINICAL_NOTES
 *         \NOMBRE_ARCHIVO\TAMANO_BYTES\HASH_ALGO\CHECKSUM\CONTENIDO_BASE64
 *
 * NOTA: Si más adelante tu profe exige otro orden, solo cambia el array 'CAMPOS_CREATE'
 *       o el orden de los campos del bloque FASTA.
 */
public class ProtocolManager {

    // Separador de campos y marcador de vacío
    private static final String SEP = "\\";
    private static final String NIL = "-";

    // Algoritmo por defecto para el checksum del FASTA (puedes leerlo luego de properties)
    private static final String HASH_DEF = "SHA-256";

    /**
     * Método general: construye mensajes dependiendo de la operación.
     * Aquí dejamos listos los casos; hoy nos enfocamos en CREATE.
     */
    public Mensaje buildMessage(Operacion operacion, Map<String, String> payload, String fastaPath) {
        switch (operacion) {
            case CREATE:
                return this.buildCreatePersonMessage(payload, fastaPath);
            case UPDATE:
                // TODO: implementar (metadata o con FASTA)
                break;
            case RETRIEVE:
                // TODO: implementar (ej. "RETRIEVE\patientId")
                break;
            case DELETE:
                // TODO: implementar (ej. "DELETE\patientId")
                break;
            default:
                System.out.println("Operación no soportada: " + operacion);
        }
        return null;
    }

    /**
     * Construye el mensaje CREATE concatenando los campos en el orden definido.
     * - Primero agrega la palabra de la operación ("CREATE")
     * - Luego agrega los campos de metadata del paciente en el orden del array
     * - Finalmente añade los datos del archivo FASTA obtenidos del ManejadorFasta
     */
    private Mensaje buildCreatePersonMessage(Map<String, String> payload, String fastaPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(Operacion.CREATE.name()); // "CREATE" al inicio

        // 1) Campos de METADATA en orden fijo (evitar forEach sobre el Map sin orden)
        final String[] CAMPOS_CREATE = {
                "NOMBRE", "APELLIDO", "ID", "EDAD", "SEXO", "CONTACT_EMAIL", "CLINICAL_NOTES"
        };
        for (String clave : CAMPOS_CREATE) {
            sb.append(SEP).append(val(payload.get(clave)));
        }

        // 2) Bloque FASTA usando tu ManejadorFasta y tu ArchivoFastaDTO
        //    Tu DTO tiene: idPaciente, nombreArchivo, tamanoBytes, algoritmoHash, checksum, contenidoBase64
        ArchivoFastaDTO fasta = ManejadorFasta.leerFasta(fastaPath, HASH_DEF);

        /*
        sb.append(SEP).append(val(fasta.getIdPaciente()));
        sb.append(SEP).append(val(fasta.getNombreArchivo()));       // nombre del archivo .fasta
        sb.append(SEP).append(String.valueOf(fasta.getTamanoBytes())); // tamaño bytes
        sb.append(SEP).append(val(fasta.getAlgoritmoHash()));       // "SHA-256" o "MD5"
        sb.append(SEP).append(val(fasta.getChecksum()));            // checksum en HEX
        sb.append(SEP).append(val(fasta.getContenidoBase64()));     // contenido en Base64 (bytes exactos)
         */

        // === Parte FASTA ===
        String headerId = org.vicnata.helpers.ManejadorFasta.extraerHeaderId(fastaPath);
        String secuencia = org.vicnata.helpers.ManejadorFasta.extraerSoloSecuencia(fastaPath);

        sb.append(SEP).append(val(headerId));   // ID del header '>'
        sb.append(SEP).append(val(secuencia));  // secuencia concatenada
        sb.append(SEP).append(String.valueOf(fasta.getTamanoBytes())); // obligatorio
        sb.append(SEP).append(val(fasta.getAlgoritmoHash()));          // obligatorio
        sb.append(SEP).append(val(fasta.getChecksum()));               // obligatorio
        sb.append(SEP).append(val(fasta.getContenidoBase64()));        // opcional (pero recomendado)

        // Devuelve el Mensaje como en el ejemplo del profe (acción + payload concatenado)
        return new Mensaje(Operacion.CREATE, sb.toString());
    }

    // ----------------- Helpers pequeños -----------------

    /** Convierte null/vacíos en marcador NIL para que el protocolo no se rompa. */
    private static String val(String s) {
        return (s == null || s.isBlank()) ? NIL : s.trim();
    }
}

