package com.snippet.controller;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    private static final String TEMP_USER_ID = "test_user_1";

    @PostMapping("/api/books/{bookId}/records")
    public ResponseEntity<Long> addRecord(
            @PathVariable Long bookId,
            @RequestBody RecordAddRequestDto requestDto) {
        Long recordId = recordService.addRecord(bookId, requestDto);
        return ResponseEntity.ok(recordId);
    }

    @GetMapping("/api/books/{bookId}/records")
    public ResponseEntity<List<RecordDto>> getRecords(
            @PathVariable Long bookId,
            @RequestParam(required = false) String type) {
        List<RecordDto> records = recordService.getRecordsByBook(bookId, type);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/api/records/monthly")
    public ResponseEntity<List<RecordDto>> getMonthlyRecords(
            @RequestParam String type) {
        List<RecordDto> records = recordService.getMonthlyRecords(TEMP_USER_ID, type);
        return ResponseEntity.ok(records);
    }
}
