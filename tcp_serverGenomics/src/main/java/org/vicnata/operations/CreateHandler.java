package org.vicnata.operations;

import org.vicnata.red.ProtocolManager;

public class CreateHandler implements OperationHandler {
    /**
     * Espera mínimo:
     * 0: CREATE
     * 1: NOMBRE
     * 2: APELLIDO
     * 3: DOC_ID
     * 4: EDAD
     * 5: SEXO
     * 6: EMAIL
     * 7: NOTAS
     * 8: FASTA_ID
     * 9: SECUENCIA
     */
    @Override
    public String handle(String[] p) {
        if (p.length < 10) {
            return ProtocolManager.error("CREATE", "MISSING_FIELDS|min>=10 got=" + p.length);
        }
        String nombre = p[1], apellido = p[2], docId = p[3];
        String fastaId = p[8];
        System.out.printf("[CREATE] %s %s (%s) FASTA=%s%n", nombre, apellido, docId, fastaId);
        return ProtocolManager.ok("CREATE", "RECEIVED|" + fastaId);
    }
}
