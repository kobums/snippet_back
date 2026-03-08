package com.snippet.service;

import com.snippet.dto.LibraryAddRequestDto;
import com.snippet.entity.Book;
import com.snippet.entity.UserBook;
import com.snippet.repository.BookRepository;
import com.snippet.repository.UserBookRepository;
import com.snippet.repository.UserRepository;
import com.snippet.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;
    private final BookSearchService bookSearchService;

    @Transactional
    public Long addBookToLibrary(Long userId, LibraryAddRequestDto request) {
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

                    String parsedAuthor = "Unknown";
                    if (request.getAuthor() != null && !request.getAuthor().trim().isEmpty()) {
                        String fullAuthor = request.getAuthor();
                        // "저자: " 로 시작하는 첫 번째 항목 파싱
                        // (예: "저자: 헨리 데이비드 소로, 옮긴이: 강승영" -> "헨리 데이비드 소로")
                        String[] parts = fullAuthor.split(",");
                        if (parts.length > 0) {
                            String firstPart = parts[0].trim();
                            if (firstPart.startsWith("저자:")) {
                                parsedAuthor = firstPart.substring(3).trim();
                            } else {
                                parsedAuthor = firstPart;
                            }
                        }
                    }

                    int pages = (request.getTotalPage() != null) ? request.getTotalPage() : 0;
                    if (pages == 0 && request.getIsbn() != null && !request.getIsbn().isEmpty()) {
                        pages = bookSearchService.getBookPageFromAladin(request.getIsbn());
                    }

                    Book newBook = Book.builder()
                            .isbn(request.getIsbn() != null && !request.getIsbn().isEmpty() ? request.getIsbn() : "")
                            .title(request.getTitle() != null ? request.getTitle() : "Unknown")
                            .author(parsedAuthor)
                            .publisher(request.getPublisher() != null ? request.getPublisher() : "")
                            .publicationDate(pubDate)
                            .totalPage(pages)
                            .coverUrl(request.getCoverUrl() != null ? request.getCoverUrl() : "")
                            .affiliateUrl("") // Will be generated later or by affiliate service
                            .build();
                    return bookRepository.save(newBook);
                });

        // 2. Check if UserBook already exists for this user and book
        UserBook userBook = userBookRepository.findByUser_IdAndBook(userId, book)
                .orElseGet(() -> {
                    LocalDateTime startDate = LocalDateTime.now();
                    LocalDateTime endDate = LocalDateTime.now();
                    if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                        try {
                            if (request.getStartDate().contains("T") || request.getStartDate().contains(":")) {
                                startDate = LocalDateTime.parse(request.getStartDate());
                            } else {
                                startDate = LocalDate.parse(request.getStartDate()).atStartOfDay();
                            }
                        } catch (DateTimeParseException e) {
                            // ignore
                        }
                    }
                    if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                        try {
                            if (request.getEndDate().contains("T") || request.getEndDate().contains(":")) {
                                endDate = LocalDateTime.parse(request.getEndDate());
                            } else {
                                endDate = LocalDate.parse(request.getEndDate()).atStartOfDay();
                            }
                        } catch (DateTimeParseException e) {
                            // ignore
                        }
                    }

                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                    UserBook newUserBook = UserBook.builder()
                            .user(user)
                            .book(book)
                            .type(request.getType() != null ? request.getType() : "wish")
                            .status(request.getStatus() != null ? request.getStatus() : "waiting")
                            .readPage(0)
                            .startDate(startDate)
                            .endDate(endDate)
                            .build();
                    return userBookRepository.save(newUserBook);
                });

        return userBook.getId();
    }

    @Transactional
    public void updateStatus(Long userBookId, Long userId, String status) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        userBook.updateStatus(status);
    }

    @Transactional
    public void updateType(Long userBookId, Long userId, String type) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        userBook.updateType(type);
    }

    @Transactional
    public void updateProgress(Long userBookId, Long userId, Integer readPage) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        userBook.updateReadPage(readPage);
    }

    @Transactional
    public void updateStartDate(Long userBookId, Long userId, String startDateStr) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                LocalDateTime startDate;
                if (startDateStr.contains("T") || startDateStr.contains(":")) {
                    startDate = LocalDateTime.parse(startDateStr);
                } else {
                    startDate = LocalDate.parse(startDateStr).atStartOfDay();
                }
                userBook.updateStartDate(startDate);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format");
            }
        }
    }

    @Transactional
    public void updateEndDate(Long userBookId, Long userId, String endDateStr) {
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found"));

        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                LocalDateTime endDate;
                if (endDateStr.contains("T") || endDateStr.contains(":")) {
                    endDate = LocalDateTime.parse(endDateStr);
                } else {
                    endDate = LocalDate.parse(endDateStr).atStartOfDay();
                }
                userBook.updateEndDate(endDate);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format");
            }
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<com.snippet.dto.UserBookDto> getUserBooks(Long userId) {
        return userBookRepository.findByUser_Id(userId).stream()
                .map(com.snippet.dto.UserBookDto::from)
                .collect(java.util.stream.Collectors.toList());
    }
}
