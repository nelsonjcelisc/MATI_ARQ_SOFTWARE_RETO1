package com.uniandes.ventas.web.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.uniandes.ventas.domain.model.OrdenCompraStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class OrdenCompraRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    @Min(0)
    private Integer idCliente;

    @NotBlank(message = "El idFactura es obligatorio y no puede estar vacío")
    @Size(min = 5, max = 50, message = "El idFactura debe tener entre 5 y 50 caracteres")
    private String idFactura;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaCompra = LocalDateTime.now();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private OrdenCompraStatus estado;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "El total debe ser > 0")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal total;

}
