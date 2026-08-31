package com.pedidos.app.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Data
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    @Column(name = "cliente_email", nullable = false)
    private String clienteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_despacho", nullable = false)
    private TipoDespacho tipoDespacho;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    public void agregarItem(ItemPedido item) {
        item.setPedido(this);
        this.items.add(item);
    }
}