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
import org.springframework.data.domain.Pageable;

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

    // ==================== 통계 쿼리 메서드 ====================

    /**
     * 월별 완료 권수
     */
    @Query("SELECT MONTH(ub.endDate) as month, COUNT(ub) as count " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "GROUP BY MONTH(ub.endDate)")
    List<Object[]> findMonthlyCompletedCount(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 월별 읽은 페이지 수
     */
    @Query("SELECT MONTH(ub.endDate) as month, SUM(ub.book.totalPage) as pages " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "GROUP BY MONTH(ub.endDate)")
    List<Object[]> findMonthlyTotalPages(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 월별 카테고리별 권수
     */
    @Query("SELECT MONTH(ub.endDate) as month, ub.book.category, COUNT(ub) " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year AND ub.book.category IS NOT NULL " +
           "GROUP BY MONTH(ub.endDate), ub.book.category")
    List<Object[]> findMonthlyCategoryCount(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 연도별 완료 권수 및 페이지 수
     */
    @Query("SELECT YEAR(ub.endDate) as year, COUNT(ub) as count, SUM(ub.book.totalPage) as pages " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "GROUP BY YEAR(ub.endDate) " +
           "ORDER BY year DESC")
    List<Object[]> findYearlyStats(@Param("userId") Long userId);

    /**
     * 카테고리별 통계 (올해 기준)
     */
    @Query("SELECT ub.book.category, COUNT(ub), " +
           "SUM(CASE WHEN ub.status = 'completed' THEN 1 ELSE 0 END) " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND YEAR(ub.startDate) = :year " +
           "AND ub.book.category IS NOT NULL " +
           "GROUP BY ub.book.category")
    List<Object[]> findCategoryStats(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 평균 독서 일수 (완료한 책만)
     */
    @Query(value = "SELECT AVG(DATEDIFF(ub_enddate, ub_startdate)) " +
           "FROM userbook_tb " +
           "WHERE ub_user = :userId AND ub_status = 'completed' " +
           "AND YEAR(ub_enddate) = :year", nativeQuery = true)
    Double findAverageReadingDays(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 최장 독서 기록
     */
    @Query(value = "SELECT * FROM userbook_tb " +
           "WHERE ub_user = :userId AND ub_status = 'completed' " +
           "AND YEAR(ub_enddate) = :year " +
           "ORDER BY DATEDIFF(ub_enddate, ub_startdate) DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<UserBook> findLongestReadingBook(@Param("userId") Long userId, @Param("year") int year, @Param("limit") int limit);
}
