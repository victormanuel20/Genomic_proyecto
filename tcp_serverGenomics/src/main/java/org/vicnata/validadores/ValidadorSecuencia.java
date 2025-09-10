package org.vicnata.validadores;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * Valida el contenido FASTA que llega por el protocolo:
 * - headerId: identificador del encabezado (sin '>')
 * - secuencia: A/C/G/T/N (mayúsculas)
 * - validación de integridad si llega BASE64 + size + hash + checksum
 */
public final class ValidadorSecuencia {

    private ValidadorSecuencia() {}

    /**
     * Valida header y secuencia.
     * Reglas:
     *  - headerId no vacío, máx 64, sólo letras/dígitos/_-. (sin '>')
     *  - secuencia no vacía, sólo A C G T N en mayúsculas, máx ~5e6 (por seguridad)
     */
    public static void validarHeaderYSecuencia(String headerId, String secuencia) {
        if (isBlank(headerId))
            throw new IllegalArgumentException("FASTA_HEADER_EMPTY");

        if (!headerId.matches("[A-Za-z0-9_.-]{1,64}"))
            throw new IllegalArgumentException("FASTA_HEADER_INVALID|Use [A-Za-z0-9_.-] (sin '>')");

        if (isBlank(secuencia))
            throw new IllegalArgumentException("FASTA_SEQUENCE_EMPTY");

        String seq = secuencia.trim().toUpperCase();
        if (!seq.matches("[ACGTN]+"))
            throw new IllegalArgumentException("FASTA_SEQUENCE_INVALID|Sólo A,C,G,T,N");

        int max = 5_000_000; // cota de seguridad opcional
        if (seq.length() > max)
            throw new IllegalArgumentException("FASTA_SEQUENCE_TOO_LARGE|" + seq.length());
    }

    /**
     * Valida integridad del archivo si llega todo el bloque exacto.
     * - base64 obligatorio
     * - sizeBytes coincide con bytes.length
     * - checksum == digest(bytes, hashAlgo)
     * hashAlgo permitido: MD5 o SHA-256
     */
    public static void validarTamanoYChecksum(String base64, String hashAlgo,
                                              String checksumEsperado, Long sizeBytes) {
        if (isBlank(base64))
            throw new IllegalArgumentException("FASTA_BASE64_EMPTY");

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("FASTA_BASE64_INVALID");
        }

        if (sizeBytes == null || sizeBytes <= 0)
            throw new IllegalArgumentException("FILE_SIZE_INVALID");

        if (bytes.length != sizeBytes)
            throw new IllegalArgumentException("FILE_SIZE_MISMATCH|" + bytes.length + "!=" + sizeBytes);

        if (isBlank(hashAlgo) || isBlank(checksumEsperado))
            throw new IllegalArgumentException("HASH_OR_CHECKSUM_MISSING");

        String algo = hashAlgo.trim().toUpperCase();
        if (!algo.equals("MD5") && !algo.equals("SHA-256"))
            throw new IllegalArgumentException("HASH_ALGO_UNSUPPORTED|" + hashAlgo);

        String calc = digestHex(bytes, algo);
        if (!calc.equalsIgnoreCase(checksumEsperado))
            throw new IllegalArgumentException("CHECKSUM_MISMATCH");
    }

    // ---------- helpers ----------
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static String digestHex(byte[] data, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("HASH_ALGO_ERROR|" + algorithm);
        }
    }
}
