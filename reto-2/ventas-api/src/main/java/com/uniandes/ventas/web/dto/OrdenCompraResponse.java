package com.uniandes.ventas.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.uniandes.ventas.domain.model.OrdenCompraStatus;
import com.uniandes.ventas.persistence.entity.OrdenCompra;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrdenCompraResponse {

    private Long idOrdenCompra;
    private String idFactura;
    private Integer idCliente;
    private LocalDateTime fechaCompra;
    private OrdenCompraStatus estado;
    private BigDecimal total;

}