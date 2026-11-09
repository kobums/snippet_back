package com.snippet.controller;

import com.snippet.security.AdminGuard;
import com.snippet.security.CustomUserDetails;
import com.snippet.service.BookInfoRefreshService;
import com.snippet.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class AdminCrawlerController {

    private final CrawlerService crawlerService;
    private final BookInfoRefreshService bookInfoRefreshService;
    private final AdminGuard adminGuard;

    @PostMapping("/aladin")
    public ResponseEntity<String> crawlAladin(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "pages", defaultValue = "1") int pages,
            @RequestParam(name = "startpages", defaultValue = "1") int startPages) {
        adminGuard.check(userDetails);
        crawlerService.crawlAladinSentences(pages, startPages);
        return ResponseEntity
                .ok("Crawler triggered successfully. Pages requested: " + pages + ". Check logs for details.");
    }

    /**
     * 기존 책 정보를 YES24로 갱신 (알라딘 표지 → YES24 XL 표지, 빈 분류·쪽수·상품 링크 채움).
     * 기본은 dryRun=true — 바꿀 건수와 예시만 보여준다. 한 권에 0.2초라 수백 권이면 몇 분 걸린다.
     */
    @PostMapping("/yes24refresh")
    public ResponseEntity<BookInfoRefreshService.Result> refreshFromYes24(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "dryRun", defaultValue = "true") boolean dryRun) {
        adminGuard.check(userDetails);
        return ResponseEntity.ok(bookInfoRefreshService.refreshFromYes24(dryRun));
    }
}
