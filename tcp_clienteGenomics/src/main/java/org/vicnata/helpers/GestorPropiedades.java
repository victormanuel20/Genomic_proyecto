package org.vicnata.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class GestorPropiedades {
    private final Properties props = new Properties();

    public GestorPropiedades(String rutaArchivo) {
        try (FileInputStream fis = new FileInputStream(rutaArchivo)) {
            props.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar el archivo de propiedades: " + rutaArchivo, e);
        }
    }

    public String getProperty(String clave) {
        return props.getProperty(clave);
    }
}
