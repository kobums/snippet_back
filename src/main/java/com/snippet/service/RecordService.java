package com.snippet.service;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.entity.Book;
import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import com.snippet.repository.BookRepository;
import com.snippet.repository.SnippetRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

        private final BookRepository bookRepository;
        private final UserRepository userRepository;
        private final SnippetRepository snippetRepository;

        @Transactional
        public Long addRecord(Long userId, Long bookId, RecordAddRequestDto requestDto) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));
                Book book = bookRepository.findById(bookId)
                                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + bookId));

                Snippet record = Snippet.builder()
                                .book(book)
                                .user(user)
                                .type(requestDto.getType())
                                .text(requestDto.getText())
                                .tag(requestDto.getTag())
                                .relatedPage(requestDto.getRelatedPage())
                                .build();

                return snippetRepository.save(record).getId();
        }

        @Transactional(readOnly = true)
        public List<RecordDto> getRecordsByBook(Long bookId, String type) {
                Book book = bookRepository.findById(bookId)
                                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + bookId));

                List<Snippet> records;
                if (type != null && !type.isEmpty()) {
                        records = snippetRepository.findByBookAndTypeOrderByCreateDateDesc(book, type);
                } else {
                        records = snippetRepository.findByBookOrderByCreateDateDesc(book);
                }

                return records.stream()
                                .map(RecordDto::from)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<RecordDto> getMonthlyRecords(Long userId, String type) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));

                YearMonth currentMonth = YearMonth.now();
                LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
                LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

                List<Snippet> records = snippetRepository
                                .findByUserAndTypeAndCreateDateBetweenOrderByCreateDateDesc(user, type, start, end);

                return records.stream()
                                .map(RecordDto::from)
                                .collect(Collectors.toList());
        }
}
