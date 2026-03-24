package com.snippet.service;

import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.dto.UserBookDto;
import com.snippet.dto.MonthlyStatsDto;
import com.snippet.dto.YearlyStatsDto;
import com.snippet.dto.CategoryStatsDto;
import com.snippet.dto.ReadingInsightsDto;
import com.snippet.entity.Book;
import com.snippet.entity.User;
import com.snippet.entity.UserBook;
import com.snippet.repository.BookRepository;
import com.snippet.repository.UserBookRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class UserBookService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;
    private final BookSearchService bookSearchService;

    @Transactional(readOnly = true)
    public List<UserBookDto> findAll() {
        return userBookRepository.findAll().stream()
                .map(UserBookDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserBookDto findById(Long id) {
        UserBook userBook = userBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found: " + id));
        return UserBookDto.from(userBook);
    }

    @Transactional
    public Long create(Long userId, LibraryAddRequestDto request) {
        Book book = bookRepository.findByIsbn(request.getIsbn())
                .orElseGet(() -> {
                    LocalDate pubDate = LocalDate.of(1970, 1, 1);
                    if (request.getPubDate() != null && !request.getPubDate().isEmpty()) {
                        try {
                            pubDate = LocalDate.parse(request.getPubDate(),
                                    DateTimeFormatter.ofPattern("yyyyMMdd"));
                        } catch (DateTimeParseException e) {
                            try {
                                pubDate = LocalDate.parse(request.getPubDate());
                            } catch (DateTimeParseException e2) {
                                // 파싱 실패 시 기본값 유지
                            }
                        }
                    }

                    String parsedAuthor = "Unknown";
                    if (request.getAuthor() != null && !request.getAuthor().trim().isEmpty()) {
                        String fullAuthor = request.getAuthor();
                        String[] parts = fullAuthor.split(",");
                        if (parts.length > 0) {
                            String firstPart = parts[0].trim();
                            if (firstPart.startsWith("저자:")) {
                                parsedAuthor = firstPart.substring(3).trim();
                            } else {
                                parsedAuthor = firstPart;
                            }
                        }
                    }

                    int pages = (request.getTotalPage() != null) ? request.getTotalPage() : 0;
                    if (pages == 0 && request.getIsbn() != null && !request.getIsbn().isEmpty()) {
                        pages = bookSearchService.getBookPageFromAladin(request.getIsbn());
                    }

                    Book newBook = Book.builder()
                            .isbn(request.getIsbn() != null && !request.getIsbn().isEmpty() ? request.getIsbn() : "")
                            .title(request.getTitle() != null ? request.getTitle() : "Unknown")
                            .author(parsedAuthor)
                            .publisher(request.getPublisher() != null ? request.getPublisher() : "")
                            .publicationDate(pubDate)
                            .totalPage(pages)
                            .coverUrl(request.getCoverUrl() != null ? request.getCoverUrl() : "")
                            .affiliateUrl("")
                            .build();
                    return bookRepository.save(newBook);
                });

        UserBook userBook = userBookRepository.findByUser_IdAndBook(userId, book)
                .orElseGet(() -> {
                    LocalDateTime startDate = LocalDateTime.now();
                    LocalDateTime endDate = LocalDateTime.now();
                    if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                        try {
                            if (request.getStartDate().contains("T") || request.getStartDate().contains(":")) {
                                startDate = LocalDateTime.parse(request.getStartDate());
                            } else {
                                startDate = LocalDate.parse(request.getStartDate()).atStartOfDay();
                            }
                        } catch (DateTimeParseException e) {
                            // ignore
                        }
                    }
                    if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                        try {
                            if (request.getEndDate().contains("T") || request.getEndDate().contains(":")) {
                                endDate = LocalDateTime.parse(request.getEndDate());
                            } else {
                                endDate = LocalDate.parse(request.getEndDate()).atStartOfDay();
                            }
                        } catch (DateTimeParseException e) {
                            // ignore
                        }
                    }

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                    // wish 타입은 status를 none으로 강제, 그 외엔 요청값 또는 waiting 기본값
                    String resolvedStatus;
                    if ("wish".equals(request.getType())) {
                        resolvedStatus = "none";
                    } else {
                        resolvedStatus = request.getStatus() != null ? request.getStatus() : "waiting";
                    }

                    UserBook newUserBook = UserBook.builder()
                            .user(user)
                            .book(book)
                            .type(request.getType() != null ? request.getType() : "wish")
                            .status(resolvedStatus)
                            .readPage(0)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();
                    return userBookRepository.save(newUserBook);
                });

        return userBook.getId();
    }

    @Caching(evict = {
        @CacheEvict(value = "monthlyStats", allEntries = true),
        @CacheEvict(value = "yearlyStats", key = "#userId"),
        @CacheEvict(value = "categoryStats", allEntries = true),
        @CacheEvict(value = "readingInsights", allEntries = true)
    })
    @Transactional
    public UserBookDto update(Long id, Long userId, String type, String status,
            Integer readPage, String startDateStr, String endDateStr) {
        UserBook userBook = userBookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found: " + id));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (type != null) {
            userBook.updateType(type);
            // 반납 완료: status를 completed로, endDate를 현재 시각으로 자동 설정
            if ("return".equals(type)) {
                userBook.updateStatus("completed");
                userBook.updateEndDate(LocalDateTime.now());
            }
            // wish로 변경: status를 none으로 강제
            if ("wish".equals(type)) {
                userBook.updateStatus("none");
            }
        }
        if (status != null) {
            userBook.updateStatus(status);
            if ("completed".equals(status) || "dropped".equals(status)) {
                userBook.updateEndDate(LocalDateTime.now());
                if ("completed".equals(status)) {
                    Integer totalPage = userBook.getBook().getTotalPage();
                    if (totalPage != null) {
                        userBook.updateReadPage(totalPage);
                    }
                }
            }
        }
        if (readPage != null) userBook.updateReadPage(readPage);
        if (startDateStr != null) {
            userBook.updateStartDate(parseDateTime(startDateStr));
        }
        if (endDateStr != null) {
            userBook.updateEndDate(parseDateTime(endDateStr));
        }

        return UserBookDto.from(userBook);
    }

    @Caching(evict = {
        @CacheEvict(value = "monthlyStats", allEntries = true),
        @CacheEvict(value = "yearlyStats", allEntries = true),
        @CacheEvict(value = "categoryStats", allEntries = true),
        @CacheEvict(value = "readingInsights", allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        userBookRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserBookDto> getUserBooks(Long userId) {
        return userBookRepository.findByUser_Id(userId).stream()
                .map(UserBookDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 해당 월에 활동이 있는 책 조회 (활동 기간 겹침 방식)
     *
     * - reading/completed/dropped: startDate ~ endDate 가 해당 월과 겹치면 포함
     * - waiting: createDate가 해당 월 안에 있으면 포함
     */
    @Transactional(readOnly = true)
    public List<UserBookDto> getUserBooksByMonth(Long userId, int year, int month) {
        java.time.YearMonth yearMonth = java.time.YearMonth.of(year, month);
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 활동 기간이 겹치는 책 (reading/completed/dropped)
        List<UserBook> overlapping = userBookRepository.findByUser_IdAndDateOverlap(userId, monthStart, monthEnd);

        // 해당 월에 생성된 대기중 책
        List<UserBook> waitingInMonth = userBookRepository.findByUser_IdAndWaitingInMonth(userId, monthStart, monthEnd);

        return java.util.stream.Stream.concat(overlapping.stream(), waitingInMonth.stream())
                .distinct()
                .map(UserBookDto::from)
                .collect(Collectors.toList());
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            if (dateStr.contains("T") || dateStr.contains(":")) {
                return LocalDateTime.parse(dateStr);
            } else {
                return LocalDate.parse(dateStr).atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    // ==================== 통계 메서드 ====================

    /**
     * 월별 통계 조회 (통합 쿼리 사용 - 3회 → 1회)
     * 캐시 적용: 같은 사용자/연도 조합은 1시간 동안 캐시
     */
    @Cacheable(value = "monthlyStats", key = "#userId + '_' + #year")
    @Transactional(readOnly = true)
    public List<MonthlyStatsDto> getMonthlyStats(Long userId, int year) {
        // 1~12월 초기화
        Map<Integer, MonthlyStatsDto> statsMap = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            statsMap.put(i, new MonthlyStatsDto(i, 0, 0, new HashMap<>()));
        }

        // 통합 쿼리로 한 번에 조회 (월별 완료 권수 + 페이지 수 + 카테고리별 권수)
        // 결과: [month, completedCount, totalPages, category, categoryCount]
        List<Object[]> results = userBookRepository.findMonthlyStatsIntegrated(userId, year);

        // 월별로 집계된 데이터를 먼저 초기화
        Map<Integer, Integer> monthCompletedMap = new HashMap<>();
        Map<Integer, Long> monthPagesMap = new HashMap<>();

        for (Object[] row : results) {
            int month = (int) row[0];
            int completedCount = ((Long) row[1]).intValue();
            long totalPages = row[2] != null ? ((Number) row[2]).longValue() : 0;
            String category = (String) row[3];
            int categoryCount = ((Long) row[4]).intValue();

            // 월별 완료 권수와 페이지는 카테고리별로 중복 집계되므로,
            // 카테고리 null 체크 없이 합산 (카테고리별로 나뉜 값들의 합)
            monthCompletedMap.merge(month, completedCount, (a, b) -> a + b);
            monthPagesMap.merge(month, totalPages, (a, b) -> a + b);

            // 카테고리별 권수 저장
            if (category != null) {
                statsMap.get(month).getCategoryCount().put(category, categoryCount);
            }
        }

        // 월별 집계값 설정
        monthCompletedMap.forEach((month, count) -> {
            statsMap.get(month).setCompletedCount(count);
        });
        monthPagesMap.forEach((month, pages) -> {
            statsMap.get(month).setTotalPages(pages.intValue());
        });

        return new ArrayList<>(statsMap.values()).stream()
                .sorted(Comparator.comparingInt(MonthlyStatsDto::getMonth))
                .collect(Collectors.toList());
    }

    /**
     * 연도별 통계 조회
     * 캐시 적용: 사용자별로 캐시
     */
    @Cacheable(value = "yearlyStats", key = "#userId")
    @Transactional(readOnly = true)
    public List<YearlyStatsDto> getYearlyStats(Long userId) {
        List<Object[]> results = userBookRepository.findYearlyStats(userId);
        return results.stream()
                .map(row -> new YearlyStatsDto(
                        (int) row[0],
                        ((Long) row[1]).intValue(),
                        row[2] != null ? ((Number) row[2]).intValue() : 0
                ))
                .collect(Collectors.toList());
    }

    /**
     * 카테고리별 통계 조회
     * 캐시 적용: 같은 사용자/연도 조합은 캐시
     */
    @Cacheable(value = "categoryStats", key = "#userId + '_' + #year")
    @Transactional(readOnly = true)
    public List<CategoryStatsDto> getCategoryStats(Long userId, int year) {
        List<Object[]> results = userBookRepository.findCategoryStats(userId, year);
        return results.stream()
                .map(row -> {
                    String category = (String) row[0];
                    int totalCount = ((Long) row[1]).intValue();
                    int completedCount = ((Long) row[2]).intValue();
                    double completionRate = totalCount > 0 ? (completedCount * 100.0 / totalCount) : 0.0;
                    return new CategoryStatsDto(category, totalCount, completedCount, completionRate);
                })
                .collect(Collectors.toList());
    }

    /**
     * 독서 인사이트 조회
     * 캐시 적용: 같은 사용자/연도 조합은 캐시
     */
    @Cacheable(value = "readingInsights", key = "#userId + '_' + #year")
    @Transactional(readOnly = true)
    public ReadingInsightsDto getReadingInsights(Long userId, int year) {
        Double avgDays = userBookRepository.findAverageReadingDays(userId, year);

        // 가장 많이 읽은 장르
        List<CategoryStatsDto> categoryStats = getCategoryStats(userId, year);
        String topCategory = categoryStats.stream()
                .max(Comparator.comparingInt(CategoryStatsDto::getCompletedCount))
                .map(CategoryStatsDto::getCategory)
                .orElse("없음");

        // 최장 독서 기록
        List<UserBook> longestBooks = userBookRepository.findLongestReadingBook(userId, year, 1);
        int longestDays = 0;
        String longestBook = "없음";
        if (!longestBooks.isEmpty()) {
            UserBook book = longestBooks.get(0);
            longestDays = (int) ChronoUnit.DAYS.between(book.getStartDate(), book.getEndDate());
            longestBook = book.getBook().getTitle();
        }

        return new ReadingInsightsDto(
                avgDays != null ? avgDays : 0.0,
                topCategory,
                longestDays,
                longestBook
        );
    }
}
