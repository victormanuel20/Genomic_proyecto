package org.vicnata.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class IdGenerator {

    /** Genera IDs como P-0001, P-0002... usando un archivito en PATH_DATA/seq_patient.txt */
    public static synchronized String nextPatientId(String pathData) {
        try {
            Path dir = Paths.get(pathData == null ? "./data" : pathData);
            Files.createDirectories(dir);
            Path seqFile = dir.resolve("seq_patient.txt");

            int current = 0;
            if (Files.exists(seqFile)) {
                String s = Files.readString(seqFile, StandardCharsets.UTF_8).trim();
                if (!s.isEmpty()) current = Integer.parseInt(s);
            }
            int next = current + 1;
            Files.writeString(seqFile, String.valueOf(next), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return String.format("P-%04d", next);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar patient_id", e);
        }
    }

}
