package com.snippet.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 클라이언트가 보낸 날짜 문자열을 서버 기준 LocalDateTime 으로 바꾼다.
 *
 * <p>허용 형식: {@code 2026-10-22}, {@code 2026-10-22T13:05:00},
 * 오프셋/Z 가 붙은 ISO-8601({@code 2026-10-22T04:05:00.000Z}, {@code 2026-10-22T13:05:00+09:00}).
 * 오프셋이 있으면 서버 시간대로 환산한다 — 앱이 LocalDateTime.now() 로 저장하는 기준과 같다.
 *
 * <p>웹은 new Date().toISOString() 으로 'Z' 를 붙여 보내는데, 예전엔 LocalDateTime.parse 가 이를 거부해
 * 생성 경로에선 조용히 현재 시각으로 대체되고 수정(PATCH) 경로에선 400 이 났다.
 */
public final class ClientDateTimeParser {

    private ClientDateTimeParser() {
    }

    /** @throws DateTimeParseException 어느 형식에도 맞지 않을 때 */
    public static LocalDateTime parse(String value) {
        return parse(value, ZoneId.systemDefault());
    }

    static LocalDateTime parse(String value, ZoneId serverZone) {
        String s = value.trim();
        if (!s.contains("T") && !s.contains(":")) {
            return LocalDate.parse(s).atStartOfDay();
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(s).atZoneSameInstant(serverZone).toLocalDateTime();
        }
    }
}
