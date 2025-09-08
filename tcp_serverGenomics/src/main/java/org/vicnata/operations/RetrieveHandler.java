package org.vicnata.operations;

import org.vicnata.red.ProtocolManager;

public class RetrieveHandler implements OperationHandler {
    @Override
    public String handle(String[] p) {
        if (p.length < 2) return ProtocolManager.error("RETRIEVE", "MISSING_PATIENT_ID");
        return ProtocolManager.ok("RETRIEVE", "PENDING|" + p[1]);
    }
}
