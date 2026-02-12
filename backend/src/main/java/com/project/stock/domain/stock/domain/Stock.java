package com.project.stock.domain.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 주식 종목 엔티티.
 * @Setter 사용 금지 - 상태 변경은 명시적 메서드로 수행.
 */
@Entity
@Table(name = "stock")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stock_code", nullable = false, unique = true, length = 10)
    private String stockCode;

    @Column(name = "stock_name", nullable = false, length = 100)
    private String stockName;

    @Column(name = "market", nullable = false, length = 10)
    private String market;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public Stock(String stockCode, String stockName, String market, BigDecimal currentPrice) {
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.market = market;
        this.currentPrice = currentPrice;
        this.updatedAt = Instant.now();
    }

    /**
     * 현재가를 업데이트합니다.
     */
    public void updatePrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        this.updatedAt = Instant.now();
    }
}
