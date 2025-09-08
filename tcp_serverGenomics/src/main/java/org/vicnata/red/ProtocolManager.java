package org.vicnata.red;

public class ProtocolManager {
    private static final String SEP_IN_REGEX = "\\\\";
    private static final String SEP_OUT      = "|";

    public static String[] split(String wire) {
        if (wire == null || wire.isBlank()) return new String[0];
        return wire.split(SEP_IN_REGEX);
    }

    public static String ok(String code, String msg) {
        return "OK" + SEP_OUT + code + (msg != null && !msg.isBlank() ? SEP_OUT + msg : "");
    }

    public static String error(String code, String msg) {
        return "ERROR" + SEP_OUT + code + (msg != null && !msg.isBlank() ? SEP_OUT + msg : "");
    }
}
