package org.vicnata.modelosDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vicnata.enums.sexo;

@Data
@AllArgsConstructor
@NoArgsConstructor


//Se tiene la metada del paciente
public class PacienteDTO {

    private String nombre;
    private String apellido;
    private String id;
    private int edad;
    private sexo sexo;
    private String contactEmail;
    private String ClinicalNotes;
    private ArchivoFastaDTO genoma;
}
