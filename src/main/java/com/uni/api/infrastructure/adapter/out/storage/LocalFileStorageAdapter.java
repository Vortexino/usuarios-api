package com.uni.api.infrastructure.adapter.out.storage;

import com.uni.api.application.port.out.FileStoragePort;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path carpeta = Path.of("uploads");

    public LocalFileStorageAdapter() {
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de uploads", e);
        }
    }

    @Override
    public String guardar(String nombreOriginal, byte[] contenido) {
        String extension = "";
        int punto = nombreOriginal.lastIndexOf('.');
        if (punto >= 0) extension = nombreOriginal.substring(punto);

        String nombreUnico = UUID.randomUUID() + extension;
        try {
            Files.write(carpeta.resolve(nombreUnico), contenido);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el archivo", e);
        }
        return nombreUnico;
    }
}