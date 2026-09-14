package com.pedidos.app.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios_local")
@Data
public class UsuarioLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "local_id", nullable = false)
    private Long localId;
}