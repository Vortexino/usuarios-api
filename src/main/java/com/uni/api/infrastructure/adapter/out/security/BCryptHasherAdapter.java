package com.uni.api.infrastructure.adapter.out.security;

import com.uni.api.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptHasherAdapter implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String plano) {
        return encoder.encode(plano);
    }

    @Override
    public boolean coincide(String plano, String hash) {
        return encoder.matches(plano, hash);
    }
}