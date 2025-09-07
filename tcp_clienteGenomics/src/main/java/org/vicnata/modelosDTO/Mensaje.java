package org.vicnata.modelosDTO;

import org.vicnata.enums.Operacion; // tu enum con CREATE, RETRIEVE, etc.
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mensaje {
    private Operacion operacion; // tipo de acción
    private String payload;      // contenido de esa acción
}

