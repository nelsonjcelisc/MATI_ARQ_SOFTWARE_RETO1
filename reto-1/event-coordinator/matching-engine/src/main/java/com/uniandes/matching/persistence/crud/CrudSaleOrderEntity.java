package com.uniandes.matching.persistence.crud;

import com.uniandes.matching.persistence.entity.SaleOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrudSaleOrderEntity extends JpaRepository<SaleOrderEntity, String> {

    List<SaleOrderEntity> findBySymbol(String symbol);
}