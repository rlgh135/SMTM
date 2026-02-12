package com.project.stock.domain.stock.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 일별 시세 엔티티.
 * Stock과 1:N 관계 (한 종목은 여러 개의 일별 시세를 가짐).
 */
@Entity
@Table(
    name = "stock_price",
    indexes = {
        @Index(name = "idx_stock_price_stock_date", columnList = "stock_id, date")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockPrice {

    @EmbeddedId
    private StockPriceId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("stockId")
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal closePrice;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "change_rate", precision = 10, scale = 4)
    private BigDecimal changeRate;

    @Builder
    public StockPrice(
        Stock stock,
        LocalDate date,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        BigDecimal changeRate
    ) {
        this.id = new StockPriceId(stock.getId(), date);
        this.stock = stock;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.changeRate = changeRate;
    }

    /**
     * 시세 데이터를 업데이트합니다.
     */
    public void updatePriceData(
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        Long volume,
        BigDecimal changeRate
    ) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.changeRate = changeRate;
    }
}
