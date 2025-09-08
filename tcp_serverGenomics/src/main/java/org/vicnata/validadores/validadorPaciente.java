package org.vicnata.validadores;

public class validadorPaciente {

    /** Lanza IllegalArgumentException si algo está mal. Devuelve edad parseada. */
    public static int validarBasicos(String nombre, String apellido, String docId, String edadStr, String sexo, String email) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Nombre vacío");
        if (apellido == null || apellido.isBlank()) throw new IllegalArgumentException("Apellido vacío");
        if (docId == null || docId.isBlank()) throw new IllegalArgumentException("Documento vacío");

        int edad;
        try {
            edad = Integer.parseInt(edadStr.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Edad inválida");
        }
        if (edad <= 0 || edad > 120) throw new IllegalArgumentException("Edad fuera de rango");

        if (sexo == null || !(sexo.equalsIgnoreCase("M") || sexo.equalsIgnoreCase("F"))) {
            throw new IllegalArgumentException("Sexo inválido (M/F)");
        }

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Email inválido");
        }
        return edad;
    }

}
