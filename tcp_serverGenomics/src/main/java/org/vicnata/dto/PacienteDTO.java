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
    private String patientId;
    private String nombre;
    private String apellido;
    private String documentId;
    private int edad;
    private String sexo;           // "M" / "F"
    private String contactEmail;
    private String clinicalNotes;
    private boolean active = true;

    public String getFullName() {
        return (nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido);
    }
}

