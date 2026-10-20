package com.snippet.service;

import com.snippet.entity.ReadingLog;
import com.snippet.entity.UserBook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/** 일별 읽은 페이지 집계 규칙 테스트. */
class ReadingLogServiceTest {

    private static final LocalDate D1 = LocalDate.of(2026, 10, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 10, 2);

    private final AtomicLong ids = new AtomicLong();

    private UserBook userBook(long id) {
        UserBook ub = UserBook.builder().build();
        ReflectionTestUtils.setField(ub, "id", id);
        return ub;
    }

    private ReadingLog log(UserBook ub, int from, int to, String source, LocalDate date) {
        ReadingLog log = ReadingLog.builder()
                .userBook(ub).fromPage(from).toPage(to).source(source).logDate(date).build();
        ReflectionTestUtils.setField(log, "id", ids.incrementAndGet());
        ReflectionTestUtils.setField(log, "delta", to - from); // @PrePersist 대신
        return log;
    }

    @Test
    @DisplayName("같은 날 여러 번 수정하면 순증가분만 센다")
    void sumsNetPerDay() {
        UserBook a = userBook(1);
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(a, 10, 30, ReadingLog.SOURCE_MANUAL, D1),
                log(a, 30, 50, ReadingLog.SOURCE_SESSION, D1),
                log(a, 50, 80, ReadingLog.SOURCE_MANUAL, D2)));

        assertThat(result).containsExactly(
                Map.entry(D1, 40), Map.entry(D2, 30));
    }

    @Test
    @DisplayName("같은 날 오타를 고치면 상쇄된다")
    void typoCorrectionCancelsOut() {
        UserBook a = userBook(1);
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(a, 50, 500, ReadingLog.SOURCE_MANUAL, D1),
                log(a, 500, 60, ReadingLog.SOURCE_MANUAL, D1)));

        assertThat(result).containsEntry(D1, 10).hasSize(1);
    }

    @Test
    @DisplayName("재독으로 페이지를 되돌린 책은 0으로 잘리고 다른 책 집계를 깎지 않는다")
    void negativeNetIsClampedPerBook() {
        UserBook a = userBook(1);
        UserBook b = userBook(2);
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(a, 300, 0, ReadingLog.SOURCE_MANUAL, D1),
                log(b, 0, 20, ReadingLog.SOURCE_MANUAL, D1)));

        assertThat(result).containsEntry(D1, 20).hasSize(1);
    }

    @Test
    @DisplayName("완독 전환으로 한꺼번에 오른 페이지도 집계에 포함한다")
    void completeJumpIsCounted() {
        UserBook a = userBook(1);
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(a, 0, 300, ReadingLog.SOURCE_COMPLETE, D1)));

        assertThat(result).containsEntry(D1, 300);
    }

    @Test
    @DisplayName("서재에서 삭제돼 userBook이 NULL인 로그도 집계에 남는다")
    void deletedUserBookStillCounted() {
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(null, 0, 15, ReadingLog.SOURCE_MANUAL, D1),
                log(null, 15, 25, ReadingLog.SOURCE_MANUAL, D1)));

        assertThat(result).containsEntry(D1, 25);
    }

    @Test
    @DisplayName("순증가가 없는 날은 결과에서 빠진다")
    void zeroDaysOmitted() {
        UserBook a = userBook(1);
        SortedMap<LocalDate, Integer> result = ReadingLogService.aggregate(List.of(
                log(a, 100, 90, ReadingLog.SOURCE_MANUAL, D1)));

        assertThat(result).isEmpty();
    }
}
