package com.snippet.controller;

import com.snippet.dto.StatsDto;
import com.snippet.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/library/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<StatsDto> getStats() {
        String mockUserId = "testUser123"; // Using mock user for MVP
        StatsDto stats = statsService.getUserStats(mockUserId);
        return ResponseEntity.ok(stats);
    }
}
