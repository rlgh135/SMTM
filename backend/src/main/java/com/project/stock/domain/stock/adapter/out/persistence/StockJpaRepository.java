package com.project.stock.domain.stock.adapter.out.persistence;

import com.project.stock.domain.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 주식 JPA 레포지토리.
 */
public interface StockJpaRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByStockCode(String stockCode);
}
