package com.uni.api.infrastructure.adapter.in.web;

import com.uni.api.application.port.in.NotificationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationUseCase notifications;

    public NotificationController(NotificationUseCase notifications) {
        this.notifications = notifications;
    }

    public record EnviarRequest(
            @NotBlank @Email String email,
            @NotBlank String subject,
            @NotBlank String message
    ) {}

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> enviar(@Valid @RequestBody EnviarRequest r) {
        notifications.enviar(r.email(), r.subject(), r.message());
        return ResponseEntity.ok(Map.of("status", "Mensaje enviado correctamente"));
    }
}