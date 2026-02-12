package com.project.stock.domain.stock.adapter.out.external.kis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * KIS API 일별 시세 조회 응답 DTO.
 */
public record KisOhlcvResponse(
    @JsonProperty("rt_cd")
    String resultCode,

    @JsonProperty("msg_cd")
    String messageCode,

    @JsonProperty("msg1")
    String message,

    @JsonProperty("output1")
    List<DailyPrice> output1,

    @JsonProperty("output2")
    Output2 output2
) {
    public record DailyPrice(
        @JsonProperty("stck_bsop_date")
        String businessDate,  // 영업일자 (YYYYMMDD)

        @JsonProperty("stck_oprc")
        String openPrice,     // 시가

        @JsonProperty("stck_hgpr")
        String highPrice,     // 고가

        @JsonProperty("stck_lwpr")
        String lowPrice,      // 저가

        @JsonProperty("stck_clpr")
        String closePrice,    // 종가

        @JsonProperty("acml_vol")
        String volume,        // 누적 거래량

        @JsonProperty("prdy_vrss")
        String changeAmount,  // 전일 대비

        @JsonProperty("prdy_vrss_sign")
        String changeSign,    // 전일 대비 부호 (1: 상한, 2: 상승, 3: 보합, 4: 하한, 5: 하락)

        @JsonProperty("prdy_ctrt")
        String changeRate     // 전일 대비율 (%)
    ) {
    }

    public record Output2(
        @JsonProperty("stck_prpr")
        String currentPrice,  // 현재가

        @JsonProperty("prdy_vrss")
        String changeAmount,  // 전일 대비

        @JsonProperty("prdy_ctrt")
        String changeRate     // 전일 대비율
    ) {
    }
}
