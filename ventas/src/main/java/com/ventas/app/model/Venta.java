package com.ventas.app.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    // Referencia opcional al pedido de origen (vive en pedidos-service; no hay FK real entre microservicios).
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "vendedor_email", nullable = false)
    private String vendedorEmail;

    @Column(nullable = false)
    private Double total = 0.0;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta = LocalDateTime.now();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemVenta> items = new ArrayList<>();

    public void agregarItem(ItemVenta item) {
        item.setVenta(this);
        this.items.add(item);
    }
}