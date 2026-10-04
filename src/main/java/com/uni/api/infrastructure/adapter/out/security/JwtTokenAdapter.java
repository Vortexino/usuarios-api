package com.uni.api.infrastructure.adapter.out.security;

import com.uni.api.application.port.out.TokenProviderPort;
import com.uni.api.domain.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtTokenAdapter implements TokenProviderPort {

    private final SecretKey key;
    private final long expiracionMs;

    public JwtTokenAdapter(@Value("${jwt.secret}") String secretBase64,
                           @Value("${jwt.expiration-ms}") long expiracionMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracionMs = expiracionMs;
    }

    @Override
    public String generar(Usuario usuario) {
        return Jwts.builder()
                .subject(usuario.matricula())
                .claim("rol", usuario.rol())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiracionMs))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<String> validarYObtenerSubject(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload().getSubject();
            return Optional.of(subject);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}