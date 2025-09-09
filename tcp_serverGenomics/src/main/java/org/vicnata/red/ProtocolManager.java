// Paquete donde está la clase que maneja el protocolo de mensajes
package org.vicnata.red;

// Clase que define cómo se interpretan y construyen los mensajes enviados por socket
public class ProtocolManager {

    // Separador de entrada: doble barra invertida (\\), usado para dividir el mensaje recibido
    private static final String SEP_IN_REGEX = "\\\\";

    // Separador de salida: una barra vertical (|), usado para construir respuestas
    private static final String SEP_OUT = "|";

    // Método que divide un mensaje recibido en partes usando el separador de entrada
    public static String[] split(String wire) {
        // Si el mensaje está vacío o nulo, devuelve un arreglo vacío
        if (wire == null || wire.isBlank()) return new String[0];

        // Divide el mensaje usando el separador definido (\\)
        return wire.split(SEP_IN_REGEX);
    }

    // Método que construye una respuesta exitosa con formato: OK|codigo|mensaje
    public static String ok(String code, String msg) {
        return "OK" + SEP_OUT + code +
                (msg != null && !msg.isBlank() ? SEP_OUT + msg : "");
    }

    // Método que construye una respuesta de error con formato: ERROR|codigo|mensaje
    public static String error(String code, String msg) {
        return "ERROR" + SEP_OUT + code +
                (msg != null && !msg.isBlank() ? SEP_OUT + msg : "");
    }
}
