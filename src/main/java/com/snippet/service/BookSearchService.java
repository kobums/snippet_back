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

    @Value("${NATIONAL_LIBRARY_KEY:}")
    private String nlkApiKey;

    public List<BookSearchDto> searchBooks(String query) {
        if (nlkApiKey == null || nlkApiKey.isEmpty()) {
            log.info("NLK API key is empty. Returning mock search results for query: {}", query);
            return getMockResults(query);
        }

        try {
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromUriString("https://www.nl.go.kr/seoji/SearchApi.do")
                    .queryParam("cert_key", nlkApiKey)
                    .queryParam("result_style", "json")
                    .queryParam("page_no", 1)
                    .queryParam("page_size", 10)
                    .queryParam("title", query)
                    .build(false)
                    .toUriString();

            com.fasterxml.jackson.databind.JsonNode response = restTemplate.getForObject(url,
                    com.fasterxml.jackson.databind.JsonNode.class);
            List<BookSearchDto> results = new ArrayList<>();

            if (response != null && response.has("docs")) {
                for (com.fasterxml.jackson.databind.JsonNode doc : response.get("docs")) {
                    Integer totalPage = null;
                    String pageStr = doc.path("PAGE").asText("").replaceAll("[^0-9]", "");
                    if (!pageStr.isEmpty()) {
                        try {
                            totalPage = Integer.parseInt(pageStr);
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    results.add(BookSearchDto.builder()
                            .title(doc.path("TITLE").asText(""))
                            .author(doc.path("AUTHOR").asText(""))
                            .publisher(doc.path("PUBLISHER").asText(""))
                            .pubDate(doc.path("PUBLISH_PREDATE").asText(""))
                            .isbn(doc.path("EA_ISBN").asText(""))
                            .coverUrl(doc.path("TITLE_URL").asText(""))
                            .totalPage(totalPage)
                            .build());
                }
                return results;
            }
        } catch (Exception e) {
            log.error("Error calling NLK API for query: {}", query, e);
        }

        // Fallback to mock results on error or empty response
        return getMockResults(query);
    }

    private List<BookSearchDto> getMockResults(String query) {
        List<BookSearchDto> results = new ArrayList<>();
        results.add(BookSearchDto.builder()
                .title("Mock Book: " + query)
                .author("Mock Author")
                .publisher("Mock Publisher")
                .pubDate("2026-01-01")
                .isbn("9781234567890")
                .coverUrl("https://via.placeholder.com/150")
                .totalPage(300)
                .build());
        return results;
    }
}
