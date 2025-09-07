package org.vicnata.helpers;


// TODO: leer archivo .fasta local, devolver secuencia como String/Base64
import org.vicnata.modelosDTO.ArchivoFastaDTO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/** Lee un .fasta, calcula tamaño y checksum, y arma el DTO listo para enviar. */
public class ManejadorFasta {

    /** Lee el archivo FASTA completo y arma el DTO. algoritmoHash: "SHA-256" o "MD5". */
    public static ArchivoFastaDTO leerFasta(String ruta, String idPaciente, String algoritmoHash) {
        try {
            Path p = Path.of(ruta);
            byte[] bytes = Files.readAllBytes(p);

            String checksum = hashHex(bytes, algoritmoHash);
            long size = Files.size(p);
            String nombreArchivo = p.getFileName().toString();
            String contenidoBase64 = Base64.getEncoder().encodeToString(bytes);

            return new ArchivoFastaDTO(
                    idPaciente,
                    nombreArchivo,
                    size,
                    algoritmoHash,
                    checksum,
                    contenidoBase64
            );
        } catch (Exception e) {
            throw new RuntimeException("Error leyendo FASTA: " + ruta, e);
        }
    }

    /** Variante si no quieres idPaciente todavía (por ejemplo en CREATE). */
    public static ArchivoFastaDTO leerFasta(String ruta, String algoritmoHash) {
        return leerFasta(ruta, null, algoritmoHash);
    }

    /** Extrae solo la secuencia (omite líneas que empiezan con '>'). Útil si quieres comparar secuencias. */
    public static String extraerSoloSecuencia(String ruta) {
        try {
            List<String> lines = Files.readAllLines(Path.of(ruta));
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (!line.startsWith(">")) sb.append(line.trim());
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo procesar FASTA (secuencia): " + ruta, e);
        }
    }

    /** Calcula hash y lo devuelve en HEX. */
    private static String hashHex(byte[] data, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Algoritmo de hash no soportado: " + algorithm, e);
        }
    }

    /** Escribe el FASTA en disco.
     *  incluirCabecera=false → escribe los MISMOS bytes (ideal para integridad).
     *  incluirCabecera=true  → genera FASTA legible (header '>' y secuencia envuelta).
     */
    public static void escribirFasta(ArchivoFastaDTO dto, String rutaSalida, boolean incluirCabecera) {
        try {
            String limpia = rutaSalida.trim().replace("\"","").replace("'","");
            Path out = Path.of(limpia);
            if (out.getParent() != null) Files.createDirectories(out.getParent());

            byte[] bytes = Base64.getDecoder().decode(dto.getContenidoBase64());

            if (!incluirCabecera) {
                // Escritura EXACTA (mismos bytes que se leyeron) → checksum coincide
                Files.write(out, bytes);
                return;
            }

            // Escritura “legible”: header y solo secuencia sin líneas '>'
            String contenido = new String(bytes); // texto FASTA
            StringBuilder sbSeq = new StringBuilder();
            for (String line : contenido.split("\\R")) {
                if (!line.startsWith(">")) sbSeq.append(line.trim());
            }
            String seq = sbSeq.toString();

            String header = dto.getIdPaciente() != null
                    ? (">" + dto.getIdPaciente())
                    : (">" + (dto.getNombreArchivo() != null ? dto.getNombreArchivo() : "genome"));

            int wrap = 60; // 60 chars por línea
            StringBuilder fastaLegible = new StringBuilder();
            fastaLegible.append(header).append("\n");
            for (int i = 0; i < seq.length(); i += wrap) {
                fastaLegible.append(seq, i, Math.min(i + wrap, seq.length())).append("\n");
            }

            Files.writeString(out, fastaLegible.toString());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo escribir FASTA en " + rutaSalida, e);
        }
    }

}
