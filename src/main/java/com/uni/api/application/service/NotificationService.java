package com.uni.api.application.service;

import com.uni.api.application.port.in.NotificationUseCase;
import com.uni.api.application.port.out.NotificationPublisherPort;

public class NotificationService implements NotificationUseCase {

    private final NotificationPublisherPort publisher;

    public NotificationService(NotificationPublisherPort publisher) {
        this.publisher = publisher;
    }

    @Override
    public void enviar(String email, String asunto, String mensaje) {
        publisher.publicar(email, asunto, mensaje);
    }
}