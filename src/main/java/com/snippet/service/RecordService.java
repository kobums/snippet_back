package com.snippet.service;

import com.snippet.dto.RecordAddRequestDto;
import com.snippet.dto.RecordDto;
import com.snippet.entity.Book;
import com.snippet.entity.Snippet;
import com.snippet.repository.BookRepository;
import com.snippet.repository.SnippetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final BookRepository bookRepository;
    private final SnippetRepository snippetRepository; // Acts as RecordRepository

    @Transactional
    public Long addRecord(Long bookId, RecordAddRequestDto requestDto) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found id: " + bookId));

        Snippet record = Snippet.builder()
                .book(book)
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
}
