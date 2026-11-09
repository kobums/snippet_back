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
    private final com.snippet.repository.UserRepository userRepository;
    /** 도서 정보 보강 — 알라딘 OpenAPI(ItemLookUp)가 2026-10-30 종료돼 YES24 제목 검색으로 바꿨다. */
    private final Yes24BookClient yes24;

    /**
     * 크롤링한 문장의 작성자 계정(로그인 불가 시스템 계정, V34 마이그레이션이 생성).
     * 예전엔 id=2(운영자 개인 계정)로 고정돼 크롤링 문장이 개인 독서 기록에 섞였다.
     */
    @org.springframework.beans.factory.annotation.Value("${crawler.user-email}")
    private String crawlerUserEmail;

    @Transactional
    public void crawlAladinSentences(int pagesToCrawl, int startPages) {
        com.snippet.entity.User crawlerUser = userRepository.findByEmail(crawlerUserEmail)
                .orElseThrow(() -> new IllegalStateException("크롤러 계정이 없습니다: " + crawlerUserEmail));
        int totalSaved = 0;

        for (int page = startPages; page <= pagesToCrawl; page++) {
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

                Elements sections = doc.select("section[id^=sec]");
                if (sections.isEmpty()) {
                    break;
                }

                int count = 0;
                for (Element section : sections) {
                    Element textEl = section.selectFirst(".text p");
                    if (textEl == null)
                        continue;
                    String text = textEl.text().trim();
                    if (text.isEmpty())
                        continue;

                    Element bookInfoEl = section.selectFirst(".text span a");
                    String rawBookInfo = bookInfoEl != null ? bookInfoEl.text().trim() : "알라딘 소개 도서";

                    String title = rawBookInfo;
                    String author = "미상";
                    int dotIdx = rawBookInfo.indexOf(". ");
                    if (dotIdx != -1) {
                        title = rawBookInfo.substring(0, dotIdx).trim();
                        author = rawBookInfo.substring(dotIdx + 2).trim();
                    }

                    String coverUrl = "";
                    String affiliateUrl = "https://www.aladin.co.kr";
                    String isbn = "978" + System.currentTimeMillis() % 10000000000L;
                    String extractedId = null;
                    String publisher = "알라딘";
                    String category = "";
                    Integer totalPage = null;
                    java.time.LocalDate publicationDate = java.time.LocalDate.of(2000, 1, 1);

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

                    // 문장은 알라딘 웹 페이지에서 가져오고, 도서 정보(실제 ISBN·표지·쪽수·분류)는 YES24에서 찾는다.
                    // 못 찾으면 페이지에 있던 제목·저자·표지와 알라딘 상품 ID로 만든 임시 ISBN을 그대로 쓴다.
                    var match = yes24.findByTitle(title, author);
                    if (match.isPresent()) {
                        var found = match.get();
                        title = found.book().getTitle();
                        author = found.book().getAuthor();
                        isbn = found.book().getIsbn();
                        if (!found.book().getCoverUrl().isEmpty()) coverUrl = found.book().getCoverUrl();
                        if (!found.link().isEmpty()) affiliateUrl = found.link();
                        if (!found.book().getPublisher().isEmpty()) publisher = found.book().getPublisher();
                        if (found.category() != null) category = found.category();
                        if (found.book().getTotalPage() > 0) totalPage = found.book().getTotalPage();
                        java.time.LocalDate published = com.snippet.util.NlRecordNormalizer
                                .compactDate(found.book().getPubDate().replace("-", ""));
                        if (published != null) publicationDate = published;
                    }

                    final String finalIsbn = isbn;
                    final String finalTitle = title;
                    final String finalAuthor = author;
                    final String finalCoverUrl = coverUrl;
                    final String finalAffiliateUrl = affiliateUrl;
                    final String finalPublisher = publisher;
                    final String finalCategory = category;
                    final Integer finalTotalPage = totalPage;
                    final java.time.LocalDate finalPublicationDate = publicationDate;

                    Book book = bookRepository.findByIsbn(finalIsbn)
                            .orElseGet(() -> bookRepository.save(Book.builder()
                                    .isbn(finalIsbn)
                                    .title(finalTitle)
                                    .author(finalAuthor)
                                    .coverUrl(finalCoverUrl)
                                    .affiliateUrl(finalAffiliateUrl)
                                    .publisher(finalPublisher)
                                    .category(finalCategory)
                                    .totalPage(finalTotalPage)
                                    .publicationDate(finalPublicationDate)
                                    .build()));

                    if (!snippetRepository.existsByBookAndText(book, text)) {
                        Snippet snippet = Snippet.builder()
                                .book(book)
                                .user(crawlerUser)
                                .type("snippet")
                                .isPublic(true)
                                .text(text)
                                .tag("Aladin")
                                .relatedPage(0)
                                .build();

                        snippetRepository.save(snippet);
                        count++;
                    }
                }
                totalSaved += count;

                Thread.sleep(5000);
            } catch (Exception e) {
            }
        }
    }
}
