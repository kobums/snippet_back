package com.snippet.controller;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.SnippetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/snippets")
@RequiredArgsConstructor
public class SnippetController {

    private final SnippetService snippetService;

    @GetMapping("/cards")
    public ResponseEntity<List<SnippetCardDto>> getCards(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) List<Long> excludeIds) {
        return ResponseEntity.ok(snippetService.getCards(count, excludeIds));
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
            @RequestBody Map<String, Long> body) {
        Long userId = userDetails.getUser().getId();
        Long snippetId = body.get("snippetId");
        Long archiveId = snippetService.addArchive(userId, snippetId);
        return ResponseEntity.ok(archiveId);
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
