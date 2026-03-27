package com.uniandes.ventas.domain.model;

import com.uniandes.ventas.persistence.entity.OrdenCompra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrdenCompraDTO implements Serializable {

    private Long idOrdenCompra;
    private String idFactura;
    private Integer idCliente;
    private LocalDateTime fechaCompra;
    private OrdenCompraStatus estado;
    private BigDecimal total;

}