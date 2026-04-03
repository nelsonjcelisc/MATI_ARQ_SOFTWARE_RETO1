package com.uniandes.admin_api.persistence.impl;

import com.uniandes.admin_api.domain.model.AdminProfileDTO;
import com.uniandes.admin_api.domain.model.AdminProfileStatus;
import com.uniandes.admin_api.domain.repository.AdminProfileRepository;
import com.uniandes.admin_api.persistence.crud.AdminProfileCRUD;
import com.uniandes.admin_api.persistence.entity.AdminProfile;
import com.uniandes.admin_api.persistence.mapper.AdminProfileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
public class AdminProfileRepositoryImpl implements AdminProfileRepository {

    private final AdminProfileCRUD crudRepository;
    private final AdminProfileMapper mapper;

    public AdminProfileRepositoryImpl(AdminProfileCRUD crudRepository, AdminProfileMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public List<AdminProfileDTO> findAll() {
        return crudRepository.findAllWithTrustedContexts().stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public List<AdminProfileDTO> findByStatus(AdminProfileStatus status) {
        return crudRepository.findByStatusWithTrustedContexts(status).stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Override
    public Optional<AdminProfileDTO> findById(long id) {
        return crudRepository.findById(id)
                .map(mapper::toDTO);
    }

    @Override
    public Optional<AdminProfileDTO> findByAdminId(String adminId) {
        return crudRepository.findByAdminIdWithTrustedContexts(adminId)
                .map(mapper::toDTO);
    }

    @Override
    public AdminProfileDTO save(AdminProfileDTO dto) {
        AdminProfile entity = mapper.toEntity(dto);
        AdminProfile saved = crudRepository.save(entity);
        return mapper.toDTO(saved);
    }

    @Override
    public void delete(long id) {
        if (!crudRepository.existsById(id)) {
            throw new NoSuchElementException("AdminProfile not found with id: " + id);
        }
        crudRepository.deleteById(id);
    }
}
