package com.snippet.service;

import com.snippet.entity.Book;
import com.snippet.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + id));
    }

    @Transactional
    public Book create(Book book) {
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, String isbn, String title, String author, String coverUrl,
            String affiliateUrl, String publisher, Integer totalPage, LocalDate publicationDate) {
        Book book = findById(id);
        book.update(isbn, title, author, coverUrl, affiliateUrl, publisher, totalPage, publicationDate);
        return book;
    }

    @Transactional
    public void delete(Long id) {
        bookRepository.deleteById(id);
    }
}
