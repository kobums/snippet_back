package com.snippet.controller;

import com.snippet.dto.ReadingSessionAddRequestDto;
import com.snippet.dto.ReadingSessionDto;
import com.snippet.dto.ReadingSessionStatsDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.ReadingSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readingsessions")
@RequiredArgsConstructor
public class ReadingSessionController {

    private final ReadingSessionService sessionService;

    @GetMapping
    public ResponseEntity<List<ReadingSessionDto>> getAll(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                sessionService.getAll(userDetails.getUser().getId()));
    }

    @PostMapping
    public ResponseEntity<Long> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ReadingSessionAddRequestDto dto) {
        Long id = sessionService.create(userDetails.getUser().getId(), dto);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/bybook")
    public ResponseEntity<List<ReadingSessionDto>> getByBook(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long userBookId) {
        return ResponseEntity.ok(
                sessionService.getByBook(userDetails.getUser().getId(), userBookId));
    }

    @GetMapping("/stats")
    public ResponseEntity<ReadingSessionStatsDto> getStats(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long userBookId) {
        return ResponseEntity.ok(
                sessionService.getStats(userDetails.getUser().getId(), userBookId));
    }
}
