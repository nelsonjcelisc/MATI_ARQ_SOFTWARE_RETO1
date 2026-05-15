package com.uniandes.registro.lamina.web.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
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

public class RegistroLaminaRequest {

    @NotNull(message = "El ID del usuario es obligatorio")
    @Min(0)
    private Integer idUsuario;

    @NotBlank(message = "El idLamina es obligatorio y no puede estar vacío")
    @Size(min = 5, max = 50, message = "El idLamina debe tener entre 5 y 50 caracteres")
    private String idLamina;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

}
