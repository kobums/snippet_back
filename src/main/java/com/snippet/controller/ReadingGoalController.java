package com.snippet.controller;

import com.snippet.dto.ReadingGoalDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.ReadingGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/readinggoals")
@RequiredArgsConstructor
public class ReadingGoalController {

    private final ReadingGoalService goalService;

    @GetMapping
    public ResponseEntity<ReadingGoalDto> getGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                goalService.getGoal(userDetails.getUser().getId(), targetYear));
    }

    @PutMapping
    public ResponseEntity<ReadingGoalDto> setGoal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody Map<String, Integer> body) {
        int year = body.getOrDefault("year", LocalDate.now().getYear());
        int targetBooks = body.getOrDefault("targetBooks", 12);
        return ResponseEntity.ok(
                goalService.setGoal(userDetails.getUser().getId(), year, targetBooks));
    }
}
