package com.project.stock.common;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 시간 관련 유틸리티 클래스.
 * 내부/DB: UTC 사용, 표시: KST 변환.
 */
public final class TimeUtil {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(KST);

    private TimeUtil() {
    }

    /**
     * 현재 UTC 시각을 반환합니다.
     */
    public static Instant nowUtc() {
        return Instant.now();
    }

    /**
     * UTC Instant를 KST 문자열로 변환합니다.
     */
    public static String toKstString(Instant instant) {
        return KST_FORMATTER.format(instant);
    }

    /**
     * UTC Instant를 KST ZonedDateTime으로 변환합니다.
     */
    public static ZonedDateTime toKst(Instant instant) {
        return instant.atZone(KST);
    }
}
