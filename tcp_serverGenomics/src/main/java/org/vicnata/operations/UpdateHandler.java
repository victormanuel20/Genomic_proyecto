package org.vicnata.operations;

import org.vicnata.red.ProtocolManager;

public class UpdateHandler implements OperationHandler {
    @Override
    public String handle(String[] p) {
        return ProtocolManager.ok("UPDATE", "PENDING");
    }
}
