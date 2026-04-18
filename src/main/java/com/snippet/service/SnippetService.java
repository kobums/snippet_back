package com.snippet.service;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.entity.Snippet;
import com.snippet.entity.SnippetArchive;
import com.snippet.entity.User;
import com.snippet.repository.SnippetArchiveRepository;
import com.snippet.repository.SnippetRepository;
import com.snippet.repository.UserBookRepository;
import com.snippet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final SnippetArchiveRepository snippetArchiveRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    public List<SnippetCardDto> getCards(int count, List<Long> excludeIds, Long userId) {
        if (userId != null) {
            return getPersonalizedCards(count, excludeIds, userId);
        }
        return getRandomCards(count, excludeIds);
    }

    // ==================== 개인화 추천 ====================

    private List<SnippetCardDto> getPersonalizedCards(int count, List<Long> requestExcludeIds, Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return getRandomCards(count, requestExcludeIds);
        }

        // 제외할 ID 수집: 요청 excludeIds + 이미 아카이브한 스니펫 + 서재에 있는 책의 스니펫
        Set<Long> excludeSet = new HashSet<>();
        if (requestExcludeIds != null) {
            excludeSet.addAll(requestExcludeIds);
        }
        excludeSet.addAll(snippetArchiveRepository.findSnippetIdsByUser(user));

        List<Long> libraryBookIds = userBookRepository.findBookIdsByUserId(userId);
        if (!libraryBookIds.isEmpty()) {
            excludeSet.addAll(snippetRepository.findSnippetIdsByBookIds(libraryBookIds));
        }

        // 선호 카테고리 계산 (아카이브 가중치 2, 서재 가중치 1)
        Map<String, Integer> categoryScore = new LinkedHashMap<>();
        snippetArchiveRepository.findArchivedCategoriesByUser(user)
                .forEach(c -> categoryScore.merge(c, 2, (a, b) -> a + b));
        userBookRepository.findCategoriesByUserId(userId)
                .forEach(c -> categoryScore.merge(c, 1, (a, b) -> a + b));

        List<String> rankedCategories = categoryScore.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 카테고리 순서대로 스니펫 채우기
        List<Long> finalExcludeIds = new ArrayList<>(excludeSet);
        List<SnippetCardDto> result = new ArrayList<>();

        for (String category : rankedCategories) {
            if (result.size() >= count) break;
            int needed = count - result.size();

            long available = snippetRepository.countSnippetsByCategoryExcluding(category, finalExcludeIds);
            if (available == 0) continue;

            int maxPage = (int) Math.ceil((double) available / needed) - 1;
            int randomPage = maxPage > 0 ? random.nextInt(maxPage + 1) : 0;

            List<Snippet> batch = snippetRepository.findSnippetsByCategoryExcluding(
                    category, finalExcludeIds, PageRequest.of(randomPage, needed));
            batch.forEach(s -> {
                result.add(SnippetCardDto.from(s));
                finalExcludeIds.add(s.getId());
            });
        }

        // 부족하면 랜덤으로 채우기
        if (result.size() < count) {
            int remaining = count - result.size();
            result.addAll(getRandomCards(remaining, finalExcludeIds));
        }

        return result;
    }

    // ==================== 기존 랜덤 조회 ====================

    private List<SnippetCardDto> getRandomCards(int count, List<Long> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            long totalCount = snippetRepository.countSnippetCards();
            if (totalCount == 0) return List.of();

            int maxPage = (int) Math.ceil((double) totalCount / count) - 1;
            int randomPage = maxPage > 0 ? random.nextInt(maxPage + 1) : 0;

            return snippetRepository.findSnippetsWithOffset(PageRequest.of(randomPage, count))
                    .stream().map(SnippetCardDto::from).toList();
        }

        long totalCount = snippetRepository.countSnippetCardsExcluding(excludeIds);
        if (totalCount == 0) return List.of();

        int maxPage = (int) Math.ceil((double) totalCount / count) - 1;
        int randomPage = maxPage > 0 ? random.nextInt(maxPage + 1) : 0;

        return snippetRepository.findSnippetsWithOffsetExcluding(excludeIds, PageRequest.of(randomPage, count))
                .stream().map(SnippetCardDto::from).toList();
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
