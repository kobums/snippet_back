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
    public List<RecordDto> findAllByUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));
        return snippetRepository.findByUserWithBook(user).stream()
                .map(RecordDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecordDto findById(Long id, Long userId) {
        Snippet record = snippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        if (!record.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return RecordDto.from(record);
    }

    @Transactional
    public Long create(Long userId, RecordAddRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));
        Book book = bookRepository.findById(requestDto.getBookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + requestDto.getBookId()));
        validateRelatedPage(requestDto.getRelatedPage(), book);

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
    public RecordDto update(Long id, Long userId, String type, String text, String tag, Integer relatedPage) {
        Snippet record = snippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        if (!record.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        validateRelatedPage(relatedPage, record.getBook());
        record.update(type, text, tag, relatedPage);
        return RecordDto.from(record);
    }

    /**
     * 관련 페이지가 책 범위를 벗어나면 400.
     * 클라이언트별 폼 검증이 제각각이라(등록 모달엔 있고 수정·기록 페이지엔 누락)
     * 서버를 단일 검증 지점으로 둔다. totalPage 미등록(null/0) 책은 상한 검증 생략.
     */
    private static void validateRelatedPage(Integer relatedPage, Book book) {
        if (relatedPage == null) {
            return;
        }
        if (relatedPage < 0) {
            throw new IllegalArgumentException("관련 페이지는 0 이상이어야 합니다");
        }
        Integer totalPage = book.getTotalPage();
        if (totalPage != null && totalPage > 0 && relatedPage > totalPage) {
            throw new IllegalArgumentException(
                    "관련 페이지(" + relatedPage + "p)가 책 전체 페이지(" + totalPage + "p)를 초과합니다");
        }
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Snippet record = snippetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + id));
        if (!record.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        snippetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<RecordDto> getRecordsByBook(Long bookId, Long userId, String type) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + bookId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found id: " + userId));

        List<Snippet> records;
        if (type != null && !type.isEmpty()) {
            records = snippetRepository.findByBookAndUserAndTypeOrderByCreateDateDesc(book, user, type);
        } else {
            records = snippetRepository.findByBookAndUserOrderByCreateDateDesc(book, user);
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
