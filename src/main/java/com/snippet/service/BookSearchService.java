package com.snippet.service;

import com.snippet.dto.BookSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${aladin.api-key:}")
    private String aladinApiKey;

    public List<BookSearchDto> searchBooks(String query) {
        if (aladinApiKey == null || aladinApiKey.isEmpty()) {
            log.info("Aladin API key is empty. Returning mock search results for query: {}", query);
            return getMockResults(query);
        }

        try {
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString("http://www.aladin.co.kr/ttb/api/ItemSearch.aspx")
                    .queryParam("ttbkey", aladinApiKey)
                    .queryParam("Query", query)
                    .queryParam("QueryType", "Keyword")
                    .queryParam("MaxResults", 10)
                    .queryParam("start", 1)
                    .queryParam("SearchTarget", "Book")
                    .queryParam("output", "js")
                    .queryParam("Version", "20131101")
                    .build(false)
                    .toUriString();

            com.fasterxml.jackson.databind.JsonNode response = restTemplate.getForObject(url,
                    com.fasterxml.jackson.databind.JsonNode.class);
            List<BookSearchDto> results = new ArrayList<>();

            if (response != null && response.has("item")) {
                for (com.fasterxml.jackson.databind.JsonNode item : response.get("item")) {
                    String isbn = item.path("isbn13").asText("");
                    if (isbn.isEmpty()) {
                        isbn = item.path("isbn").asText(""); // fallback to isbn10
                    }

                    String rawAuthor = item.path("author").asText("");
                    String parsedAuthor = formatAuthorString(rawAuthor);

                    results.add(BookSearchDto.builder()
                            .title(item.path("title").asText(""))
                            .author(parsedAuthor)
                            .publisher(item.path("publisher").asText(""))
                            .pubDate(item.path("pubDate").asText(""))
                            .isbn(isbn)
                            .coverUrl(item.path("cover").asText("")) // 알라딘 커버 이미지
                            .totalPage(0) // 검색 목록에서는 0 고정, 추가 시 단건 조회로 보완
                            .build());
                }
                return results;
            }
        } catch (Exception e) {
            log.error("Error calling Aladin API for query: {}", query, e);
        }

        // Fallback to mock results on error or empty response
        return getMockResults(query);
    }

    public Integer getBookPageFromAladin(String isbn) {
        if (aladinApiKey == null || aladinApiKey.isEmpty() || isbn == null || isbn.isEmpty()) {
            return 0; // fallback
        }

        try {
            // 알라딘 상품 조회 API (ItemLookUp)
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString("http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx")
                    .queryParam("ttbkey", aladinApiKey)
                    .queryParam("itemIdType", "ISBN") // ISBN(10자리 또는 13자리) 기준 검색
                    .queryParam("ItemId", isbn)
                    .queryParam("output", "js")
                    .queryParam("Version", "20131101")
                    // .queryParam("OptResult", "packing") // packing, ebookList 등 옵션 (페이지수는 기본으로
                    // 내려옴)
                    .build(false)
                    .toUriString();

            com.fasterxml.jackson.databind.JsonNode response = restTemplate.getForObject(url,
                    com.fasterxml.jackson.databind.JsonNode.class);

            if (response != null && response.has("item") && response.get("item").isArray()
                    && response.get("item").size() > 0) {
                com.fasterxml.jackson.databind.JsonNode item = response.get("item").get(0);
                if (item.has("subInfo") && item.get("subInfo").has("itemPage")) {
                    return item.path("subInfo").path("itemPage").asInt(0);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Aladin ItemLookUp API for ISBN ({}): {}", isbn, e.getMessage());
        }

        return 0;
    }

    private String formatAuthorString(String rawAuthor) {
        if (rawAuthor == null || rawAuthor.isEmpty()) {
            return "";
        }

        // 예: "헨리 데이비드 소로 (지은이), 강승영 (옮긴이), 허버트 웬델 글리슨 (사진)"
        String[] parts = rawAuthor.split(",\\s*");
        List<String> formattedParts = new ArrayList<>();

        for (String part : parts) {
            // 정규식이나 단순 indexOf로 역할 분리
            int openParen = part.lastIndexOf('(');
            int closeParen = part.lastIndexOf(')');

            if (openParen != -1 && closeParen != -1 && openParen < closeParen) {
                String name = part.substring(0, openParen).trim();
                String role = part.substring(openParen + 1, closeParen).trim();

                // "지은이" -> "저자" 로 변경
                if ("지은이".equals(role)) {
                    role = "저자";
                }

                formattedParts.add(role + ": " + name);
            } else {
                // 괄호가 없는 경우 (기본 저자 취급)
                formattedParts.add("저자: " + part.trim());
            }
        }

        return String.join(", ", formattedParts);
    }

    private List<BookSearchDto> getMockResults(String query) {
        List<BookSearchDto> results = new ArrayList<>();
        results.add(BookSearchDto.builder()
                .title("Mock Book: " + query)
                .author("Mock Author")
                .publisher("Mock Publisher")
                .pubDate("2026-01-01")
                .isbn("9781234567890")
                .coverUrl("https://image.aladin.co.kr/product/35824/53/cover500/k222036733_1.jpg")
                .totalPage(300)
                .build());
        return results;
    }
}
