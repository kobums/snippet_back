package com.snippet.repository;

import com.snippet.dto.ReadingSessionStatsDto;
import com.snippet.entity.ReadingSession;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {

    @Query("SELECT rs FROM ReadingSession rs JOIN FETCH rs.book WHERE rs.user = :user ORDER BY rs.sessionDate DESC")
    List<ReadingSession> findAllByUser(@Param("user") User user);

    @Query("SELECT rs FROM ReadingSession rs JOIN FETCH rs.book WHERE rs.user = :user AND rs.userBook.id = :userBookId ORDER BY rs.sessionDate DESC")
    List<ReadingSession> findByUserAndUserBookIdWithBook(@Param("user") User user, @Param("userBookId") Long userBookId);

    @Query("""
            SELECT new com.snippet.dto.ReadingSessionStatsDto(
                COUNT(rs),
                COALESCE(SUM(rs.durationSeconds), 0L),
                COALESCE(SUM(CAST(rs.pagesRead AS long)), 0L),
                COALESCE(AVG(rs.secondsPerPage), 0.0)
            )
            FROM ReadingSession rs
            WHERE rs.user = :user AND rs.userBook.id = :userBookId
            """)
    ReadingSessionStatsDto getStatsByUserAndUserBookId(@Param("user") User user, @Param("userBookId") Long userBookId);
}
