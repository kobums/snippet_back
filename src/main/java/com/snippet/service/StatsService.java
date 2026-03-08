package com.snippet.service;

import com.snippet.dto.StatsDto;
import com.snippet.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserBookRepository userBookRepository;

    @Transactional(readOnly = true)
    public StatsDto getUserStats(Long userId) {
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);

        long monthlyCompleted = userBookRepository.countByUser_IdAndStatusAndUpdateDateBetween(
                userId, "completed", startOfMonth, endOfMonth);

        long currentlyReading = userBookRepository.countByUser_IdAndStatus(userId, "reading");
        long totalCompleted = userBookRepository.countByUser_IdAndStatus(userId, "completed");

        return StatsDto.builder()
                .monthlyCompletedCount(monthlyCompleted)
                .currentlyReadingCount(currentlyReading)
                .totalCompletedCount(totalCompleted)
                .build();
    }
}
