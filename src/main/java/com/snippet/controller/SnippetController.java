package com.snippet.controller;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetArchiveRequestDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.SnippetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final SnippetService snippetService;

    @GetMapping("/cards")
    public ResponseEntity<List<SnippetCardDto>> getCards(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) List<Long> excludeIds,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getUser().getId() : null;
        return ResponseEntity.ok(snippetService.getCards(count, excludeIds, userId));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<SnippetArchiveDto>> getArchive(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(snippetService.getArchive(userId));
    }

    @PostMapping("/archive")
    public ResponseEntity<Long> addArchive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SnippetArchiveRequestDto request) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(snippetService.addArchive(userId, request.getSnippetId()));
    }

    @DeleteMapping("/archive/{snippetId}")
    public ResponseEntity<Void> removeArchive(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long snippetId) {
        Long userId = userDetails.getUser().getId();
        snippetService.removeArchive(userId, snippetId);
        return ResponseEntity.ok().build();
    }
}
