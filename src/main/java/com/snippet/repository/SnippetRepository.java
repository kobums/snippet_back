package com.snippet.repository;

import com.snippet.entity.Book;
import com.snippet.entity.Snippet;
import com.snippet.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    // ==================== JOIN FETCH 메서드 (N+1 방지) ====================

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book")
    List<Snippet> findAllWithBook();

    // 개선된 랜덤 조회 - OFFSET 방식 (ORDER BY RAND() 대체)
    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.type = 'snippet' ORDER BY s.id")
    List<Snippet> findSnippetsWithOffset(Pageable pageable);

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.type = 'snippet' AND s.id NOT IN :excludeIds ORDER BY s.id")
    List<Snippet> findSnippetsWithOffsetExcluding(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Snippet s WHERE s.type = 'snippet'")
    long countSnippetCards();

    @Query("SELECT COUNT(s) FROM Snippet s WHERE s.type = 'snippet' AND s.id NOT IN :excludeIds")
    long countSnippetCardsExcluding(@Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT DISTINCT s FROM Snippet s JOIN FETCH s.book WHERE s.id IN :ids")
    List<Snippet> findByIdIn(@Param("ids") List<Long> ids);

    boolean existsByBookAndText(com.snippet.entity.Book book, String text);

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.book = :book ORDER BY s.createDate DESC")
    List<Snippet> findByBookOrderByCreateDateDesc(@Param("book") com.snippet.entity.Book book);

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.book = :book AND s.type = :type ORDER BY s.createDate DESC")
    List<Snippet> findByBookAndTypeOrderByCreateDateDesc(@Param("book") com.snippet.entity.Book book, @Param("type") String type);

    @Query("SELECT DISTINCT s FROM Snippet s JOIN FETCH s.book WHERE s.book IN :books AND s.type = :type AND s.createDate BETWEEN :start AND :end ORDER BY s.createDate DESC")
    List<Snippet> findByBookInAndTypeAndCreateDateBetweenOrderByCreateDateDesc(
            @Param("books") List<Book> books, @Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.user = :user AND s.type = :type AND s.createDate BETWEEN :start AND :end ORDER BY s.createDate DESC")
    List<Snippet> findByUserAndTypeAndCreateDateBetweenOrderByCreateDateDesc(
            @Param("user") User user, @Param("type") String type, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.user = :user AND s.createDate BETWEEN :start AND :end ORDER BY s.createDate DESC")
    List<Snippet> findByUserAndCreateDateBetweenOrderByCreateDateDesc(
            @Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ==================== 추천 알고리즘용 쿼리 ====================

    @Query("SELECT s FROM Snippet s JOIN FETCH s.book WHERE s.type = 'snippet' AND s.book.category = :category AND s.id NOT IN :excludeIds ORDER BY s.id")
    List<Snippet> findSnippetsByCategoryExcluding(
            @Param("category") String category,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Snippet s WHERE s.type = 'snippet' AND s.book.category = :category AND s.id NOT IN :excludeIds")
    long countSnippetsByCategoryExcluding(
            @Param("category") String category,
            @Param("excludeIds") List<Long> excludeIds);

    @Query("SELECT s.id FROM Snippet s WHERE s.type = 'snippet' AND s.book.id IN :bookIds")
    List<Long> findSnippetIdsByBookIds(@Param("bookIds") List<Long> bookIds);
}
