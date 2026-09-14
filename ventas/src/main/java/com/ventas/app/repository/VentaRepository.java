package com.ventas.app.repository;

import com.ventas.app.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByLocalIdOrderByFechaVentaDesc(Long localId);
    List<Venta> findAllByOrderByFechaVentaDesc();
}