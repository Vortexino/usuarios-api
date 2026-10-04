package com.uni.api.infrastructure.config;

import com.uni.api.application.port.out.TokenProviderPort;
import com.uni.api.application.port.out.UsuarioRepositoryPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TokenProviderPort tokens,
                                    UsuarioRepositoryPort repo) throws Exception {
        return http
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/**", "/uploads/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(tokens, repo), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    static class JwtAuthFilter extends OncePerRequestFilter {
        private final TokenProviderPort tokens;
        private final UsuarioRepositoryPort repo;

        JwtAuthFilter(TokenProviderPort tokens, UsuarioRepositoryPort repo) {
            this.tokens = tokens;
            this.repo = repo;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                        FilterChain chain) throws ServletException, IOException {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                tokens.validarYObtenerSubject(header.substring(7))
                        .flatMap(repo::buscarPorMatricula)
                        .ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(u.matricula(), null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + u.rol())))));
            }
            chain.doFilter(req, res);
        }
    }
}