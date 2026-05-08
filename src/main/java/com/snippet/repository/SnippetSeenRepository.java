package com.snippet.repository;

import com.snippet.entity.SnippetSeen;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SnippetSeenRepository extends JpaRepository<SnippetSeen, Long> {

    Optional<SnippetSeen> findByUserAndSnippetId(User user, Long snippetId);

    @Query("SELECT ss.snippet.id FROM SnippetSeen ss WHERE ss.user = :user AND ss.seenDate >= :cutoffDate")
    List<Long> findRecentlySeenSnippetIds(@Param("user") User user, @Param("cutoffDate") LocalDate cutoffDate);

    @Modifying
    @Query(value = """
            INSERT INTO snippet_seen_tb (ss_user, ss_snippet, ss_seendate)
            VALUES (:userId, :snippetId, :seenDate)
            ON DUPLICATE KEY UPDATE ss_seendate = :seenDate
            """, nativeQuery = true)
    void upsertSeen(@Param("userId") Long userId, @Param("snippetId") Long snippetId, @Param("seenDate") LocalDate seenDate);
}
