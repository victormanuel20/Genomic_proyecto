package org.vicnata.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PacienteDTO {

    private String patientId;        // P-0001
    private String nombre;           // "victor"
    private String apellido;         // "garcia"
    private String documentId;       // "1002..."
    private int    edad;             // 20
    private String sexo;             // "M" / "F"
    private String contactEmail;     // "victor@mail..."
    private String clinicalNotes;    // "ninguna"

    // Campos que guardas en CSV
    private String registrationDate; // ISO (se escribe con UtilFechas.ahoraIsoBogota())
    private String checksumFasta;    // puede ser vacío si no vino
    private long   fileSizeBytes;    // puede ser 0 si no vino
    private boolean active = true;   // por defecto true
    public String getFullName() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }
}

