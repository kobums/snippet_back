package com.snippet.repository;

import com.snippet.entity.SnippetDailyView;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SnippetDailyViewRepository extends JpaRepository<SnippetDailyView, Long> {
    Optional<SnippetDailyView> findByUserAndDate(User user, LocalDate date);
}
