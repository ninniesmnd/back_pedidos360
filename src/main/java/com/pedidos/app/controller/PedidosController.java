package com.pedidos.app.controller;

import com.pedidos.app.dto.CambiarEstadoRequest;
import com.pedidos.app.dto.CrearPedidoRequest;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.ItemPedido;
import com.pedidos.app.model.Pedido;
import com.pedidos.app.repository.PedidoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidosController {

    private final PedidoRepository pedidoRepository;

    public PedidosController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("/cocina/ping")
    public String pingCocina() {
        return "OK - autenticado como OperadorCocina o superior";
    }

    // CU-02: Cliente, AdminLocal o AdminGeneral crean un pedido
    @PostMapping
    public ResponseEntity<Pedido> crearPedido(@Valid @RequestBody CrearPedidoRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        Pedido pedido = new Pedido();
        pedido.setLocalId(request.localId());
        pedido.setTipoDespacho(request.tipoDespacho());
        pedido.setClienteEmail(extraerEmail(jwt));
        pedido.setEstado(EstadoPedido.RECIBIDO);

        request.items().forEach(itemReq -> {
            ItemPedido item = new ItemPedido();
            item.setProductoId(itemReq.productoId());
            item.setNombreProducto(itemReq.nombreProducto());
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(itemReq.precioUnitario());
            pedido.agregarItem(item);
        });

        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoRepository.save(pedido));
    }

    // CU-02 (consulta): Cliente ve sus propios pedidos
    @GetMapping("/mis-pedidos")
    public List<Pedido> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidoRepository.findByClienteEmailOrderByFechaCreacionDesc(extraerEmail(jwt));
    }

    // CU-04: OperadorCocina ve pedidos recien recibidos de su local
    @GetMapping("/cocina")
    public List<Pedido> pedidosParaCocina(@RequestParam Long localId) {
        return pedidoRepository.findByLocalIdAndEstado(localId, EstadoPedido.RECIBIDO);
    }

    // CU-04: Repartidor ve pedidos listos para reparto
    @GetMapping("/despacho")
    public List<Pedido> pedidosParaDespacho() {
        return pedidoRepository.findByEstado(EstadoPedido.LISTO_PARA_DESPACHO);
    }

    // CU-04: AdminLocal/AdminGeneral ven el listado de su local
    @GetMapping("/admin")
    public List<Pedido> listarPedidos(@RequestParam Long localId) {
        return pedidoRepository.findByLocalIdOrderByFechaCreacionDesc(localId);
    }

    // CU-03: cambiar estado (OperadorCocina, AdminLocal, AdminGeneral)
    @PatchMapping("/{id}/estado")
    public Pedido cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado: " + id));
        pedido.setEstado(request.nuevoEstado());
        pedido.setFechaActualizacion(LocalDateTime.now());
        return pedidoRepository.save(pedido);
    }

    private String extraerEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        return email != null ? email : jwt.getClaimAsString("email");
    }
}