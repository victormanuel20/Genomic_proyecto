package org.vicnata.modelosDTO;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArchivoFastaDTO {
    private String idPaciente;        // para asociar, si lo decides así
    private String nombreArchivo;     // ej. "sarscov2.fasta"
    private long tamanoBytes;         // tamaño del archivo
    private String algoritmoHash;     // "MD5" o "SHA-256"
    private String checksum;          // hash calculado
    private String contenidoBase64;   // contenido del archivo en Base64
    private String ruta;
}
