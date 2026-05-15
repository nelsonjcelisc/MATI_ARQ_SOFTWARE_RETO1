package com.uniandes.registro.lamina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistroLaminaResponse {

    private Long idRegistroLamina;
    private String idLamina;
    private Integer idUsuario;
    private LocalDateTime fechaRegistro;
}