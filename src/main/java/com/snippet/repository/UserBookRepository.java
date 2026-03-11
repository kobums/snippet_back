package com.snippet.repository;

import com.snippet.entity.Book;
import com.snippet.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findByUser_IdAndBook(Long userId, Book book);

    List<UserBook> findByUser_Id(Long userId);

    List<UserBook> findByUser_IdOrderByUpdateDateDesc(Long userId);

    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user.id = :userId AND ub.status = :status AND ub.updateDate >= :startDate AND ub.updateDate <= :endDate")
    long countByUser_IdAndStatusAndUpdateDateBetween(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByUser_IdAndStatus(Long userId, String status);

    List<UserBook> findByUser_IdAndStatus(Long userId, String status);

    /**
     * 활동 기간(startDate ~ endDate)이 해당 월과 겹치는 책 조회.
     * - startDate <= 해당 월 말일  AND  endDate >= 해당 월 1일
     * - 즉, status가 reading/completed/dropped 인 책 대상
     */
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId " +
           "AND ub.status != 'waiting' " +
           "AND ub.startDate < :monthEnd " +
           "AND ub.endDate >= :monthStart")
    List<UserBook> findByUser_IdAndDateOverlap(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    /**
     * 해당 월에 생성된 대기중(waiting) 책 조회.
     * - createDate가 해당 월 안에 있는 경우
     */
    @Query("SELECT ub FROM UserBook ub WHERE ub.user.id = :userId " +
           "AND ub.status = 'waiting' " +
           "AND ub.createDate >= :monthStart " +
           "AND ub.createDate < :monthEnd")
    List<UserBook> findByUser_IdAndWaitingInMonth(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);
}
