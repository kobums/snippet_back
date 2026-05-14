package com.snippet.service;

import com.snippet.dto.BookRecommendDto;
import com.snippet.entity.Book;
import com.snippet.entity.User;
import com.snippet.repository.BookRepository;
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
public class BookRecommendService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final UserRepository userRepository;

    public List<BookRecommendDto> getRecommendations(Long userId, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 사용자가 이미 서재에 가진 책 ID 목록
        List<Long> ownedIds = userBookRepository.findBookIdsByUserId(userId);
        if (ownedIds.isEmpty()) ownedIds = List.of(-1L); // JPA IN (empty) 방지

        // 선호 카테고리 산출: 완독 + 별점 4~5점 책 카테고리 우선
        List<String> preferredCategories = buildPreferredCategories(userId);

        List<BookRecommendDto> results = new ArrayList<>();

        // 1단계: 선호 카테고리 기반 추천
        if (!preferredCategories.isEmpty()) {
            List<Book> catBooks = bookRepository.findByCategoryInAndIdNotIn(
                    preferredCategories, ownedIds, PageRequest.of(0, limit));
            String topCat = preferredCategories.get(0);
            for (Book b : catBooks) {
                results.add(new BookRecommendDto(b, topCat + " 분야를 좋아하시는 것 같아요"));
            }
        }

        // 2단계: 부족하면 카테고리 무관 랜덤 추가
        if (results.size() < limit) {
            List<Long> alreadyAdded = results.stream()
                    .map(BookRecommendDto::getId).toList();
            List<Long> exclude = new ArrayList<>(ownedIds);
            exclude.addAll(alreadyAdded);

            List<Book> extras = bookRepository.findByIdNotIn(
                    exclude, PageRequest.of(0, limit - results.size()));
            for (Book b : extras) {
                results.add(new BookRecommendDto(b, "Snippet의 추천 도서"));
            }
        }

        return results;
    }

    private List<String> buildPreferredCategories(Long userId) {
        // 카테고리별 점수: 완독=2점, 별점 4~5=보너스 2점
        List<Object[]> categoryData = userBookRepository.findCategoriesByUserId(userId)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(c -> c, Collectors.summingInt(c -> 1)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new Object[]{e.getKey(), e.getValue()})
                .collect(Collectors.toList());

        return categoryData.stream()
                .map(row -> (String) row[0])
                .limit(3)
                .collect(Collectors.toList());
    }
}
