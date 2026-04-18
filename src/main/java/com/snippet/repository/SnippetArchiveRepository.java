package com.snippet.repository;

import com.snippet.entity.Snippet;
import com.snippet.entity.SnippetArchive;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnippetArchiveRepository extends JpaRepository<SnippetArchive, Long> {

    @Query("SELECT sa FROM SnippetArchive sa JOIN FETCH sa.snippet s JOIN FETCH s.book WHERE sa.user = :user ORDER BY sa.createDate DESC")
    List<SnippetArchive> findByUserOrderByCreateDateDesc(@Param("user") User user);

    boolean existsByUserAndSnippet(User user, Snippet snippet);

    void deleteByUserAndSnippet(User user, Snippet snippet);

    @Query("SELECT sa.snippet.id FROM SnippetArchive sa WHERE sa.user = :user")
    List<Long> findSnippetIdsByUser(@Param("user") User user);

    @Query("SELECT sa.snippet.book.category FROM SnippetArchive sa WHERE sa.user = :user AND sa.snippet.book.category IS NOT NULL")
    List<String> findArchivedCategoriesByUser(@Param("user") User user);
}
