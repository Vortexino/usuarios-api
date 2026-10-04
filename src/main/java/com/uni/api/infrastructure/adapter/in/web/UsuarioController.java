package com.uni.api.infrastructure.adapter.in.web;

import com.uni.api.application.port.in.UsuarioUseCase;
import com.uni.api.application.port.out.FileStoragePort;
import com.uni.api.domain.model.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioUseCase usuarios;
    private final FileStoragePort fileStorage;

    public UsuarioController(UsuarioUseCase usuarios, FileStoragePort fileStorage) {
        this.usuarios = usuarios;
        this.fileStorage = fileStorage;
    }

    public record UsuarioResponse(Long id, String matricula, String nombre, String email, String rol, String fotoUrl) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(u.id(), u.matricula(), u.nombre(), u.email(), u.rol(), u.fotoUrl());
        }
    }
    public record ActualizarRequest(@NotBlank String nombre, @NotBlank @Email String email) {}

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarios.listar().stream().map(UsuarioResponse::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioResponse porId(@PathVariable Long id) {
        return UsuarioResponse.de(usuarios.porId(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarRequest r) {
        return UsuarioResponse.de(usuarios.actualizar(id, r.nombre(), r.email()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) { usuarios.eliminar(id); }

    @PostMapping("/{id}/foto")
    public UsuarioResponse subirFoto(@PathVariable Long id,
                                     @RequestParam("archivo") MultipartFile archivo) throws IOException {
        String nombreGuardado = fileStorage.guardar(archivo.getOriginalFilename(), archivo.getBytes());
        return UsuarioResponse.de(usuarios.actualizarFoto(id, nombreGuardado));
    }
}