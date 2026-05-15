package com.uniandes.registro.lamina.domain.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroLaminaDTO implements Serializable {

    private Long idRegistroLamina;
    private String idLamina;
    private Integer idUsuario;
    private LocalDateTime fechaRegistro;
}