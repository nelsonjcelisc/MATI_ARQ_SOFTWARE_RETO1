package com.uniandes.ventas.persistence.crud;

import com.uniandes.ventas.persistence.entity.OrdenCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrudOrdenCompraEntity extends JpaRepository<OrdenCompra, Long> {

    Optional<OrdenCompra> findByIdFactura(String idFactura);
}
