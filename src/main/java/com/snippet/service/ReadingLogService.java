package com.snippet.service;

import com.snippet.dto.DailyPagesDto;
import com.snippet.entity.ReadingLog;
import com.snippet.entity.UserBook;
import com.snippet.repository.ReadingLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingLogService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReadingLogRepository readingLogRepository;

    /** readPage가 실제로 바뀐 경우에만 기록. 오늘(KST) 날짜로 집계된다. */
    public void record(UserBook userBook, int fromPage, int toPage, String source) {
        record(userBook, fromPage, toPage, source, LocalDate.now(KST));
    }

    public void record(UserBook userBook, int fromPage, int toPage, String source, LocalDate logDate) {
        if (fromPage == toPage) return;
        readingLogRepository.save(ReadingLog.builder()
                .user(userBook.getUser())
                .userBook(userBook)
                .book(userBook.getBook())
                .fromPage(fromPage)
                .toPage(toPage)
                .source(source)
                .logDate(logDate)
                .build());
    }

    @Transactional(readOnly = true)
    public List<DailyPagesDto> getDaily(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month는 1~12 사이여야 합니다");
        }
        YearMonth ym = YearMonth.of(year, month);
        List<ReadingLog> logs = readingLogRepository
                .findByUserIdAndLogDateBetween(userId, ym.atDay(1), ym.atEndOfMonth());
        return aggregate(logs).entrySet().stream()
                .map(e -> new DailyPagesDto(e.getKey().toString(), e.getValue()))
                .toList();
    }

    /** 읽은 페이지가 1 이상인 날짜 (스트릭 계산용) */
    @Transactional(readOnly = true)
    public Set<LocalDate> getReadDates(Long userId) {
        return aggregate(readingLogRepository.findAllByUserId(userId)).keySet();
    }

    /**
     * (책, 날짜)별로 delta를 순합산하고 음수는 0으로 잘라 날짜별로 더한다.
     * 같은 날 50 → 500(오타) → 50 처럼 고친 경우 상쇄되고, 재독으로 페이지를 되돌려도
     * 그날 집계가 음수가 되지 않는다. 결과에는 1페이지 이상인 날짜만 담긴다.
     */
    static SortedMap<LocalDate, Integer> aggregate(List<ReadingLog> logs) {
        Map<LocalDate, Map<Object, Integer>> netByDateAndBook = new HashMap<>();
        for (ReadingLog log : logs) {
            // 서재에서 삭제돼 userBook이 NULL이 된 로그는 묶을 기준이 없으므로 행 단위로 취급
            Object bookKey = log.getUserBook() != null
                    ? log.getUserBook().getId()
                    : "log-" + log.getId();
            netByDateAndBook
                    .computeIfAbsent(log.getLogDate(), d -> new HashMap<>())
                    .merge(bookKey, log.getDelta(), Integer::sum);
        }

        SortedMap<LocalDate, Integer> result = new TreeMap<>();
        netByDateAndBook.forEach((date, byBook) -> {
            int pages = byBook.values().stream().mapToInt(net -> Math.max(0, net)).sum();
            if (pages > 0) result.put(date, pages);
        });
        return result;
    }
}
