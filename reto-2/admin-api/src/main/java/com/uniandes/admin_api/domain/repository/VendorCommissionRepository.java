package com.uniandes.admin_api.domain.repository;

import com.uniandes.admin_api.domain.model.VendorCommissionDTO;

import java.util.List;

public interface VendorCommissionRepository {
    List<VendorCommissionDTO> findAll();
    VendorCommissionDTO findById(long id);
    VendorCommissionDTO save(VendorCommissionDTO dto);
    VendorCommissionDTO update(long id, VendorCommissionDTO dto);
    void delete(long id);
}
