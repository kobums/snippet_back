package com.snippet.controller;

import com.snippet.dto.DailyPagesDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.ReadingLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readinglogs")
@RequiredArgsConstructor
public class ReadingLogController {

    private final ReadingLogService readingLogService;

    @GetMapping("/daily")
    public ResponseEntity<List<DailyPagesDto>> getDaily(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(
                readingLogService.getDaily(userDetails.getUser().getId(), year, month));
    }
}
