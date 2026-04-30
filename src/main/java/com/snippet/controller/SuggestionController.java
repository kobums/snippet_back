package com.snippet.controller;

import com.snippet.dto.SuggestionAddRequestDto;
import com.snippet.dto.SuggestionDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.SuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<SuggestionDto> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SuggestionAddRequestDto dto) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(suggestionService.create(userId, dto));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SuggestionDto>> getMine(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(suggestionService.findByUser(userId));
    }
}
