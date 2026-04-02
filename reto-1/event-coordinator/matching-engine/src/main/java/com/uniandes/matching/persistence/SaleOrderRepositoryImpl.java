package com.uniandes.matching.persistence;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.repository.SaleOrderRepository;
import com.uniandes.matching.persistence.crud.CrudSaleOrderEntity;
import com.uniandes.matching.persistence.entity.SaleOrderEntity;
import com.uniandes.matching.persistence.mapper.SaleOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SaleOrderRepositoryImpl implements SaleOrderRepository {

    private final CrudSaleOrderEntity crudRepository;
    private final SaleOrderMapper mapper;

    public SaleOrderRepositoryImpl(CrudSaleOrderEntity crudRepository,
                                   SaleOrderMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        SaleOrderEntity entity = mapper.toEntity(order);
        SaleOrderEntity saved = crudRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(String id) {
        return crudRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Order> findBySymbol(String symbol) {
        return crudRepository.findBySymbol(symbol).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Order> findAll() {
        return crudRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        crudRepository.deleteById(id);
    }
}