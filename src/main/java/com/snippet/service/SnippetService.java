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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final SnippetArchiveRepository snippetArchiveRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public List<SnippetCardDto> getCards(int count, List<Long> excludeIds) {
        // 개선된 랜덤 조회 - ORDER BY RAND() 대신 랜덤 오프셋 사용
        if (excludeIds == null || excludeIds.isEmpty()) {
            long totalCount = snippetRepository.countSnippetCards();
            if (totalCount == 0) {
                return List.of();
            }

            // 랜덤 페이지 계산
            int maxPage = (int) Math.ceil((double) totalCount / count) - 1;
            int randomPage = maxPage > 0 ? random.nextInt(maxPage + 1) : 0;

            return snippetRepository.findSnippetsWithOffset(PageRequest.of(randomPage, count))
                    .stream()
                    .map(SnippetCardDto::from)
                    .toList();
        }

        long totalCount = snippetRepository.countSnippetCardsExcluding(excludeIds);
        if (totalCount == 0) {
            return List.of();
        }

        int maxPage = (int) Math.ceil((double) totalCount / count) - 1;
        int randomPage = maxPage > 0 ? random.nextInt(maxPage + 1) : 0;

        return snippetRepository.findSnippetsWithOffsetExcluding(excludeIds, PageRequest.of(randomPage, count))
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
