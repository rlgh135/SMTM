package com.project.stock.common;

import java.time.Instant;

/**
 * 전역 에러 응답 DTO.
 */
public record ErrorResponse(
        int status,
        String message,
        Instant timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Instant.now());
    }
}
