package com.snippet.controller;

import com.snippet.security.AdminGuard;
import com.snippet.security.CustomUserDetails;
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
}
