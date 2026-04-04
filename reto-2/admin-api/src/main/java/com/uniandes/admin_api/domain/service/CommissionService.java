package com.uniandes.admin_api.domain.service;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;
import com.uniandes.admin_api.domain.repository.VendorCommissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommissionService {
    private final VendorCommissionRepository repository;

    public CommissionService(VendorCommissionRepository repository) {
        this.repository = repository;
    }

    public List<VendorCommissionDTO> findAll() {
        return repository.findAll();
    }

    public VendorCommissionDTO findById(long id) {
        return repository.findById(id);
    }

    public VendorCommissionDTO create(VendorCommissionDTO dto) {
        return repository.save(dto);
    }

    public VendorCommissionDTO update(long id, VendorCommissionDTO dto) {
        return repository.update(id, dto);
    }

    public void delete(long id) {
        repository.delete(id);
    }
}
