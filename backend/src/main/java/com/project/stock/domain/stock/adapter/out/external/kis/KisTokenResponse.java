package com.project.stock.domain.stock.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * KIS API 토큰 응답 DTO.
 */
public record KisTokenResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("access_token_token_expired")
    String accessTokenExpired,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Integer expiresIn
) {
}
