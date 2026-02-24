package com.snippet.service;

import com.snippet.entity.Book;
import com.snippet.entity.Snippet;
import com.snippet.repository.BookRepository;
import com.snippet.repository.SnippetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlerService {

    private final BookRepository bookRepository;
    private final SnippetRepository snippetRepository;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${ALADIN_TTB_KEY}")
    private String ttbKey;

    @Transactional
    public void crawlAladinSentences(int pagesToCrawl, int startPages) {
        int totalSaved = 0;

        for (int page = startPages; page <= pagesToCrawl; page++) {
            // 알라딘 무한 스크롤 AJAX 엔드포인트
            String url = "https://www.aladin.co.kr/shop/common/wsentence_ajax.aspx?mobile=0&force=1&Page=" + page
                    + "&sort=11&ItemId=0";
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent(
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept",
                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                        .header("Accept-Language", "ko-KR,ko;q=0.8,en-US;q=0.5,en;q=0.3")
                        .get();

                // 문서에 있는 sec1, sec2 형식의 모든 section 탐색
                Elements sections = doc.select("section[id^=sec]");
                if (sections.isEmpty()) {
                    log.info("No more sections found at page {}", page);
                    break;
                }

                int count = 0;
                for (Element section : sections) {
                    // 문장 추출
                    Element textEl = section.selectFirst(".text p");
                    if (textEl == null)
                        continue;
                    String text = textEl.text().trim();
                    if (text.isEmpty())
                        continue;

                    // 책 정보 텍스트 (예: 동물 인터넷. 마르틴 비켈스키 지음, 박래선 옮김)
                    Element bookInfoEl = section.selectFirst(".text span a");
                    String rawBookInfo = bookInfoEl != null ? bookInfoEl.text().trim() : "알라딘 소개 도서";

                    String title = rawBookInfo;
                    String author = "미상";
                    int dotIdx = rawBookInfo.indexOf(". ");
                    if (dotIdx != -1) {
                        title = rawBookInfo.substring(0, dotIdx).trim();
                        author = rawBookInfo.substring(dotIdx + 2).trim();
                    }

                    // 표지 이미지와 제휴 링크 추출
                    String coverUrl = "";
                    String affiliateUrl = "https://www.aladin.co.kr";
                    String isbn = "978" + System.currentTimeMillis() % 10000000000L; // Fallback
                    String extractedId = null;

                    Element coverA = section.selectFirst(".cover a");
                    if (coverA != null) {
                        String onclick = coverA.attr("onclick");
                        if (onclick != null && onclick.contains("'")) {
                            affiliateUrl = onclick.split("'")[1];
                            if (affiliateUrl.contains("ItemId=")) {
                                String[] parts = affiliateUrl.split("ItemId=");
                                if (parts.length > 1) {
                                    extractedId = parts[1].split("&")[0];
                                    isbn = "AL_" + extractedId;
                                    if (isbn.length() > 13) {
                                        isbn = isbn.substring(0, 13);
                                    }
                                }
                            }
                        }
                        Element img = coverA.selectFirst("img");
                        if (img != null) {
                            coverUrl = img.attr("src");
                            if (coverUrl.startsWith("//")) {
                                coverUrl = "https:" + coverUrl;
                            }
                        }
                    }

                    // OpenAPI를 통해 상세 정보 (ISBN13 등) 조회
                    if (extractedId != null) {
                        String apiUrl = "http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx?ttbkey=" + ttbKey
                                + "&itemIdType=ItemId&ItemId=" + extractedId + "&output=js&Version=20131101";
                        try {
                            String response = restTemplate.getForObject(apiUrl, String.class);
                            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
                            com.fasterxml.jackson.databind.JsonNode itemNode = root.path("item").path(0);

                            if (!itemNode.isMissingNode()) {
                                title = itemNode.path("title").asText(title);
                                author = itemNode.path("author").asText(author);
                                coverUrl = itemNode.path("cover").asText(coverUrl);
                                affiliateUrl = itemNode.path("link").asText(affiliateUrl);

                                String isbn13 = itemNode.path("isbn13").asText("");
                                if (!isbn13.isEmpty()) {
                                    isbn = isbn13;
                                    if (isbn.length() > 13) {
                                        isbn = isbn.substring(0, 13);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to fetch book info from Aladin OpenAPI for ItemId: " + extractedId, e);
                        }
                    }

                    final String finalIsbn = isbn;
                    final String finalTitle = title;
                    final String finalAuthor = author;
                    final String finalCoverUrl = coverUrl;
                    final String finalAffiliateUrl = affiliateUrl;

                    Book book = bookRepository.findByIsbn(finalIsbn)
                            .orElseGet(() -> bookRepository.save(Book.builder()
                                    .isbn(finalIsbn)
                                    .title(finalTitle)
                                    .author(finalAuthor)
                                    .coverUrl(finalCoverUrl)
                                    .affiliateUrl(finalAffiliateUrl)
                                    .build()));

                    if (!snippetRepository.existsByBookAndText(book, text)) {
                        Snippet snippet = Snippet.builder()
                                .book(book)
                                .text(text)
                                .tag("Aladin")
                                .build();

                        snippetRepository.save(snippet);
                        count++;
                    } else {
                        log.debug("Snippet already exists. Skipping duplicate.");
                    }
                }
                totalSaved += count;
                log.info("Crawled page {}. Saved {} snippets in this page.", page, count);

                // 알라딘 서버 부하를 방지하기 위해 잠시 대기
                Thread.sleep(5000);
            } catch (Exception e) {
                log.error("Failed to crawl Aladin sentences at page {}", page, e);
            }
        }
        log.info("Finished crawling Aladin sentences. Total saved: {} snippets.", totalSaved);
    }
}
