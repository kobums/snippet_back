package com.snippet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientDateTimeParserTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("날짜만 오면 그날 0시 (iOS·DatePicker 형식)")
    void dateOnly() {
        assertThat(ClientDateTimeParser.parse("2026-10-22", SEOUL)).isEqualTo(LocalDateTime.of(2026, 10, 22, 0, 0));
    }

    @Test
    @DisplayName("오프셋 없는 날짜시각은 그대로 (Android 형식)")
    void localDateTime() {
        assertThat(ClientDateTimeParser.parse("2026-10-22T13:05:00", SEOUL)).isEqualTo(LocalDateTime.of(2026, 10, 22, 13, 5));
    }

    @Test
    @DisplayName("웹 toISOString()의 Z(UTC)는 서버 시간대로 환산한다")
    void utcZulu() {
        assertThat(ClientDateTimeParser.parse("2026-10-22T04:05:00.000Z", SEOUL)).isEqualTo(LocalDateTime.of(2026, 10, 22, 13, 5));
    }

    @Test
    @DisplayName("+09:00 오프셋도 같은 시각")
    void explicitOffset() {
        assertThat(ClientDateTimeParser.parse("2026-10-22T13:05:00+09:00", SEOUL)).isEqualTo(LocalDateTime.of(2026, 10, 22, 13, 5));
    }

    @Test
    @DisplayName("UTC 전날 밤은 서울 기준 다음 날 (날짜 경계)")
    void crossesDateBoundary() {
        assertThat(ClientDateTimeParser.parse("2026-10-21T20:30:00Z", SEOUL)).isEqualTo(LocalDateTime.of(2026, 10, 22, 5, 30));
    }

    @Test
    @DisplayName("형식이 틀리면 DateTimeParseException — 호출부가 기존 정책(생성: 현재 시각, 수정: 400)대로 처리")
    void invalidFormats() {
        assertThatThrownBy(() -> ClientDateTimeParser.parse("10/22/2026 13:05", SEOUL)).isInstanceOf(DateTimeParseException.class);
        assertThatThrownBy(() -> ClientDateTimeParser.parse("2026/10/22", SEOUL)).isInstanceOf(DateTimeParseException.class);
    }
}
