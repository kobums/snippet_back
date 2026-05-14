package com.snippet.repository;

import com.snippet.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE b.category IN :categories AND b.id NOT IN :excludeIds ORDER BY RAND()")
    List<Book> findByCategoryInAndIdNotIn(
            @Param("categories") List<String> categories,
            @Param("excludeIds") List<Long> excludeIds,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.id NOT IN :excludeIds ORDER BY RAND()")
    List<Book> findByIdNotIn(
            @Param("excludeIds") List<Long> excludeIds,
            org.springframework.data.domain.Pageable pageable);
}
