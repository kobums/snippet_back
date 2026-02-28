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
    Optional<UserBook> findByUserIdAndBook(String userId, Book book);

    List<UserBook> findByUserId(String userId);

    List<UserBook> findByUserIdOrderByUpdateDateDesc(String userId);

    @Query("SELECT COUNT(ub) FROM UserBook ub WHERE ub.userId = :userId AND ub.status = :status AND ub.updateDate >= :startDate AND ub.updateDate <= :endDate")
    long countByUserIdAndStatusAndUpdateDateBetween(
            @Param("userId") String userId,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByUserIdAndStatus(String userId, String status);

    List<UserBook> findByUserIdAndStatus(String userId, String status);
}
