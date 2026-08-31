package com.pedidos.app.repository;

import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmailOrderByFechaCreacionDesc(String clienteEmail);

    List<Pedido> findByLocalIdAndEstado(Long localId, EstadoPedido estado);

    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findByLocalIdOrderByFechaCreacionDesc(Long localId);
}