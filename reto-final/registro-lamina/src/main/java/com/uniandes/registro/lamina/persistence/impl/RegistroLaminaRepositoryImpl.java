package com.uniandes.registro.lamina.persistence.impl;

import com.uniandes.registro.lamina.domain.model.RegistroLaminaDTO;
import com.uniandes.registro.lamina.domain.repository.RegistroLaminaRepository;
import com.uniandes.registro.lamina.persistence.crud.CrudRegistroLaminaEntity;
import com.uniandes.registro.lamina.persistence.entity.RegistroLamina;
import com.uniandes.registro.lamina.persistence.mapper.RegistroLaminaEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RegistroLaminaRepositoryImpl implements RegistroLaminaRepository {

    private final CrudRegistroLaminaEntity crudRepository;
    private final RegistroLaminaEntityMapper mapper;

    public RegistroLaminaRepositoryImpl(CrudRegistroLaminaEntity crudRepository, RegistroLaminaEntityMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public RegistroLaminaDTO save(RegistroLaminaDTO registroLaminaDTO) {
        RegistroLamina entity = mapper.fromDTO(registroLaminaDTO);
        RegistroLamina saved = crudRepository.saveAndFlush(entity);
        return mapper.fromEntity(saved);
    }

    @Override
    public Optional<RegistroLaminaDTO> findById(Long id) {
        return crudRepository.findById(id)
                .map(mapper::fromEntity);
    }

    @Override
    public List<RegistroLaminaDTO> findAll() {
        return crudRepository.findAll().stream()
                .map(mapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        crudRepository.deleteById(id);
    }

    @Override
    public Optional<RegistroLaminaDTO> findByIdLamina(String idLamina) {
        return crudRepository.findByIdLamina(idLamina)
                .map(mapper::fromEntity);
    }
}
