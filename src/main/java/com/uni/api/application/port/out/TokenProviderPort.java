package com.uni.api.application.port.out;

import com.uni.api.domain.model.Usuario;
import java.util.Optional;

public interface TokenProviderPort {
    String generar(Usuario usuario);
    Optional<String> validarYObtenerSubject(String token);
}