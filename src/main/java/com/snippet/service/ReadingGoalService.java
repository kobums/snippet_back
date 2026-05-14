package com.snippet.service;

import com.snippet.dto.ReadingGoalDto;
import com.snippet.entity.ReadingGoal;
import com.snippet.entity.User;
import com.snippet.repository.ReadingGoalRepository;
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
public class ReadingGoalService {

    private final ReadingGoalRepository goalRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReadingGoalDto getGoal(Long userId, int year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        int target = goalRepository.findByUserAndYear(user, year)
                .map(ReadingGoal::getTargetBooks)
                .orElse(0);

        int completed = countCompletedForYear(userId, year);
        return new ReadingGoalDto(year, target, completed);
    }

    public ReadingGoalDto setGoal(Long userId, int year, int targetBooks) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        ReadingGoal goal = goalRepository.findByUserAndYear(user, year)
                .orElse(null);

        if (goal == null) {
            goal = ReadingGoal.builder().user(user).year(year).targetBooks(targetBooks).build();
        } else {
            goal.updateTarget(targetBooks);
        }
        goalRepository.save(goal);

        int completed = countCompletedForYear(userId, year);
        return new ReadingGoalDto(year, targetBooks, completed);
    }

    private int countCompletedForYear(Long userId, int year) {
        List<Object[]> yearlyStats = userBookRepository.findYearlyStats(userId);
        return yearlyStats.stream()
                .filter(row -> row[0] != null && ((Number) row[0]).intValue() == year)
                .mapToInt(row -> ((Number) row[1]).intValue())
                .findFirst()
                .orElse(0);
    }
}
