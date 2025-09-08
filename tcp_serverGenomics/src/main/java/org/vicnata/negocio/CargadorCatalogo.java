package org.vicnata.negocio;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class CargadorCatalogo {
    private final Path csv;
    private Map<String,Integer> severidadPorId;
    private Map<String,String>  nombrePorId;

    public CargadorCatalogo(String rutaCsv) {
        this.csv = Paths.get(rutaCsv);
    }

    public synchronized Map<String,Integer> severidad() {
        if (severidadPorId != null) return severidadPorId;
        cargar();
        return severidadPorId;
    }

    public synchronized Map<String,String> nombres() {
        if (nombrePorId != null) return nombrePorId;
        cargar();
        return nombrePorId;
    }

    private void cargar() {
        Map<String,Integer> sev = new HashMap<>();
        Map<String,String> nom = new HashMap<>();
        if (!Files.exists(csv)) {
            severidadPorId = Collections.unmodifiableMap(sev);
            nombrePorId    = Collections.unmodifiableMap(nom);
            return;
        }
        try (Stream<String> lines = Files.lines(csv, StandardCharsets.UTF_8)) {
            lines.skip(1).forEach(line -> {
                String[] c = line.split(",", -1);
                if (c.length < 3) return;
                String id   = c[0].trim();
                String name = c[1].trim();
                int sevInt  = 0;
                try { sevInt = Integer.parseInt(c[2].trim()); } catch (Exception ignored) {}
                if (!id.isEmpty()) {
                    sev.put(id, sevInt);
                    nom.put(id, name);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("No se pudo leer catalog.csv", e);
        }
        severidadPorId = Collections.unmodifiableMap(sev);
        nombrePorId    = Collections.unmodifiableMap(nom);
    }
}
