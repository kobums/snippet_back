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

    public List<BookSearchDto> searchBooks(String query, int page) {
        if (aladinApiKey == null || aladinApiKey.isEmpty()) {
            return getMockResults(query);
        }

        try {
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString("http://www.aladin.co.kr/ttb/api/ItemSearch.aspx")
                    .queryParam("ttbkey", aladinApiKey)
                    .queryParam("Query", query)
                    .queryParam("QueryType", "Keyword")
                    .queryParam("MaxResults", 10)
                    .queryParam("start", page)
                    .queryParam("SearchTarget", "Book")
                    .queryParam("output", "js")
                    .queryParam("Version", "20131101")
                    .queryParam("Cover", "Big")
                    .build(false)
                    .toUriString();

            com.fasterxml.jackson.databind.JsonNode response = restTemplate.getForObject(url,
                    com.fasterxml.jackson.databind.JsonNode.class);
            List<BookSearchDto> results = new ArrayList<>();

            if (response != null && response.has("item")) {
                for (com.fasterxml.jackson.databind.JsonNode item : response.get("item")) {
                    String categoryName = item.path("categoryName").asText("");
                    if (shouldExcludeCategory(categoryName)) {
                        continue;
                    }

                    String isbn = item.path("isbn13").asText("");
                    if (isbn.isEmpty()) {
                        isbn = item.path("isbn").asText("");
                    }

                    String rawAuthor = item.path("author").asText("");
                    String parsedAuthor = formatAuthorString(rawAuthor);

                    results.add(BookSearchDto.builder()
                            .title(item.path("title").asText(""))
                            .author(parsedAuthor)
                            .publisher(item.path("publisher").asText(""))
                            .pubDate(item.path("pubDate").asText(""))
                            .isbn(isbn)
                            .coverUrl(item.path("cover").asText(""))
                            .totalPage(0)
                            .build());
                }
                return results;
            }
        } catch (Exception e) {
        }

        return getMockResults(query);
    }

    public Integer getBookPageFromAladin(String isbn) {
        if (aladinApiKey == null || aladinApiKey.isEmpty() || isbn == null || isbn.isEmpty()) {
            return 0;
        }

        try {
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString("http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx")
                    .queryParam("ttbkey", aladinApiKey)
                    .queryParam("itemIdType", "ISBN")
                    .queryParam("ItemId", isbn)
                    .queryParam("output", "js")
                    .queryParam("Version", "20131101")
                    .queryParam("Cover", "Big")
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
        }

        return 0;
    }

    private boolean shouldExcludeCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return false;
        }

        String[] excludeKeywords = {
                "북토크",
                "상품",
                "eBook",
                "중고",
                "외국도서",
                "음반",
                "DVD",
                "블루레이",
                "굿즈",
                "세트",
        };

        String lowerCategoryName = categoryName.toLowerCase();
        for (String keyword : excludeKeywords) {
            if (lowerCategoryName.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private String formatAuthorString(String rawAuthor) {
        if (rawAuthor == null || rawAuthor.isEmpty()) {
            return "";
        }

        String[] parts = rawAuthor.split(",\\s*");
        List<String> formattedParts = new ArrayList<>();

        for (String part : parts) {
            int openParen = part.lastIndexOf('(');
            int closeParen = part.lastIndexOf(')');

            if (openParen != -1 && closeParen != -1 && openParen < closeParen) {
                String name = part.substring(0, openParen).trim();
                String role = part.substring(openParen + 1, closeParen).trim();

                if ("지은이".equals(role)) {
                    role = "저자";
                }

                formattedParts.add(role + ": " + name);
            } else {
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
