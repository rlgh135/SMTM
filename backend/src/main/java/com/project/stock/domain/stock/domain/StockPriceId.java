package com.project.stock.domain.stock.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * StockPrice 복합 키.
 * stock_id + date를 PK로 사용.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class StockPriceId implements Serializable {

    private Long stockId;

    private LocalDate date;

    public StockPriceId(Long stockId, LocalDate date) {
        this.stockId = stockId;
        this.date = date;
    }
}
