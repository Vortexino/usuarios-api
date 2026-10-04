package com.uni.api.infrastructure.adapter.out.persistence;

import com.uni.api.application.port.out.UsuarioRepositoryPort;
import com.uni.api.domain.model.Usuario;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpa;

    public UsuarioPersistenceAdapter(UsuarioJpaRepository jpa) { this.jpa = jpa; }

    @Override public Usuario guardar(Usuario u) { return jpa.save(new UsuarioEntity(u)).toDomain(); }
    @Override public Optional<Usuario> buscarPorId(Long id) { return jpa.findById(id).map(UsuarioEntity::toDomain); }
    @Override public Optional<Usuario> buscarPorMatricula(String m) { return jpa.findByMatricula(m).map(UsuarioEntity::toDomain); }
    @Override public List<Usuario> listar() { return jpa.findAll().stream().map(UsuarioEntity::toDomain).toList(); }
    @Override public void eliminar(Long id) { jpa.deleteById(id); }
    @Override public boolean existePorMatricula(String m) { return jpa.existsByMatricula(m); }
}