package com.ventas.app.controller;

import com.ventas.app.dto.CrearVentaRequest;
import com.ventas.app.model.ItemVenta;
import com.ventas.app.model.Producto;
import com.ventas.app.model.Venta;
import com.ventas.app.repository.ProductoRepository;
import com.ventas.app.repository.VentaRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;

    public VentaController(VentaRepository ventaRepository, ProductoRepository productoRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public List<Venta> listarVentas(@RequestParam(required = false) Long localId) {
        return localId != null
                ? ventaRepository.findByLocalIdOrderByFechaVentaDesc(localId)
                : ventaRepository.findAllByOrderByFechaVentaDesc();
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public ResponseEntity<Venta> registrarVenta(@Valid @RequestBody CrearVentaRequest request,
                                                 @AuthenticationPrincipal Jwt jwt) {
        Venta venta = new Venta();
        venta.setLocalId(request.localId());
        venta.setPedidoId(request.pedidoId());
        venta.setVendedorEmail(extraerEmail(jwt));

        double total = 0.0;
        for (CrearVentaRequest.ItemVentaRequest itemReq : request.items()) {
            Producto producto = productoRepository.findById(itemReq.productoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Producto no encontrado: " + itemReq.productoId()));

            int nuevoStock = producto.getStock() - itemReq.cantidad();
            if (nuevoStock < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre());
            }
            producto.setStock(nuevoStock);
            productoRepository.save(producto);

            ItemVenta item = new ItemVenta();
            item.setProductoId(producto.getId());
            item.setNombreProducto(producto.getNombre());
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(producto.getPrecio());
            venta.agregarItem(item);

            total += producto.getPrecio() * itemReq.cantidad();
        }
        venta.setTotal(total);

        return ResponseEntity.status(HttpStatus.CREATED).body(ventaRepository.save(venta));
    }

    private String extraerEmail(Jwt jwt) {
        String email = jwt.getClaimAsString("preferred_username");
        return email != null ? email : jwt.getClaimAsString("email");
    }
}