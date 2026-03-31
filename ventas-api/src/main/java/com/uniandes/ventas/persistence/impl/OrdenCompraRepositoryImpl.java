package com.uniandes.ventas.persistence.impl;

import com.uniandes.ventas.domain.model.OrdenCompraDTO;
import com.uniandes.ventas.domain.repository.OrdenCompraRepository;
import com.uniandes.ventas.persistence.crud.CrudOrdenCompraEntity;
import com.uniandes.ventas.persistence.entity.OrdenCompra;
import com.uniandes.ventas.persistence.mapper.OrdenCompraEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class OrdenCompraRepositoryImpl implements OrdenCompraRepository {

    private final CrudOrdenCompraEntity crudRepository;
    private final OrdenCompraEntityMapper mapper;

    public OrdenCompraRepositoryImpl(CrudOrdenCompraEntity crudRepository, OrdenCompraEntityMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public OrdenCompraDTO save(OrdenCompraDTO ordenCompraDTO) {
        OrdenCompra entity = mapper.fromDTO(ordenCompraDTO);
        OrdenCompra saved = crudRepository.saveAndFlush(entity);
        return mapper.fromEntity(saved);
    }

    @Override
    public Optional<OrdenCompraDTO> findById(Long id) {
        return crudRepository.findById(id)
                .map(mapper::fromEntity);
    }

    @Override
    public List<OrdenCompraDTO> findAll() {
        return crudRepository.findAll().stream()
                .map(mapper::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        crudRepository.deleteById(id);
    }

    @Override
    public Optional<OrdenCompraDTO> findByIdFactura(String idFactura) {
        return crudRepository.findByIdFactura(idFactura)
                .map(mapper::fromEntity);
    }
}
