package org.vicnata.operations;

import org.vicnata.red.ProtocolManager;


public class DeleteHandler implements OperationHandler {
    @Override
    public String handle(String[] p) {
        if (p.length < 2) return ProtocolManager.error("DELETE", "MISSING_PATIENT_ID");
        return ProtocolManager.ok("DELETE", "PENDING|" + p[1]);
    }
}
