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

    @Transactional(readOnly = true)
    public List<RecordDto> findAll() {
        return snippetRepository.findAllWithBook().stream()
                .map(RecordDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecordDto findById(Long id) {
        Snippet record = snippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        return RecordDto.from(record);
    }

    @Transactional
    public Long create(Long userId, RecordAddRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));
        Book book = bookRepository.findById(requestDto.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + requestDto.getBookId()));

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

    @Transactional
    public RecordDto update(Long id, String type, String text, String tag, Integer relatedPage) {
        Snippet record = snippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        record.update(type, text, tag, relatedPage);
        return RecordDto.from(record);
    }

    @Transactional
    public void delete(Long id) {
        snippetRepository.deleteById(id);
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
    public List<RecordDto> getMonthlyRecords(Long userId, String type, int year, int month) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Snippet> records = (type != null && !type.isBlank())
                ? snippetRepository.findByUserAndTypeAndCreateDateBetweenOrderByCreateDateDesc(user, type, start, end)
                : snippetRepository.findByUserAndCreateDateBetweenOrderByCreateDateDesc(user, start, end);

        return records.stream()
                .map(RecordDto::from)
                .collect(Collectors.toList());
    }
}
