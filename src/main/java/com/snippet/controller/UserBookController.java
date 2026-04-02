package com.snippet.controller;

import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.dto.UserBookDto;
import com.snippet.dto.MonthlyStatsDto;
import com.snippet.dto.YearlyStatsDto;
import com.snippet.dto.CategoryStatsDto;
import com.snippet.dto.ReadingInsightsDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.UserBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/userbooks")
@RequiredArgsConstructor
public class UserBookController {

    private final UserBookService userBookService;

    @GetMapping
    public ResponseEntity<List<UserBookDto>> getAll() {
        return ResponseEntity.ok(userBookService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserBookDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userBookService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Long> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody LibraryAddRequestDto requestDto) {
        Long userId = userDetails.getUser().getId();
        Long userBookId = userBookService.create(userId, requestDto);
        return ResponseEntity.ok(userBookId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserBookDto> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = userDetails.getUser().getId();
        UserBookDto updated = userBookService.update(id, userId,
                (String) body.get("type"),
                (String) body.get("status"),
                body.get("readPage") != null ? ((Number) body.get("readPage")).intValue() : null,
                (String) body.get("startDate"),
                (String) body.get("endDate"));
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserBookDto> patch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = userDetails.getUser().getId();
        UserBookDto updated = userBookService.update(id, userId,
                (String) body.get("type"),
                (String) body.get("status"),
                body.get("readPage") != null ? ((Number) body.get("readPage")).intValue() : null,
                (String) body.get("startDate"),
                (String) body.get("endDate"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userBookService.delete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 해당 월에 활동이 있는 책 목록 조회 (활동 기간 겹침 방식)
     * 파라미터 미입력 시 현재 월 기본값 사용
     */
    @GetMapping("/monthly")
    public ResponseEntity<List<UserBookDto>> getMonthly(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = userDetails.getUser().getId();
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();
        return ResponseEntity.ok(userBookService.getUserBooksByMonth(userId, ym.getYear(), ym.getMonthValue()));
    }

    /**
     * 대시보드 진행 탭용 책 목록 조회
     * - waiting, reading: 날짜 무관 전체 조회
     * - completed: 해당 월의 완독만 조회
     * 파라미터 미입력 시 현재 월 기본값 사용
     */
    @GetMapping("/progress")
    public ResponseEntity<List<UserBookDto>> getProgress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = userDetails.getUser().getId();
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();
        return ResponseEntity.ok(userBookService.getProgressBooks(userId, ym.getYear(), ym.getMonthValue()));
    }

    /**
     * 페이지네이션 지원 - 서재용 전체 책 목록 조회 (최신순)
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserBookDto>> getAllPaginated(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(userBookService.getUserBooksPaginated(userId, page, size));
    }

    // ==================== 통계 API ====================

    /**
     * 월별 통계 조회
     */
    @GetMapping("/stats/monthly")
    public ResponseEntity<List<MonthlyStatsDto>> getMonthlyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        Long userId = userDetails.getUser().getId();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(userBookService.getMonthlyStats(userId, targetYear));
    }

    /**
     * 연도별 통계 조회
     */
    @GetMapping("/stats/yearly")
    public ResponseEntity<List<YearlyStatsDto>> getYearlyStats(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(userBookService.getYearlyStats(userId));
    }

    /**
     * 카테고리별 통계 조회
     */
    @GetMapping("/stats/category")
    public ResponseEntity<List<CategoryStatsDto>> getCategoryStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        Long userId = userDetails.getUser().getId();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(userBookService.getCategoryStats(userId, targetYear));
    }

    /**
     * 독서 인사이트 조회
     */
    @GetMapping("/stats/insights")
    public ResponseEntity<ReadingInsightsDto> getReadingInsights(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        Long userId = userDetails.getUser().getId();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(userBookService.getReadingInsights(userId, targetYear));
    }
}
