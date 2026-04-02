package com.uniandes.ventas.domain.repository;

import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.persistence.entity.OrdenCompra;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository {

    OrdenCompraDTO save(OrdenCompraDTO order);
    Optional<OrdenCompraDTO> findById(Long id);
    List<OrdenCompraDTO> findAll();
    void deleteById(Long id);
    Optional<OrdenCompraDTO> findByIdFactura(String idFactura);
}
