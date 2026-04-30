package com.snippet.repository;

import com.snippet.entity.Suggestion;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {
    List<Suggestion> findByUserOrderByCreateDateDesc(User user);
}
