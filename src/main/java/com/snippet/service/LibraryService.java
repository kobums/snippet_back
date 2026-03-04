package com.snippet.service;

import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.entity.Book;
import com.snippet.entity.UserBook;
import com.snippet.repository.BookRepository;
import com.snippet.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;

    @Transactional
    public Long addBookToLibrary(String userId, LibraryAddRequestDto request) {
        // 1. Check if Book exists by ISBN, otherwise save new Book
        Book book = bookRepository.findByIsbn(request.getIsbn())
                .orElseGet(() -> {
                    LocalDate pubDate = LocalDate.of(1970, 1, 1);
                    if (request.getPubDate() != null && !request.getPubDate().isEmpty()) {
                        try {
                            // PUBLISH_PREDATE는 yyyyMMdd 형식 (예: 20220509)
                            pubDate = LocalDate.parse(request.getPubDate(),
                                    DateTimeFormatter.ofPattern("yyyyMMdd"));
                        } catch (DateTimeParseException e) {
                            try {
                                // yyyy-MM-dd 형식도 시도
                                pubDate = LocalDate.parse(request.getPubDate());
                            } catch (DateTimeParseException e2) {
                                // 파싱 실패 시 기본값 유지
                            }
                        }
                    }

                    Book newBook = Book.builder()
                            .isbn(request.getIsbn())
                            .title(request.getTitle())
                            .author(request.getAuthor() != null ? request.getAuthor() : "Unknown")
                            .publisher(request.getPublisher())
                            .publicationDate(pubDate)
                            .totalPage(request.getTotalPage())
                            .coverUrl(request.getCoverUrl() != null ? request.getCoverUrl() : "")
                            .affiliateUrl("") // Will be generated later or by affiliate service
                            .build();
                    return bookRepository.save(newBook);
                });

        // 2. Check if UserBook already exists for this user and book
        UserBook userBook = userBookRepository.findByUserIdAndBook(userId, book)
                .orElseGet(() -> {
                    UserBook newUserBook = UserBook.builder()
                            .userId(userId)
                            .book(book)
                            .status(request.getStatus() != null ? request.getStatus() : "wish")
                            .readPage(0)
                            .build();
                    return userBookRepository.save(newUserBook);
                });

        return userBook.getId();
    }

    @Transactional
    public void updateStatus(Long userBookId, String userId, String status) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        userBook.updateStatus(status);
    }

    @Transactional
    public void updateProgress(Long userBookId, String userId, Integer readPage) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        userBook.updateReadPage(readPage);
    }

    @Transactional(readOnly = true)
    public java.util.List<com.snippet.dto.UserBookDto> getUserBooks(String userId) {
        return userBookRepository.findByUserId(userId).stream()
                .map(com.snippet.dto.UserBookDto::from)
                .collect(java.util.stream.Collectors.toList());
    }
}
