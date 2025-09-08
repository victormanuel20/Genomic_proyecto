package org.vicnata.negocio;

public class Enfermedad {

    private final String id;        // p.ej. "sarscov2"
    private final String nombre;    // p.ej. "SARS-CoV-2"
    private final String secuencia; // referencia ACGT...

    public Enfermedad(String id, String nombre, String secuencia) {
        this.id = id;
        this.nombre = nombre;
        this.secuencia = secuencia != null ? secuencia.toUpperCase() : "";
    }
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSecuencia() { return secuencia; }


}
