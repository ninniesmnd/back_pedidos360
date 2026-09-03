package com.pedidos.app.repository;

import com.pedidos.app.model.UsuarioLocal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioLocalRepository extends JpaRepository<UsuarioLocal, Long> {
    Optional<UsuarioLocal> findByEmail(String email);
}