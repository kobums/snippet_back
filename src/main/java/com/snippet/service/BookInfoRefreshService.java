package com.snippet.service;

import com.snippet.entity.Book;
import com.snippet.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기존 book_tb를 YES24 정보로 갱신한다 — 알라딘 OpenAPI 종료(2026-10-30) 뒤처리.
 *
 * <p>대부분의 표지가 알라딘 CDN(image.aladin.co.kr)을 가리키고 분류는 거의 비어 있다.
 * ISBN이 있는 책만 YES24 상세로 찾아 알라딘·빈 표지를 YES24 XL 표지로 바꾸고, 빈 분류·쪽수·상품 링크만 채운다.
 * 사용자가 고친 값(쪽수 등)은 덮어쓰지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookInfoRefreshService {

    /** YES24 한도(초당 10회) 아래로 — 검색 API와 같은 키를 쓰므로 여유를 둔다. */
    static final long CALL_INTERVAL_MS = 200;

    private final BookRepository bookRepository;
    private final Yes24BookClient yes24;

    public record Result(int scanned, int matched, int covers, int categories, int pages, int links,
                         List<String> samples) {
    }

    @Caching(evict = {
        @CacheEvict(value = "categoryStats", allEntries = true),
        @CacheEvict(value = "readingInsights", allEntries = true)
    })
    public Result refreshFromYes24(boolean dryRun) {
        if (!yes24.isEnabled()) {
            throw new IllegalStateException("YES24_API_KEY 미설정");
        }
        int scanned = 0, matched = 0, covers = 0, categories = 0, pages = 0, links = 0;
        List<String> samples = new java.util.ArrayList<>();
        for (Book book : bookRepository.findAll()) {
            if (book.getIsbn() == null || !book.getIsbn().matches("97[89]\\d{10}")) {
                continue;
            }
            scanned++;
            var found = yes24.lookupItem(book.getIsbn());
            sleep();
            if (found.isEmpty()) {
                continue;
            }
            matched++;
            var item = found.get();

            String newCover = null;
            if (isReplaceableCover(book.getCoverUrl()) && !item.book().getCoverUrl().isEmpty()) {
                newCover = item.book().getCoverUrl();
                covers++;
            }
            String newCategory = null;
            if (isBlank(book.getCategory()) && item.category() != null) {
                newCategory = item.category();
                categories++;
            }
            Integer newPages = null;
            if ((book.getTotalPage() == null || book.getTotalPage() == 0) && item.book().getTotalPage() > 0) {
                newPages = item.book().getTotalPage();
                pages++;
            }
            String newLink = null;
            if ((isBlank(book.getAffiliateUrl()) || book.getAffiliateUrl().contains("aladin.co.kr"))
                    && !item.link().isEmpty()) {
                newLink = item.link();
                links++;
            }
            if (newCover == null && newCategory == null && newPages == null && newLink == null) {
                continue;
            }
            if (samples.size() < 5) {
                samples.add(book.getId() + " " + book.getTitle() + " → cover=" + newCover + " category=" + newCategory
                        + " pages=" + newPages);
            }
            if (!dryRun) {
                book.update(null, null, null, newCover, newLink, null, newPages, null, newCategory);
                bookRepository.save(book);
            }
        }
        Result result = new Result(scanned, matched, covers, categories, pages, links, samples);
        log.info("YES24 도서 정보 갱신 dryRun={} {}", dryRun, result);
        return result;
    }

    /** 알라딘 CDN·국립중앙도서관 작은 썸네일·빈 표지만 바꾼다 (다른 출처는 사용자가 고른 것일 수 있다). */
    static boolean isReplaceableCover(String coverUrl) {
        return isBlank(coverUrl) || coverUrl.contains("aladin.co.kr") || coverUrl.contains("nl.go.kr");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void sleep() {
        try {
            Thread.sleep(CALL_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("중단됨", e);
        }
    }
}
