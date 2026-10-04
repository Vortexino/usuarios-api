package com.uni.api.domain.model;

public record Usuario(Long id, String matricula, String nombre,
                      String email, String passwordHash, String rol, String fotoUrl) {}