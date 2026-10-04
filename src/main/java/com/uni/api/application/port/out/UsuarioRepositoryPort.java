package com.uni.api.application.port.out;

import com.uni.api.domain.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {
    Usuario guardar(Usuario usuario);
    Optional<Usuario> buscarPorId(Long id);
    Optional<Usuario> buscarPorMatricula(String matricula);
    List<Usuario> listar();
    void eliminar(Long id);
    boolean existePorMatricula(String matricula);
}