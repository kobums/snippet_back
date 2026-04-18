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
    // ==================== JOIN FETCH 메서드 (N+1 방지) ====================

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.id = :id")
    Optional<UserBook> findByIdWithBook(@Param("id") Long id);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book")
    List<UserBook> findAllWithBook();

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId AND ub.book = :book")
    Optional<UserBook> findByUser_IdAndBook(@Param("userId") Long userId, @Param("book") Book book);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId")
    List<UserBook> findByUser_Id(@Param("userId") Long userId);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId ORDER BY ub.updateDate DESC")
    List<UserBook> findByUser_IdOrderByUpdateDateDesc(@Param("userId") Long userId);

    /**
     * 페이지네이션 지원 - updateDate 내림차순 정렬 (JPQL로 변경하여 JOIN FETCH 적용)
     */
    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId ORDER BY ub.updateDate DESC")
    List<UserBook> findByUser_IdPaginatedWithBook(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.user.id = :userId AND ub.status = :status AND ub.updateDate >= :startDate AND ub.updateDate <= :endDate")
    long countByUser_IdAndStatusAndUpdateDateBetween(
            @Param("userId") Long userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByUser_IdAndStatus(Long userId, String status);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId AND ub.status = :status")
    List<UserBook> findByUser_IdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    /**
     * 활동 기간(startDate ~ endDate)이 해당 월과 겹치는 책 조회.
     * - startDate <= 해당 월 말일  AND  endDate >= 해당 월 1일
     * - 즉, status가 reading/completed/dropped 인 책 대상
     */
    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId " +
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
    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId " +
           "AND ub.status = 'waiting' " +
           "AND ub.createDate >= :monthStart " +
           "AND ub.createDate < :monthEnd")
    List<UserBook> findByUser_IdAndWaitingInMonth(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    // ==================== 통계 쿼리 메서드 ====================

    /**
     * 월별 통합 통계 (완료 권수 + 페이지 수 + 카테고리별 권수)
     * 반환: [month, completedCount, totalPages, category, categoryCount]
     * category가 null이면 해당 월의 집계 정보만, null이 아니면 카테고리별 상세 정보
     */
    @Query("SELECT MONTH(ub.endDate) as month, " +
           "COUNT(ub) as completedCount, " +
           "SUM(ub.book.totalPage) as totalPages, " +
           "ub.book.category as category, " +
           "COUNT(ub) as categoryCount " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "GROUP BY MONTH(ub.endDate), ub.book.category")
    List<Object[]> findMonthlyStatsIntegrated(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 월별 완료 권수 (개별 쿼리 - 하위 호환성)
     */
    @Deprecated
    @Query("SELECT MONTH(ub.endDate) as month, COUNT(ub) as count " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "GROUP BY MONTH(ub.endDate)")
    List<Object[]> findMonthlyCompletedCount(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 월별 읽은 페이지 수 (개별 쿼리 - 하위 호환성)
     */
    @Deprecated
    @Query("SELECT MONTH(ub.endDate) as month, SUM(ub.book.totalPage) as pages " +
           "FROM UserBook ub " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "GROUP BY MONTH(ub.endDate)")
    List<Object[]> findMonthlyTotalPages(@Param("userId") Long userId, @Param("year") int year);

    /**
     * 월별 카테고리별 권수 (개별 쿼리 - 하위 호환성)
     */
    @Deprecated
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
     * 최장 독서 기록 (JPQL로 변경하여 JOIN FETCH 적용)
     * DATEDIFF를 네이티브 함수로 사용하되, JPQL에서 function()으로 호출
     */
    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book " +
           "WHERE ub.user.id = :userId AND ub.status = 'completed' " +
           "AND YEAR(ub.endDate) = :year " +
           "ORDER BY function('DATEDIFF', ub.endDate, ub.startDate) DESC")
    List<UserBook> findLongestReadingBookWithBook(@Param("userId") Long userId, @Param("year") int year, Pageable pageable);

    /**
     * 대시보드 진행 탭용 조회
     * - waiting, reading: 날짜 무관 전체 조회
     * - completed: 해당 월의 완독만 조회
     */
    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId " +
           "AND (ub.status = 'waiting' OR ub.status = 'reading' " +
           "OR (ub.status = 'completed' AND ub.endDate >= :monthStart AND ub.endDate < :monthEnd))")
    List<UserBook> findProgressBooks(
            @Param("userId") Long userId,
            @Param("monthStart") LocalDateTime monthStart,
            @Param("monthEnd") LocalDateTime monthEnd);

    // ==================== 추천 알고리즘용 쿼리 ====================

    @Query("SELECT ub.book.id FROM UserBook ub WHERE ub.user.id = :userId")
    List<Long> findBookIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ub.book.category FROM UserBook ub WHERE ub.user.id = :userId AND ub.book.category IS NOT NULL")
    List<String> findCategoriesByUserId(@Param("userId") Long userId);
}
