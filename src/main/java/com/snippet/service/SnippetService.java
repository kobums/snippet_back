package com.snippet.service;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.entity.Snippet;
import com.snippet.entity.SnippetArchive;
import com.snippet.entity.User;
import com.snippet.repository.SnippetArchiveRepository;
import com.snippet.repository.SnippetRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final SnippetArchiveRepository snippetArchiveRepository;
    private final UserRepository userRepository;

    public List<SnippetCardDto> getCards(int count, List<Long> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return snippetRepository.findRandomSnippets(count)
                    .stream()
                    .map(SnippetCardDto::from)
                    .toList();
        }

        return snippetRepository.findRandomSnippets(count, excludeIds)
                .stream()
                .map(SnippetCardDto::from)
                .toList();
    }

    public List<SnippetArchiveDto> getArchive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        return snippetArchiveRepository.findByUserOrderByCreateDateDesc(user)
                .stream()
                .map(sa -> SnippetArchiveDto.from(sa.getSnippet()))
                .toList();
    }

    @Transactional
    public Long addArchive(Long userId, Long snippetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        Snippet snippet = snippetRepository.findById(snippetId)
                .orElseThrow(() -> new IllegalArgumentException("스니펫을 찾을 수 없습니다"));

        if (snippetArchiveRepository.existsByUserAndSnippet(user, snippet)) {
            return null;
        }

        SnippetArchive archive = SnippetArchive.builder()
                .user(user)
                .snippet(snippet)
                .build();
        return snippetArchiveRepository.save(archive).getId();
    }

    @Transactional
    public void removeArchive(Long userId, Long snippetId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));
        Snippet snippet = snippetRepository.findById(snippetId)
                .orElseThrow(() -> new IllegalArgumentException("스니펫을 찾을 수 없습니다"));
        snippetArchiveRepository.deleteByUserAndSnippet(user, snippet);
    }
}
