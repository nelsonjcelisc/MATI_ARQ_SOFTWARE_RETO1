package com.uniandes.registro.lamina.domain.repository;

import com.uniandes.registro.lamina.domain.model.RegistroLaminaDTO;

import java.util.List;
import java.util.Optional;

public interface RegistroLaminaRepository {

    RegistroLaminaDTO save(RegistroLaminaDTO registro);
    Optional<RegistroLaminaDTO> findById(Long id);
    List<RegistroLaminaDTO> findAll();
    void deleteById(Long id);
    Optional<RegistroLaminaDTO> findByIdLamina(String idLamina);
}
