package com.pedidos.app.repository;

import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.Pedido;
import com.pedidos.app.model.TipoDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmailOrderByFechaCreacionDesc(String clienteEmail);

    List<Pedido> findByLocalIdOrderByFechaCreacionDesc(Long localId);

    List<Pedido> findByLocalIdAndEstadoIn(Long localId, List<EstadoPedido> estados);

    List<Pedido> findByEstadoAndTipoDespacho(EstadoPedido estado, TipoDespacho tipoDespacho);
}