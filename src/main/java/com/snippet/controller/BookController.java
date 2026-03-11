package com.snippet.controller;

import com.snippet.dto.BookSearchDto;
import com.snippet.entity.Book;
import com.snippet.service.BookSearchService;
import com.snippet.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookSearchService bookSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<BookSearchDto>> searchBooks(@RequestParam String query) {
        return ResponseEntity.ok(bookSearchService.searchBooks(query));
    }

    @GetMapping
    public ResponseEntity<List<Book>> getAll() {
        return ResponseEntity.ok(bookService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Book> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Book> create(@RequestBody Map<String, Object> body) {
        Book book = Book.builder()
                .isbn((String) body.get("isbn"))
                .title((String) body.get("title"))
                .author((String) body.get("author"))
                .coverUrl((String) body.getOrDefault("coverUrl", ""))
                .affiliateUrl((String) body.getOrDefault("affiliateUrl", ""))
                .publisher((String) body.get("publisher"))
                .totalPage(body.get("totalPage") != null ? ((Number) body.get("totalPage")).intValue() : null)
                .publicationDate(body.get("publicationDate") != null ? LocalDate.parse((String) body.get("publicationDate")) : null)
                .build();
        return ResponseEntity.ok(bookService.create(book));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Book> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Book updated = bookService.update(id,
                (String) body.get("isbn"),
                (String) body.get("title"),
                (String) body.get("author"),
                (String) body.get("coverUrl"),
                (String) body.get("affiliateUrl"),
                (String) body.get("publisher"),
                body.get("totalPage") != null ? ((Number) body.get("totalPage")).intValue() : null,
                body.get("publicationDate") != null ? LocalDate.parse((String) body.get("publicationDate")) : null);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Book> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Book updated = bookService.update(id,
                (String) body.get("isbn"),
                (String) body.get("title"),
                (String) body.get("author"),
                (String) body.get("coverUrl"),
                (String) body.get("affiliateUrl"),
                (String) body.get("publisher"),
                body.get("totalPage") != null ? ((Number) body.get("totalPage")).intValue() : null,
                body.get("publicationDate") != null ? LocalDate.parse((String) body.get("publicationDate")) : null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.ok().build();
    }
}
