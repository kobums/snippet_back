package com.snippet.service;

import com.snippet.dto.ReadingSessionAddRequestDto;
import com.snippet.dto.ReadingSessionDto;
import com.snippet.dto.ReadingSessionStatsDto;
import com.snippet.entity.Book;
import com.snippet.entity.ReadingSession;
import com.snippet.entity.User;
import com.snippet.entity.UserBook;
import com.snippet.repository.ReadingSessionRepository;
import com.snippet.repository.UserBookRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingSessionService {

    private final ReadingSessionRepository sessionRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;

    public Long create(Long userId, ReadingSessionAddRequestDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserBook userBook = userBookRepository.findByIdWithBook(dto.getUserBookId())
                .orElseThrow(() -> new IllegalArgumentException("UserBook not found: " + dto.getUserBookId()));

        Book book = userBook.getBook();

        ReadingSession session = ReadingSession.builder()
                .user(user)
                .userBook(userBook)
                .book(book)
                .durationSeconds(dto.getDurationSeconds())
                .startPage(dto.getStartPage())
                .endPage(dto.getEndPage())
                .sessionDate(LocalDate.parse(dto.getSessionDate()))
                .build();

        if (dto.getEndPage() > userBook.getReadPage()) {
            userBook.updateReadPage(dto.getEndPage());
        }

        return sessionRepository.save(session).getId();
    }

    @Transactional(readOnly = true)
    public List<ReadingSessionDto> getByBook(Long userId, Long userBookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return sessionRepository.findByUserAndUserBookIdWithBook(user, userBookId)
                .stream()
                .map(ReadingSessionDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingSessionStatsDto getStats(Long userId, Long userBookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return sessionRepository.getStatsByUserAndUserBookId(user, userBookId);
    }
}
