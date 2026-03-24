package com.snippet.repository;

import com.snippet.entity.Snippet;
import com.snippet.entity.SnippetArchive;
import com.snippet.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SnippetArchiveRepository extends JpaRepository<SnippetArchive, Long> {

    List<SnippetArchive> findByUserOrderByCreateDateDesc(User user);

    boolean existsByUserAndSnippet(User user, Snippet snippet);

    void deleteByUserAndSnippet(User user, Snippet snippet);
}
