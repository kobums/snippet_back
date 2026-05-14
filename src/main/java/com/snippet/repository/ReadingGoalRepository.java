package com.snippet.repository;

import com.snippet.entity.ReadingGoal;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {
    Optional<ReadingGoal> findByUserAndYear(User user, int year);
}
