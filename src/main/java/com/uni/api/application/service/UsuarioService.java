package com.uni.api.application.service;

import com.uni.api.application.port.in.LoginUseCase;
import com.uni.api.application.port.in.UsuarioUseCase;
import com.uni.api.application.port.out.PasswordHasherPort;
import com.uni.api.application.port.out.TokenProviderPort;
import com.uni.api.application.port.out.UsuarioRepositoryPort;
import com.uni.api.domain.exception.CredencialesInvalidasException;
import com.uni.api.domain.exception.MatriculaDuplicadaException;
import com.uni.api.domain.exception.RecursoNoEncontradoException;
import com.uni.api.domain.model.Usuario;
import com.uni.api.application.port.out.FileStoragePort;
import java.util.List;

public class UsuarioService implements UsuarioUseCase, LoginUseCase {

    private final UsuarioRepositoryPort repo;
    private final PasswordHasherPort hasher;
    private final TokenProviderPort tokens;
    private final FileStoragePort fileStorage;

    public UsuarioService(UsuarioRepositoryPort repo, PasswordHasherPort hasher,
                          TokenProviderPort tokens, FileStoragePort fileStorage) {
        this.repo = repo;
        this.hasher = hasher;
        this.tokens = tokens;
        this.fileStorage = fileStorage;
    }

    @Override
    public Usuario actualizarFoto(Long id, String fotoUrl) {
        Usuario actual = porId(id);
        Usuario nuevo = new Usuario(actual.id(), actual.matricula(), actual.nombre(),
                actual.email(), actual.passwordHash(), actual.rol(), fotoUrl);
        return repo.guardar(nuevo);
    }

    @Override
    public Usuario crear(String matricula, String nombre, String email, String password) {
        if (repo.existePorMatricula(matricula)) {
            throw new MatriculaDuplicadaException(matricula);
        }
        Usuario nuevo = new Usuario(null, matricula, nombre, email, hasher.hash(password), "USER", null);
        return repo.guardar(nuevo);
    }

    @Override
    public List<Usuario> listar() {
        return repo.listar();
    }

    @Override
    public Usuario porId(Long id) {
        return repo.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario " + id + " no existe"));
    }

    @Override
    public Usuario actualizar(Long id, String nombre, String email) {
        Usuario actual = porId(id);
        Usuario nuevo = new Usuario(actual.id(), actual.matricula(), nombre, email,
                actual.passwordHash(), actual.rol(), actual.fotoUrl());
        return repo.guardar(nuevo);
    }

    @Override
    public void eliminar(Long id) {
        porId(id); // lanza error 404 si no existe
        repo.eliminar(id);
    }

    @Override
    public String login(String matricula, String password) {
        Usuario usuario = repo.buscarPorMatricula(matricula)
                .orElseThrow(CredencialesInvalidasException::new);
        if (!hasher.coincide(password, usuario.passwordHash())) {
            throw new CredencialesInvalidasException();
        }
        return tokens.generar(usuario);
    }
}