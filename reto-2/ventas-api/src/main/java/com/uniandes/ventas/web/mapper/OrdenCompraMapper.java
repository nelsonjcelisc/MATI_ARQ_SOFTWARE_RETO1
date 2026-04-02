package com.uniandes.ventas.web.mapper;

import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.persistence.entity.OrdenCompra;
import com.uniandes.ventas.web.dto.OrdenCompraRequest;
import com.uniandes.ventas.web.dto.OrdenCompraResponse;
import org.springframework.stereotype.Component;

//NO SE NECESITA POR AHORA

@Component
public class OrdenCompraMapper {
    /**
     * Método estático para convertir la Entidad (DB) a este POJO de Caché
     */
    public static OrdenCompraDTO fromOrderCompraRequest(OrdenCompraRequest entity) {
        if (entity == null) return null;

        return OrdenCompraDTO.builder()
                //.idOrdenCompra(entity.getIdOrdenCompra())
                .idCliente(entity.getIdCliente())
                .idFactura(entity.getIdFactura())
                .fechaCompra(entity.getFechaCompra())
                .estado(entity.getEstado())
                .total(entity.getTotal())
                .build();
    }

    /**
     * Método estático para convertir  este POJO de Cache a OrdenCompraResponse
     */
    public static OrdenCompraResponse fromDTO(OrdenCompraDTO oc) {
        return OrdenCompraResponse.builder()
                .idOrdenCompra(oc.getIdOrdenCompra())
                .idFactura(oc.getIdFactura())
                .idCliente(oc.getIdCliente())
                .fechaCompra(oc.getFechaCompra())
                .estado(oc.getEstado())
                .total(oc.getTotal())
                .build();
    }

}
