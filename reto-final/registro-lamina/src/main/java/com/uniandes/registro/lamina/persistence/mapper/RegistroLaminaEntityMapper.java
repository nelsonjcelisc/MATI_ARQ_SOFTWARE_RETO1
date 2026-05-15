package com.uniandes.registro.lamina.persistence.mapper;

import com.uniandes.registro.lamina.domain.model.RegistroLaminaDTO;
import com.uniandes.registro.lamina.persistence.entity.RegistroLamina;
import org.springframework.stereotype.Component;

@Component
public class RegistroLaminaEntityMapper {
    /**
     * Método estático para convertir la Entidad (DB) a este POJO de Caché
     */
    public RegistroLaminaDTO fromEntity(RegistroLamina entity) {
        if (entity == null) return null;

        return RegistroLaminaDTO.builder()
                .idRegistroLamina(entity.getIdRegistroLamina())
                .idUsuario(entity.getIdUsuario())
                .idLamina(entity.getIdLamina())
                .fechaRegistro(entity.getFechaRegistro())
                .build();
    }

    /**
     * Método estático para convertir  este POJO de Caché a la Entidad
     */
    public RegistroLamina fromDTO(RegistroLaminaDTO entity) {
        if (entity == null) return null;

        return RegistroLamina.builder()
                .idRegistroLamina(entity.getIdRegistroLamina())
                .idUsuario(entity.getIdUsuario())
                .idLamina(entity.getIdLamina())
                .fechaRegistro(entity.getFechaRegistro())
                .build();
    }

}
