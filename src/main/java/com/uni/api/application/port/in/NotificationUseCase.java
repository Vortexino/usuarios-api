package com.uni.api.application.port.in;

public interface NotificationUseCase {
    void enviar(String email, String asunto, String mensaje);
}