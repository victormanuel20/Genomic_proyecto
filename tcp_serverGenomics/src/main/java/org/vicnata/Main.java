package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.helpers.GestorPropiedades;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        // 1. Configurar SSL
        ConfigSSL.configurar();

        // 2. Cargar propiedades
        GestorPropiedades gestor = new GestorPropiedades();
        int port = Integer.parseInt(gestor.getProperty("SERVER_PORT"));
        System.out.println("Server port: " + port);

        // 3. Iniciar servidor
        //TCPServer server = new TCPServer(port);
        //server.start();


    }
}