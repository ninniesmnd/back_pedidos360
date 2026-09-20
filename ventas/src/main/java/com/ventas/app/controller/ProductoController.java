package com.ventas.app.controller;

import com.ventas.app.dto.ActualizarEstadoProductoRequest;
import com.ventas.app.dto.ActualizarStockRequest;
import com.ventas.app.dto.CrearProductoRequest;
import com.ventas.app.model.Producto;
import com.ventas.app.repository.ProductoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoRepository productoRepository;

    public ProductoController(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('APPROLE_Cliente','APPROLE_OperadorCocina','APPROLE_Repartidor','APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public List<Producto> listarProductos(@RequestParam(required = false) Long localId) {
        return localId != null
                ? productoRepository.findByLocalIdOrderByNombreAsc(localId)
                : productoRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('APPROLE_Cliente','APPROLE_OperadorCocina','APPROLE_Repartidor','APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public Producto obtenerProducto(@PathVariable Long id) {
        return buscarOFallar(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public ResponseEntity<Producto> crearProducto(@Valid @RequestBody CrearProductoRequest request) {
        Producto producto = new Producto();
        aplicarDatos(producto, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(productoRepository.save(producto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public Producto editarProducto(@PathVariable Long id, @Valid @RequestBody CrearProductoRequest request) {
        Producto producto = buscarOFallar(id);
        aplicarDatos(producto, request);
        producto.setFechaActualizacion(LocalDateTime.now());
        return productoRepository.save(producto);
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral','APPROLE_OperadorCocina')")
    public Producto actualizarStock(@PathVariable Long id, @Valid @RequestBody ActualizarStockRequest request) {
        Producto producto = buscarOFallar(id);
        int nuevoStock = producto.getStock() + request.delta();
        if (nuevoStock < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para el producto " + producto.getNombre());
        }
        producto.setStock(nuevoStock);
        producto.setFechaActualizacion(LocalDateTime.now());
        return productoRepository.save(producto);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('APPROLE_AdminLocal','APPROLE_AdminGeneral')")
    public Producto cambiarEstado(@PathVariable Long id, @Valid @RequestBody ActualizarEstadoProductoRequest request) {
        Producto producto = buscarOFallar(id);
        producto.setActivo(request.activo());
        producto.setFechaActualizacion(LocalDateTime.now());
        return productoRepository.save(producto);
    }

    private void aplicarDatos(Producto producto, CrearProductoRequest request) {
        producto.setLocalId(request.localId());
        producto.setNombre(request.nombre());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setStock(request.stock());
        producto.setCategoria(request.categoria());
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado: " + id));
    }
}