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
}
