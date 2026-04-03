package com.uniandes.admin_api.persistence.crud;

import com.uniandes.admin_api.persistence.entity.VendorCommission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorCommissionCRUD extends JpaRepository<VendorCommission, Long> {

}
