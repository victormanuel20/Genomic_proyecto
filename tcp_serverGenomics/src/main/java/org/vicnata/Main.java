package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.red.TCPServer;
import org.vicnata.red.TCPServer1;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {

        // 1. Configurar SSL
        ConfigSSL.configurar();

        // 2. Cargar propiedades
        GestorPropiedades gestor = new GestorPropiedades();
        int port = Integer.parseInt(gestor.getProperty("SERVER_PORT"));
        System.out.println("SERVER_PORT: " + port);


        // 3. Iniciar servidor
        //TCPServer1 server = new TCPServer1(2020);
        //server.start();

        new TCPServer(port).start();

    }
}