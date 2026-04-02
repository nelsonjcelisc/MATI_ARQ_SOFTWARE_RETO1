package com.uniandes.matching.persistence.crud;

import com.uniandes.matching.persistence.entity.BuyOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrudBuyOrderEntity extends JpaRepository<BuyOrderEntity, String> {

    List<BuyOrderEntity> findBySymbol(String symbol);
}