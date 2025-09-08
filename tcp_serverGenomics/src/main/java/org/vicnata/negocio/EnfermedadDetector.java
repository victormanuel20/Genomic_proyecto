package org.vicnata.negocio;

import java.util.*;

public class EnfermedadDetector {

    public static class Deteccion {
        public final String diseaseId;
        public final String description;
        public Deteccion(String diseaseId, String description) {
            this.diseaseId = diseaseId;
            this.description = description;
        }
    }

    /** match simple: si la referencia aparece dentro de la secuencia del paciente. */
    public static List<Deteccion> detectar(String secuenciaPaciente, List<Enfermedad> catalogo) {
        if (secuenciaPaciente == null) return List.of();
        String seq = secuenciaPaciente.toUpperCase();

        List<Deteccion> out = new ArrayList<>();
        for (Enfermedad enf : catalogo) {
            String ref = enf.getSecuencia();
            if (ref == null || ref.isBlank()) continue;

            if (seq.contains(ref)) {
                String desc = String.format("Matched %d/%d bases (100%%)", ref.length(), ref.length());
                out.add(new Deteccion(enf.getId(), desc));
            }
        }
        return out;
    }
}
