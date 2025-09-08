package org.vicnata.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UtilFechas {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    // ejemplo: 2025-09-07T23:22:31  (hora local sin “Z”)

    public static String ahoraIsoBogota() {
        return ZonedDateTime.now(BOGOTA).format(ISO_LOCAL);
    }
}
