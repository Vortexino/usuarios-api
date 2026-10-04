package com.uni.api.application.port.out;

public interface FileStoragePort {
    String guardar(String nombreOriginal, byte[] contenido);
}