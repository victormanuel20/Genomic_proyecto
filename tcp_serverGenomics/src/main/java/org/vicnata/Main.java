package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.red.TCPServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        // 1. Configurar SSL
        ConfigSSL.configurar();

        // 2. Cargar propiedades
        GestorPropiedades gestor = new GestorPropiedades();
        int port = Integer.parseInt(gestor.getProperty("SSL_PASSWORD"));
        System.out.println("SSL_PASSWORD: " + port);


        // 3. Iniciar servidor
        TCPServer server = new TCPServer(2020);
        server.start();


    }
}