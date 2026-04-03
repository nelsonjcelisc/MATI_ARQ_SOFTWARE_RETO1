package com.uniandes.admin_api.domain.repository;

import com.uniandes.admin_api.domain.model.AdminProfileDTO;
import com.uniandes.admin_api.domain.model.AdminProfileStatus;

import java.util.List;
import java.util.Optional;

public interface AdminProfileRepository {
    List<AdminProfileDTO> findAll();
    List<AdminProfileDTO> findByStatus(AdminProfileStatus status);
    Optional<AdminProfileDTO> findById(long id);
    Optional<AdminProfileDTO> findByAdminId(String adminId);
    AdminProfileDTO save(AdminProfileDTO dto);
    void delete(long id);
}
