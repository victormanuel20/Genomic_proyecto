package org.vicnata.config;

import org.vicnata.helpers.GestorPropiedades;

public class ConfigSSL {
    public static void configurar(String rutaProperties) {
        GestorPropiedades gestor = new GestorPropiedades(rutaProperties);

        String certificateRoute = gestor.getProperty("SSL_CERTIFICATE_ROUTE");
        String certificatePassword = gestor.getProperty("SSL_PASSWORD");

        System.setProperty("javax.net.ssl.keyStore", certificateRoute);
        System.setProperty("javax.net.ssl.keyStorePassword", certificatePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

        System.setProperty("javax.net.ssl.trustStore", certificateRoute);
        System.setProperty("javax.net.ssl.trustStorePassword", certificatePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

        System.out.println("SSL configurado correctamente ✅");
    }
}
