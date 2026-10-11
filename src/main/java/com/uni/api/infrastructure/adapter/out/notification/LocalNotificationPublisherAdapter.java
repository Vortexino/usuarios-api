package com.uni.api.infrastructure.adapter.out.notification;

import com.uni.api.application.port.out.NotificationPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!lambda")
public class LocalNotificationPublisherAdapter implements NotificationPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LocalNotificationPublisherAdapter.class);

    @Override
    public void publicar(String email, String asunto, String mensaje) {
        log.info("[SIMULADO] Notificación a {} | asunto: {} | mensaje: {}", email, asunto, mensaje);
    }
}