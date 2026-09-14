package com.pedidos.app.controller;

import com.pedidos.app.dto.CambiarEstadoRequest;
import com.pedidos.app.dto.CrearPedidoRequest;
import com.pedidos.app.model.EstadoPedido;
import com.pedidos.app.model.ItemPedido;
import com.pedidos.app.model.Pedido;
import com.pedidos.app.model.TipoDespacho;
import com.pedidos.app.repository.PedidoRepository;
import com.pedidos.app.repository.UsuarioLocalRepository;
import com.pedidos.app.model.UsuarioLocal;
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
    private final UsuarioLocalRepository usuarioLocalRepository;

    public PedidosController(PedidoRepository pedidoRepository, UsuarioLocalRepository usuarioLocalRepository) {
        this.pedidoRepository = pedidoRepository;
        this.usuarioLocalRepository = usuarioLocalRepository;
    }

    // CU-02
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

    @GetMapping("/mis-pedidos")
    public List<Pedido> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidoRepository.findByClienteEmailOrderByFechaCreacionDesc(extraerEmail(jwt));
    }

    // Ahora usa el local del propio usuario (tabla usuarios_local), ya no un query param.
    // Incluye RECIBIDO + EN_PREPARACION para que el pedido no "desaparezca" apenas se empieza a preparar.
    @GetMapping("/cocina")
    public List<Pedido> pedidosParaCocina(@AuthenticationPrincipal Jwt jwt) {
        Long localId = obtenerLocalDelUsuario(jwt);
        return pedidoRepository.findByLocalIdAndEstadoIn(
                localId, List.of(EstadoPedido.RECIBIDO, EstadoPedido.EN_PREPARACION));
    }

    // Ahora filtra solo DELIVERY: el Repartidor no debe ver retiros en tienda.
    @GetMapping("/despacho")
    public List<Pedido> pedidosParaDespacho() {
        return pedidoRepository.findByEstadoAndTipoDespacho(EstadoPedido.LISTO_PARA_DESPACHO, TipoDespacho.DELIVERY);
    }

    // AdminGeneral puede pasar cualquier localId (o ninguno, para ver todos).
    // AdminLocal SIEMPRE queda restringido a su propio local, sin importar qué mande en el query param.
    @GetMapping("/admin")
    public List<Pedido> listarPedidos(@RequestParam(required = false) Long localId,
                                       @AuthenticationPrincipal Jwt jwt) {
        if (tieneRol(jwt, "AdminGeneral")) {
            return localId != null
                    ? pedidoRepository.findByLocalIdOrderByFechaCreacionDesc(localId)
                    : pedidoRepository.findAll();
        }
        Long miLocalId = obtenerLocalDelUsuario(jwt);
        return pedidoRepository.findByLocalIdOrderByFechaCreacionDesc(miLocalId);
    }

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

    private boolean tieneRol(Jwt jwt, String rol) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(rol);
    }

    private Long obtenerLocalDelUsuario(Jwt jwt) {
        String email = extraerEmail(jwt);
        return usuarioLocalRepository.findByEmail(email)
                .map(UsuarioLocal::getLocalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "El usuario " + email + " no está asociado a ningún local"));
    }
}