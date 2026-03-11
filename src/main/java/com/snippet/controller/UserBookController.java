package com.snippet.controller;

import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.dto.UserBookDto;
import com.snippet.service.UserBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/userbooks")
@RequiredArgsConstructor
public class UserBookController {

    private final UserBookService userBookService;

    private static final Long TEMP_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<List<UserBookDto>> getAll() {
        return ResponseEntity.ok(userBookService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserBookDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userBookService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody LibraryAddRequestDto requestDto) {
        Long userBookId = userBookService.create(TEMP_USER_ID, requestDto);
        return ResponseEntity.ok(userBookId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserBookDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserBookDto updated = userBookService.update(id, TEMP_USER_ID,
                (String) body.get("type"),
                (String) body.get("status"),
                body.get("readPage") != null ? ((Number) body.get("readPage")).intValue() : null,
                (String) body.get("startDate"),
                (String) body.get("endDate"));
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserBookDto> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserBookDto updated = userBookService.update(id, TEMP_USER_ID,
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
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();
        return ResponseEntity.ok(userBookService.getUserBooksByMonth(TEMP_USER_ID, ym.getYear(), ym.getMonthValue()));
    }
}
