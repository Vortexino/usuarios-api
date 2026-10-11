package com.uni.api.application.port.out;

public interface NotificationPublisherPort {
    void publicar(String email, String asunto, String mensaje);
}