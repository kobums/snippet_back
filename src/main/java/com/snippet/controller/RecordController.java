package com.snippet.controller;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books/{bookId}/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @PostMapping
    public ResponseEntity<Long> addRecord(
            @PathVariable Long bookId,
            @RequestBody RecordAddRequestDto requestDto) {
        Long recordId = recordService.addRecord(bookId, requestDto);
        return ResponseEntity.ok(recordId);
    }

    @GetMapping
    public ResponseEntity<List<RecordDto>> getRecords(
            @PathVariable Long bookId,
            @RequestParam(required = false) String type) {
        List<RecordDto> records = recordService.getRecordsByBook(bookId, type);
        return ResponseEntity.ok(records);
    }
}
