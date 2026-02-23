package com.snippet.controller;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.service.SnippetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestParam List<Long> ids) {
        return ResponseEntity.ok(snippetService.getArchive(ids));
    }
}
