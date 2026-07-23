package com.snippet.repository;

import com.snippet.entity.RefreshToken;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByUserAndExpireDateBefore(User user, LocalDateTime dateTime);
}
