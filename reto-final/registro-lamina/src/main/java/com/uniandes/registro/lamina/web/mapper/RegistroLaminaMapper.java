package com.uniandes.registro.lamina.web.mapper;

import com.uniandes.registro.lamina.domain.model.RegistroLaminaDTO;
import com.uniandes.registro.lamina.persistence.entity.RegistroLamina;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaRequest;
import com.uniandes.registro.lamina.web.dto.RegistroLaminaResponse;
import org.springframework.stereotype.Component;

//NO SE NECESITA POR AHORA

@Component
public class RegistroLaminaMapper {
    /**
     * Método estático para convertir la Entidad (DB) a este POJO de Caché
     */
    public static RegistroLaminaDTO fromRegistroLaminaRequest(RegistroLaminaRequest entity) {
        if (entity == null) return null;

        return RegistroLaminaDTO.builder()
                .idUsuario(entity.getIdUsuario())
                .idLamina(entity.getIdLamina())
                .fechaRegistro(entity.getFechaRegistro())
                .build();
    }

    /**
     * Método estático para convertir  este POJO de Cache a OrdenCompraResponse
     */
    public static RegistroLaminaResponse fromDTO(RegistroLaminaDTO oc) {
        return RegistroLaminaResponse.builder()
                .idRegistroLamina(oc.getIdRegistroLamina())
                .idLamina(oc.getIdLamina())
                .idUsuario(oc.getIdUsuario())
                .fechaRegistro(oc.getFechaRegistro())
                .build();
    }

}
