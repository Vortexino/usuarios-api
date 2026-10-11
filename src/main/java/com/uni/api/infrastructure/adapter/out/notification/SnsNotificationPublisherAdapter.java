package com.uni.api.infrastructure.adapter.out.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.api.application.port.out.NotificationPublisherPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

@Component
@Profile("lambda")
public class SnsNotificationPublisherAdapter implements NotificationPublisherPort {

    private final SnsClient sns;
    private final String topicArn;
    private final ObjectMapper mapper = new ObjectMapper();

    public SnsNotificationPublisherAdapter(SnsClient sns,
                                           @Value("${app.sns.topic-arn}") String topicArn) {
        this.sns = sns;
        this.topicArn = topicArn;
    }

    @Override
    public void publicar(String email, String asunto, String mensaje) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "email", email,
                    "subject", asunto,
                    "message", mensaje
            ));
            sns.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .message(body)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo publicar la notificación en SNS", e);
        }
    }
}