package org.vicnata.negocio;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CargadorEnfermedades {
    private final Path dir;
    private List<Enfermedad> cache;

    public CargadorEnfermedades(String rutaDir) {
        this.dir = Paths.get(rutaDir);
    }

    public synchronized List<Enfermedad> cargar() {
        if (cache != null) return cache;
        try {
            if (!Files.exists(dir)) {
                cache = List.of();
                return cache;
            }
            List<Enfermedad> lista = new ArrayList<>();
            try (var paths = Files.list(dir)) {
                for (Path p : paths.collect(Collectors.toList())) {
                    if (!Files.isRegularFile(p) || !p.getFileName().toString().toLowerCase().endsWith(".fasta")) continue;

                    var lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                    if (lines.size() < 2) continue;

                    String linea1 = lines.get(0).trim();
                    if (linea1.startsWith(">")) linea1 = linea1.substring(1).trim(); // soporta '>Nombre'

                    String nombre   = linea1;
                    String secuencia= lines.get(1).trim().toUpperCase();

                    String fileName = p.getFileName().toString();
                    String id = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

                    lista.add(new Enfermedad(id, nombre, secuencia));
                }
            }
            cache = Collections.unmodifiableList(lista);
            System.out.println("[CATALOGO] Enfermedades cargadas: " + cache.size());
            return cache;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo cargar FASTA de enfermedades", e);
        }
    }
}
