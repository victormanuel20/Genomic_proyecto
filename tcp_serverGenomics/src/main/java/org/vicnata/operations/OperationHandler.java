package org.vicnata.operations;


/**
 * Interfaz base para operaciones del protocolo (CREATE, RETRIEVE, UPDATE, DELETE).
 * Cada implementación debe definir cómo manejar los parámetros recibidos desde el cliente TCP.
 *
 * Método:
 *   - handle(String[] parts): procesa la operación según los campos recibidos y retorna la respuesta en formato de protocolo.
 */

public interface OperationHandler {
    String handle(String[] parts);
}
