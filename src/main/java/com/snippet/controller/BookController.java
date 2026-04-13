package com.snippet.controller;

import com.snippet.dto.BookSearchDto;
import com.snippet.dto.PopularBookDto;
import com.snippet.entity.Book;
import com.snippet.service.BookSearchService;
import com.snippet.service.BookService;
import com.snippet.service.PopularBookService;
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
    private final PopularBookService popularBookService;

    @GetMapping("/popular")
    public ResponseEntity<List<PopularBookDto>> getPopularBooks(
            @RequestParam(defaultValue = "") String startDt,
            @RequestParam(defaultValue = "") String endDt,
            @RequestParam(defaultValue = "") String kdc,
            @RequestParam(defaultValue = "") String dtlKdc,
            @RequestParam(defaultValue = "") String age,
            @RequestParam(defaultValue = "") String gender,
            @RequestParam(defaultValue = "") String region,
            @RequestParam(defaultValue = "") String dtlRegion,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(popularBookService.getPopularBooks(
                startDt, endDt, kdc, dtlKdc, age, gender, region, dtlRegion, pageNo, pageSize));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookSearchDto>> searchBooks(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(bookSearchService.searchBooks(query, page));
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
                .category((String) body.get("category"))
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
                body.get("publicationDate") != null ? LocalDate.parse((String) body.get("publicationDate")) : null,
                (String) body.get("category"));
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
                body.get("publicationDate") != null ? LocalDate.parse((String) body.get("publicationDate")) : null,
                (String) body.get("category"));
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.ok().build();
    }
}
