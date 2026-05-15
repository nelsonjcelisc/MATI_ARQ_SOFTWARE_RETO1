package com.uniandes.registro.lamina.persistence.entity;

import com.uniandes.registro.lamina.web.dto.RegistroLaminaRequest;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_lamina", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_registrolamina_idLamina", columnNames = "id_lamina")
        }
)

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // requerido por JPA
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true) // evita imprimir todo (lazy, relaciones)
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // equals/hashCode por id

public class RegistroLamina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_lamina")
    @ToString.Include
    private Long idRegistroLamina;

    @Column(name = "id_usuario", nullable = false)
    @ToString.Include
    private Integer idUsuario;

    @Column(name = "id_lamina", length = 120, nullable = false, unique = true)
    @ToString.Include
    private String idLamina;

    @Column(name = "fecha_registro", nullable = false)
    @ToString.Include
    private LocalDateTime fechaRegistro;

    @Builder
    public RegistroLamina(String idLamina, Integer idUsuario, LocalDateTime fechaRegistro, Long idRegistroLamina) {
        this.idLamina = idLamina;
        this.idUsuario = idUsuario;
        this.fechaRegistro = fechaRegistro;
        this.idRegistroLamina = idRegistroLamina;
    }

    public RegistroLamina(RegistroLaminaRequest request) {
        this.idLamina = request.getIdLamina();
        this.idUsuario = request.getIdUsuario();
        this.fechaRegistro = request.getFechaRegistro();
    }
}
