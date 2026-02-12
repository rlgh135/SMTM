package com.project.stock.domain.stock.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 시세 응답 DTO.
 */
public record StockPriceResponse(
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate date,

    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    Long volume,
    BigDecimal changeRate
) {
}
