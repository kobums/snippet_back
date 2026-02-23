package com.snippet.repository;

import com.snippet.entity.Snippet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnippetRepository extends JpaRepository<Snippet, Long> {

    @Query(value = "SELECT * FROM snippet_tb WHERE s_id NOT IN (:excludeIds) ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Snippet> findRandomSnippets(@Param("count") int count, @Param("excludeIds") List<Long> excludeIds);

    @Query(value = "SELECT * FROM snippet_tb ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Snippet> findRandomSnippets(@Param("count") int count);

    List<Snippet> findByIdIn(List<Long> ids);
}
