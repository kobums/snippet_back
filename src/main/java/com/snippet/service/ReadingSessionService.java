package com.snippet.service;

import com.snippet.dto.ReadingSessionAddRequestDto;
import com.snippet.dto.ReadingSessionDto;
import com.snippet.dto.ReadingSessionStatsDto;
import com.snippet.dto.StreakDto;
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

        // 남의 서재 항목에 세션을 붙여 readPage를 밀어올리는 것을 차단
        if (!userBook.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

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
    public List<ReadingSessionDto> getAll(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return sessionRepository.findAllByUser(user)
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

    @Transactional(readOnly = true)
    public StreakDto getStreak(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        List<LocalDate> dates = sessionRepository.findDistinctSessionDatesByUser(user);
        if (dates.isEmpty()) return new StreakDto(0, 0, null);

        LocalDate today = LocalDate.now();
        LocalDate latest = dates.get(0);

        // currentStreak: count consecutive days from today or yesterday
        int current = 0;
        if (!latest.isBefore(today.minusDays(1))) {
            LocalDate expected = latest;
            for (LocalDate d : dates) {
                if (!d.isAfter(expected)) {
                    if (d.isEqual(expected)) {
                        current++;
                        expected = expected.minusDays(1);
                    } else {
                        break;
                    }
                }
            }
        }

        // maxStreak: longest consecutive run
        int max = 1;
        int run = 1;
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i - 1).minusDays(1).isEqual(dates.get(i))) {
                run++;
                if (run > max) max = run;
            } else {
                run = 1;
            }
        }

        String lastReadDate = latest.toString();
        return new StreakDto(current, Math.max(max, current), lastReadDate);
    }
}
