package com.uni.api.infrastructure.config;

import com.uni.api.application.port.out.FileStoragePort;
import com.uni.api.application.port.out.PasswordHasherPort;
import com.uni.api.application.port.out.TokenProviderPort;
import com.uni.api.application.port.out.UsuarioRepositoryPort;
import com.uni.api.application.service.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean
    public UsuarioService usuarioService(UsuarioRepositoryPort repo,
                                         PasswordHasherPort hasher,
                                         TokenProviderPort tokens,
                                         FileStoragePort fileStorage) {
        return new UsuarioService(repo, hasher, tokens, fileStorage);
    }
}