package com.snippet.controller;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping
    public ResponseEntity<List<RecordDto>> getAll() {
        return ResponseEntity.ok(recordService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Long> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RecordAddRequestDto requestDto) {
        Long userId = userDetails.getUser().getId();
        Long recordId = recordService.create(userId, requestDto);
        return ResponseEntity.ok(recordId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecordDto> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        RecordDto updated = recordService.update(id,
                (String) body.get("type"),
                (String) body.get("text"),
                (String) body.get("tag"),
                body.get("relatedPage") != null ? ((Number) body.get("relatedPage")).intValue() : null);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RecordDto> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        RecordDto updated = recordService.update(id,
                (String) body.get("type"),
                (String) body.get("text"),
                (String) body.get("tag"),
                body.get("relatedPage") != null ? ((Number) body.get("relatedPage")).intValue() : null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bybook")
    public ResponseEntity<List<RecordDto>> getByBook(
            @RequestParam Long bookId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(recordService.getRecordsByBook(bookId, type));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<RecordDto>> getMonthly(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        Long userId = userDetails.getUser().getId();
        java.time.YearMonth ym = (year != null && month != null)
                ? java.time.YearMonth.of(year, month)
                : java.time.YearMonth.now();
        return ResponseEntity.ok(recordService.getMonthlyRecords(userId, type, ym.getYear(), ym.getMonthValue()));
    }
}
