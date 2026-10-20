package com.snippet.repository;

import com.snippet.entity.ReadingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReadingLogRepository extends JpaRepository<ReadingLog, Long> {

    @Query("SELECT rl FROM ReadingLog rl WHERE rl.user.id = :userId AND rl.logDate BETWEEN :from AND :to ORDER BY rl.logDate, rl.id")
    List<ReadingLog> findByUserIdAndLogDateBetween(@Param("userId") Long userId,
                                                   @Param("from") LocalDate from,
                                                   @Param("to") LocalDate to);

    @Query("SELECT rl FROM ReadingLog rl WHERE rl.user.id = :userId")
    List<ReadingLog> findAllByUserId(@Param("userId") Long userId);
}
