package com.uni.api.infrastructure.adapter.in.web;

import com.uni.api.application.port.in.LoginUseCase;
import com.uni.api.application.port.in.UsuarioUseCase;
import com.uni.api.domain.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioUseCase usuarios;
    private final LoginUseCase loginUseCase;

    public AuthController(UsuarioUseCase usuarios, LoginUseCase loginUseCase) {
        this.usuarios = usuarios;
        this.loginUseCase = loginUseCase;
    }

    public record RegistroRequest(@NotBlank String matricula, @NotBlank String nombre,
                                  @NotBlank @Email String email, @Size(min = 8) String password) {}
    public record LoginRequest(@NotBlank String matricula, @NotBlank String password) {}
    public record TokenResponse(String token, Long id, String nombre) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioController.UsuarioResponse registrar(@Valid @RequestBody RegistroRequest r) {
        return UsuarioController.UsuarioResponse.de(
                usuarios.crear(r.matricula(), r.nombre(), r.email(), r.password()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest r) {
        String token = loginUseCase.login(r.matricula(), r.password());
        Usuario u = usuarios.listar().stream()
                .filter(usr -> usr.matricula().equals(r.matricula()))
                .findFirst().orElseThrow();
        return new TokenResponse(token, u.id(), u.nombre());
    }
}