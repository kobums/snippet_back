package com.snippet.controller;

import com.snippet.dto.BookSearchDto;
import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.service.BookSearchService;
import com.snippet.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final BookSearchService bookSearchService;
    private final LibraryService libraryService;

    // TODO: Get actual User ID from session or token (Hardcoded 1L for MVP)
    private static final Long TEMP_USER_ID = 1L;

    @GetMapping("/search")
    public ResponseEntity<List<BookSearchDto>> searchBooks(@RequestParam String query) {
        List<BookSearchDto> results = bookSearchService.searchBooks(query);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/add")
    public ResponseEntity<Long> addBookToLibrary(@RequestBody LibraryAddRequestDto requestDto) {
        Long userBookId = libraryService.addBookToLibrary(TEMP_USER_ID, requestDto);
        return ResponseEntity.ok(userBookId);
    }

    @GetMapping
    public ResponseEntity<List<com.snippet.dto.UserBookDto>> getUserBooks() {
        List<com.snippet.dto.UserBookDto> results = libraryService.getUserBooks(TEMP_USER_ID);
        return ResponseEntity.ok(results);
    }

    @PatchMapping("/{userBookId}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long userBookId,
            @RequestBody com.snippet.dto.StatusUpdateRequestDto requestDto) {
        libraryService.updateStatus(userBookId, TEMP_USER_ID, requestDto.getStatus());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userBookId}/type")
    public ResponseEntity<Void> updateType(
            @PathVariable Long userBookId,
            @RequestBody com.snippet.dto.TypeUpdateRequestDto requestDto) {
        libraryService.updateType(userBookId, TEMP_USER_ID, requestDto.getType());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userBookId}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long userBookId,
            @RequestBody com.snippet.dto.ProgressUpdateRequestDto requestDto) {
        libraryService.updateProgress(userBookId, TEMP_USER_ID, requestDto.getReadPage());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userBookId}/startdate")
    public ResponseEntity<Void> updateStartDate(
            @PathVariable Long userBookId,
            @RequestBody com.snippet.dto.DateUpdateRequestDto requestDto) {
        libraryService.updateStartDate(userBookId, TEMP_USER_ID, requestDto.getStartDate());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userBookId}/enddate")
    public ResponseEntity<Void> updateEndDate(
            @PathVariable Long userBookId,
            @RequestBody com.snippet.dto.DateUpdateRequestDto requestDto) {
        libraryService.updateEndDate(userBookId, TEMP_USER_ID, requestDto.getEndDate());
        return ResponseEntity.ok().build();
    }
}
