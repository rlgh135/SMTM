package com.project.stock.domain.stock.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

/**
 * 주식 시세 동기화 요청 DTO.
 */
public record SyncStockPricesRequest(
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate startDate,

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate endDate
) {
}
