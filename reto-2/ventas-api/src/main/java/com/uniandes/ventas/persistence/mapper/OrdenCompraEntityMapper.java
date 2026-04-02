package com.uniandes.ventas.persistence.mapper;

import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.persistence.entity.OrdenCompra;
import org.springframework.stereotype.Component;

@Component
public class OrdenCompraEntityMapper {
    /**
     * Método estático para convertir la Entidad (DB) a este POJO de Caché
     */
    public OrdenCompraDTO fromEntity(OrdenCompra entity) {
        if (entity == null) return null;

        return OrdenCompraDTO.builder()
                .idOrdenCompra(entity.getIdOrdenCompra())
                .idCliente(entity.getIdCliente())
                .idFactura(entity.getIdFactura())
                .fechaCompra(entity.getFechaCompra())
                .estado(entity.getEstado())
                .total(entity.getTotal())
                .build();
    }

    /**
     * Método estático para convertir  este POJO de Caché a la Entidad
     */
    public OrdenCompra fromDTO(OrdenCompraDTO entity) {
        if (entity == null) return null;

        return OrdenCompra.builder()
                .idOrdenCompra(entity.getIdOrdenCompra())
                .idCliente(entity.getIdCliente())
                .idFactura(entity.getIdFactura())
                .fechaCompra(entity.getFechaCompra())
                .estado(entity.getEstado())
                .total(entity.getTotal())
                .build();
    }

}
