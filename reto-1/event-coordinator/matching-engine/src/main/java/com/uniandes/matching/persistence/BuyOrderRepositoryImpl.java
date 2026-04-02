package com.uniandes.matching.persistence;

import com.uniandes.matching.domain.model.Order;
import com.uniandes.matching.domain.repository.BuyOrderRepository;
import com.uniandes.matching.persistence.crud.CrudBuyOrderEntity;
import com.uniandes.matching.persistence.entity.BuyOrderEntity;
import com.uniandes.matching.persistence.mapper.BuyOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class BuyOrderRepositoryImpl implements BuyOrderRepository {

    private final CrudBuyOrderEntity crudRepository;
    private final BuyOrderMapper mapper;

    public BuyOrderRepositoryImpl(CrudBuyOrderEntity crudRepository,
                                  BuyOrderMapper mapper) {
        this.crudRepository = crudRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        BuyOrderEntity entity = mapper.toEntity(order);
        BuyOrderEntity saved = crudRepository.save(entity);
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