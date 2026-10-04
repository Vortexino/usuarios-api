package com.uni.api.infrastructure.adapter.out.persistence;

import com.uni.api.domain.model.Usuario;
import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String matricula;
    private String nombre;
    @Column(unique = true, nullable = false)
    private String email;
    private String passwordHash;
    private String rol;
    private String fotoUrl;

    protected UsuarioEntity() {}

    public UsuarioEntity(Usuario u) {
        this.id = u.id();
        this.matricula = u.matricula();
        this.nombre = u.nombre();
        this.email = u.email();
        this.passwordHash = u.passwordHash();
        this.rol = u.rol();
        this.fotoUrl = u.fotoUrl();
    }

    public Usuario toDomain() {
        return new Usuario(id, matricula, nombre, email, passwordHash, rol, fotoUrl);
    }
}