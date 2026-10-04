package com.uni.api.application.port.out;

public interface PasswordHasherPort {
    String hash(String plano);
    boolean coincide(String plano, String hash);
}