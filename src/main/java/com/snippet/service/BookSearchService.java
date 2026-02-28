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

    @Value("${NLK_API_KEY:}")
    private String nlkApiKey;

    public List<BookSearchDto> searchBooks(String query) {
        // MVP: if NLK API key is empty, return mock data for testing
        if (nlkApiKey == null || nlkApiKey.isEmpty()) {
            log.info("NLK API key is empty. Returning mock search results for query: {}", query);
            return getMockResults(query);
        }

        // Implementation of NLK API (국립중앙도서관 서지정보 API)
        // Note: The actual NLK API uses XML response, requiring XML parsing.
        // For the purpose of moving forward quickly without the exact API structure,
        // we'll leave a mock implementation for now until real integration is done.
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
                .build());
        return results;
    }
}
