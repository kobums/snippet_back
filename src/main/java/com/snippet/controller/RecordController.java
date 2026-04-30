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
    public ResponseEntity<List<RecordDto>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(recordService.findAllByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordDto> getById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(recordService.findById(id, userId));
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
    public ResponseEntity<RecordDto> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = userDetails.getUser().getId();
        RecordDto updated = recordService.update(id, userId,
                (String) body.get("type"),
                (String) body.get("text"),
                (String) body.get("tag"),
                body.get("relatedPage") != null ? ((Number) body.get("relatedPage")).intValue() : null);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RecordDto> patch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = userDetails.getUser().getId();
        RecordDto updated = recordService.update(id, userId,
                (String) body.get("type"),
                (String) body.get("text"),
                (String) body.get("tag"),
                body.get("relatedPage") != null ? ((Number) body.get("relatedPage")).intValue() : null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        Long userId = userDetails.getUser().getId();
        recordService.delete(id, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/bybook")
    public ResponseEntity<List<RecordDto>> getByBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long bookId,
            @RequestParam(required = false) String type) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(recordService.getRecordsByBook(bookId, userId, type));
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
