package org.vicnata.helpers;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;



import lombok.Data;
@Data

public class GestorPropiedades {
    private final Properties props = new Properties();

    // Ruta fija del archivo de propiedades
    private static final String CONFIG_FILE = "C:\\Users\\User\\Desktop\\Genomic_proyecto\\tcp_clienteGenomics\\configuration.properties";

    public GestorPropiedades() {
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            Logger.getLogger(GestorPropiedades.class.getName())
                    .log(Level.SEVERE, "No se pudo cargar el archivo de propiedades: " + CONFIG_FILE, e);
            throw new RuntimeException("No se pudo cargar el archivo de propiedades: " + CONFIG_FILE, e);
        }
    }

    public String getProperty(String clave) {
        return props.getProperty(clave);
    }
}
