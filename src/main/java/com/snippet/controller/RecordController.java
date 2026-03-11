package com.snippet.controller;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    private static final Long TEMP_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<List<RecordDto>> getAll() {
        return ResponseEntity.ok(recordService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Long> create(@RequestBody RecordAddRequestDto requestDto) {
        Long recordId = recordService.create(TEMP_USER_ID, requestDto);
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
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        java.time.YearMonth ym = (year != null && month != null)
                ? java.time.YearMonth.of(year, month)
                : java.time.YearMonth.now();
        return ResponseEntity.ok(recordService.getMonthlyRecords(TEMP_USER_ID, type, ym.getYear(), ym.getMonthValue()));
    }
}
