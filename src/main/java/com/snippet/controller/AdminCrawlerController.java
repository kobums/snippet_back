package com.snippet.controller;

import com.snippet.service.CrawlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/crawl")
@RequiredArgsConstructor
public class AdminCrawlerController {

    private final CrawlerService crawlerService;

    @PostMapping("/aladin")
    public ResponseEntity<String> crawlAladin(
            @RequestParam(name = "pages", defaultValue = "1") int pages,
            @RequestParam(name = "startpages", defaultValue = "1") int startPages) {
        crawlerService.crawlAladinSentences(pages, startPages);
        return ResponseEntity
                .ok("Crawler triggered successfully. Pages requested: " + pages + ". Check logs for details.");
    }
}
