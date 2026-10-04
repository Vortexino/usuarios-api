package com.uni.api.application.port.in;

import com.uni.api.domain.model.Usuario;
import java.util.List;

public interface UsuarioUseCase {
    Usuario crear(String matricula, String nombre, String email, String password);
    List<Usuario> listar();
    Usuario porId(Long id);
    Usuario actualizar(Long id, String nombre, String email);
    void eliminar(Long id);
    Usuario actualizarFoto(Long id, String fotoUrl);
}