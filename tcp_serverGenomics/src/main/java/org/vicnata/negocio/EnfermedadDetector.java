package org.vicnata.negocio;

import java.util.*;

/**
 * Detector de enfermedades en la secuencia de un paciente.
 *
 * Estrategias:
 *  - BÁSICA: detección exacta con contains (100% match).
 *  - EXTENDIDA: detección parcial con porcentaje de similitud usando ventana deslizante.
 */
public class EnfermedadDetector {

    /**
     * Representa una detección encontrada en el paciente.
     */
    public static class Deteccion {
        public final String diseaseId;   // ID de la enfermedad (ej: D-001)
        public final String description; // Descripción del match (ej: "Matched 5/5 bases (100%)")

        public Deteccion(String diseaseId, String description) {
            this.diseaseId = diseaseId;
            this.description = description;
        }
    }

    /**
     * Detecta enfermedades en la secuencia del paciente.
     *
     * @param secuenciaPaciente Secuencia completa del paciente (ej: ACTTAACCTGTTAAG).
     * @param catalogo Lista de enfermedades (id + secuencia de referencia).
     * @return Lista de detecciones encontradas (puede estar vacía).
     */
    public static List<Deteccion> detectar(String secuenciaPaciente, List<Enfermedad> catalogo) {
        if (secuenciaPaciente == null) return List.of();

        String seq = secuenciaPaciente.toUpperCase();
        List<Deteccion> out = new ArrayList<>();

        for (Enfermedad enf : catalogo) {
            String ref = (enf.getSecuencia() != null) ? enf.getSecuencia().toUpperCase() : "";
            if (ref.isBlank()) continue;

            // --- 1) MATCH EXACTO ---
            if (seq.contains(ref)) {
                String desc = String.format("Matched %d/%d bases (100%%)", ref.length(), ref.length());
                out.add(new Deteccion(enf.getId(), desc));
                continue; // ya no buscamos coincidencias parciales
            }

            // --- 2) MATCH PARCIAL ---
            int bestMatch = bestPartialMatch(seq, ref);
            int porcentaje = (int) ((bestMatch * 100.0) / ref.length());

            // Solo registramos si hay al menos 70% de similitud (umbral configurable)
            if (porcentaje >= 70) {
                String desc = String.format("Matched %d/%d bases (%d%%)", bestMatch, ref.length(), porcentaje);
                out.add(new Deteccion(enf.getId(), desc));
            }
        }

        return out;
    }

    /**
     * Calcula el mejor match parcial usando ventana deslizante.
     * Compara subsecuencias del paciente contra la referencia.
     *
     * @param paciente Secuencia del paciente.
     * @param ref Secuencia de la enfermedad (referencia).
     * @return Número máximo de bases coincidentes.
     */
    private static int bestPartialMatch(String paciente, String ref) {
        int maxMatch = 0;
        int refLen = ref.length();

        // Ventana deslizante: recorre subsecuencias de longitud refLen
        for (int i = 0; i <= paciente.length() - refLen; i++) {
            String sub = paciente.substring(i, i + refLen);

            int matches = 0;
            for (int j = 0; j < refLen; j++) {
                if (sub.charAt(j) == ref.charAt(j)) matches++;
            }

            maxMatch = Math.max(maxMatch, matches);
            if (maxMatch == refLen) return maxMatch; // coincidencia exacta encontrada
        }

        return maxMatch;
    }
}
