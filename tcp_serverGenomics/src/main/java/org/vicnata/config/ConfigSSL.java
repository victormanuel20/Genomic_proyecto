package org.vicnata.config;
import org.vicnata.helpers.GestorPropiedades;

public class ConfigSSL {

    public static void configurar() {
        // Cargamos el archivo de configuración
        GestorPropiedades gestor = new GestorPropiedades();

        String certificateRoute = gestor.getProperty("SSL_CERTIFICATE_ROUTE");
        String certificatePassword = gestor.getProperty("SSL_PASSWORD");

        // Configuración de keystore
        System.setProperty("javax.net.ssl.keyStore", certificateRoute);
        System.setProperty("javax.net.ssl.keyStorePassword", certificatePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

        // Configuración de truststore
        System.setProperty("javax.net.ssl.trustStore", certificateRoute);
        System.setProperty("javax.net.ssl.trustStorePassword", certificatePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

        System.out.println("[SSL] Configuración aplicada correctamente ✅");
    }

}
