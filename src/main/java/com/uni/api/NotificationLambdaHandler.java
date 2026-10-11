package com.uni.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

public class NotificationLambdaHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SesClient SES = SesClient.builder()
            .region(Region.of(System.getenv("AWS_REGION")))
            .build();

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        String remitente = System.getenv("SES_SENDER_EMAIL");

        for (SQSEvent.SQSMessage msg : event.getRecords()) {
            try {
                JsonNode data = MAPPER.readTree(msg.getBody());
                String destinatario = data.get("email").asText();
                String asunto = data.get("subject").asText();
                String mensaje = data.get("message").asText();

                logger.log("Procesando notificación para " + destinatario);

                SES.sendEmail(SendEmailRequest.builder()
                        .source(remitente)
                        .destination(Destination.builder().toAddresses(destinatario).build())
                        .message(Message.builder()
                                .subject(Content.builder().data(asunto).build())
                                .body(Body.builder().text(Content.builder().data(mensaje).build()).build())
                                .build())
                        .build());

                logger.log("Correo enviado correctamente a " + destinatario);
            } catch (Exception e) {
                logger.log("ERROR procesando mensaje: " + e.getMessage());
                throw new RuntimeException("Fallo procesando mensaje SQS", e);
            }
        }
        return null;
    }
}