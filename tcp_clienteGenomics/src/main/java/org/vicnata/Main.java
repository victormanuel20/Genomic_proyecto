package org.vicnata;

import org.vicnata.config.ConfigSSL;
import org.vicnata.enums.Operacion;
import org.vicnata.helpers.GestorPropiedades;
import org.vicnata.helpers.ManejadorFasta;
import org.vicnata.helpers.ProtocolManager;
import org.vicnata.modelosDTO.ArchivoFastaDTO;
import org.vicnata.modelosDTO.Mensaje;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import org.vicnata.MenuCliente.MenuCliente;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

// TODO: cargar ConfigSSL, iniciar MenuCliente
public class Main {
    public static void main(String[] args) {



        //"C:/Users/User/Desktop/paciente1.fasta";
        ConfigSSL.configurar();
        MenuCliente menuCliente = new MenuCliente();
        menuCliente.mostrar();


    }
}