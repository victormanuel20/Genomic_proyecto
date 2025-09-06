package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.helpers.GestorPropiedades;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

// TODO: cargar ConfigSSL, iniciar MenuCliente
public class Main {
    public static void main(String[] args) {

    ConfigSSL.configurar("src/main/java/org/vicnata/configuration.properties");

    }
}