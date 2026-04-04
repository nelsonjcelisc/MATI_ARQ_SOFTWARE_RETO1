package com.uniandes.admin_api.persistence.impl;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;
import com.uniandes.admin_api.domain.repository.VendorCommissionRepository;
import com.uniandes.admin_api.persistence.crud.VendorCommissionCRUD;
import com.uniandes.admin_api.persistence.entity.VendorCommission;
import com.uniandes.admin_api.persistence.mapper.VendorCommissionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;

@Repository
public class VendorCommissionRepositoryImpl implements VendorCommissionRepository {
    private final VendorCommissionCRUD crudRepository;
    private final VendorCommissionMapper mapper;

    public VendorCommissionRepositoryImpl(VendorCommissionCRUD crudRepository, VendorCommissionMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public List<VendorCommissionDTO> findAll() {
        return crudRepository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public VendorCommissionDTO findById(long id) {
        return crudRepository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NoSuchElementException("Commission not found with id: " + id));
    }

    @Override
    public VendorCommissionDTO save(VendorCommissionDTO dto) {
        VendorCommission entity = mapper.toEntity(dto);
        VendorCommission saved = crudRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public VendorCommissionDTO update(long id, VendorCommissionDTO dto) {
        VendorCommission entity = crudRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Commission not found with id: " + id));
        mapper.updateEntityFromDTO(entity, dto);
        VendorCommission updated = crudRepository.save(entity);
        return mapper.toDTO(updated);
    }

    @Override
    public void delete(long id) {
        if (!crudRepository.existsById(id)) {
            throw new NoSuchElementException("Commission not found with id: " + id);
        }
        crudRepository.deleteById(id);
    }
}
