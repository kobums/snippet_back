package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.dto.BookSearchDto;
import com.snippet.exception.BookSearchUnavailableException;
import com.snippet.util.NlRecordNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 도서 검색·ISBN 조회 — 국립중앙도서관 OpenAPI.
 *
 * <p>알라딘 OpenAPI가 2026-10-30 서비스를 종료해(신규 키 발급도 9/4 종료) 교체했다.
 * 검색은 소장자료 검색 API(관련도순), 쪽수·정확한 출판일은 ISBN 서지정보(seoji) API로 채운다.
 * 두 API는 같은 인증키를 쓴다.
 */
@Slf4j
@Service
public class BookSearchService {

    /** 클라이언트(웹·iOS·Android)는 한 페이지가 10개 미만이면 마지막 페이지로 본다. */
    static final int PAGE_SIZE = 10;
    /** 국외 도서·ISBN 없는 자료·전자책·중복 판본을 걸러내므로 한 번에 넉넉히 받는다. */
    static final int UPSTREAM_PAGE_SIZE = 50;
    static final int MAX_UPSTREAM_CALLS = 4;

    static final String SEARCH_URL = "https://www.nl.go.kr/NL/search/openApi/search.do";
    static final String ISBN_URL = "https://www.nl.go.kr/seoji/SearchApi.do";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate = createRestTemplate();

    @Value("${national-library.api-key:}")
    private String apiKey;

    public List<BookSearchDto> searchBooks(String query, int page) {
        String keyword = query == null ? "" : query.trim();
        if (keyword.isEmpty()) {
            return List.of();
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.error("national-library.api-key(NATIONAL_LIBRARY_KEY) 미설정 — 도서 검색 불가");
            throw new BookSearchUnavailableException();
        }

        int safePage = Math.max(page, 1);
        int wanted = safePage * PAGE_SIZE;
        // 관련도 순서를 유지한 채 같은 ISBN 레코드를 합친다. 포함 여부는 레코드 하나만 보고 정하므로
        // 뒤 페이지를 더 받아도 앞쪽 순서가 바뀌지 않는다 (클라이언트 페이지 사이 중복·누락 방지).
        Map<String, Candidate> merged = new LinkedHashMap<>();
        for (int upstreamPage = 1; upstreamPage <= MAX_UPSTREAM_CALLS && merged.size() < wanted; upstreamPage++) {
            JsonNode body;
            try {
                body = fetchSearchPage(keyword, upstreamPage);
            } catch (UpstreamFailure e) {
                if (upstreamPage == 1) {
                    log.error("국립중앙도서관 검색 실패: query='{}' {}", keyword, e.getMessage());
                    throw new BookSearchUnavailableException();
                }
                // 이미 모은 결과로 응답한다 (마지막 페이지를 넘기면 JSON 대신 HTML 오류 페이지가 온다)
                log.warn("국립중앙도서관 검색 {}페이지 실패, 앞 페이지까지만 사용: query='{}' {}",
                        upstreamPage, keyword, e.getMessage());
                break;
            }
            JsonNode items = body.path("result");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                Candidate candidate = Candidate.from(item);
                if (candidate != null) {
                    merged.merge(candidate.isbn, candidate, Candidate::absorb);
                }
            }
            if ((long) upstreamPage * UPSTREAM_PAGE_SIZE >= body.path("total").asLong(0)) {
                break;
            }
        }

        return merged.values().stream()
                .skip((long) (safePage - 1) * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .map(Candidate::toDto)
                .toList();
    }

    /**
     * ISBN 서지정보로 쪽수·출판일을 찾는다. 소장자료 검색은 쪽수를 주지 않고 출판일도 연도뿐인 경우가 많다.
     * 실패해도 책 추가는 막지 않는다 (쪽수는 사용자가 나중에 고칠 수 있다).
     */
    public Optional<IsbnLookup> lookupByIsbn(String isbn) {
        if (apiKey == null || apiKey.isBlank() || isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(ISBN_URL)
                    .queryParam("cert_key", "{key}")
                    .queryParam("result_style", "json")
                    .queryParam("page_no", 1)
                    .queryParam("page_size", 1)
                    .queryParam("isbn", "{isbn}")
                    .encode()
                    .buildAndExpand(apiKey, isbn.trim())
                    .toUri();
            JsonNode body = fetch(uri);
            if ("ERROR".equals(body.path("RESULT").asText())) {
                throw new UpstreamFailure("ERR_CODE=" + body.path("ERR_CODE").asText()
                        + " " + body.path("ERR_MESSAGE").asText());
            }
            JsonNode doc = body.path("docs").path(0);
            if (doc.isMissingNode()) {
                return Optional.empty();
            }
            LocalDate published = NlRecordNormalizer.compactDate(doc.path("REAL_PUBLISH_DATE").asText(""));
            if (published == null) {
                published = NlRecordNormalizer.compactDate(doc.path("PUBLISH_PREDATE").asText(""));
            }
            return Optional.of(new IsbnLookup(NlRecordNormalizer.pageCount(doc.path("PAGE").asText("")), published));
        } catch (UpstreamFailure e) {
            log.warn("ISBN 서지정보 조회 실패: isbn={} {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    /** totalPage·publicationDate는 서지정보에 없으면 null. */
    public record IsbnLookup(Integer totalPage, LocalDate publicationDate) {
    }

    private JsonNode fetchSearchPage(String keyword, int upstreamPage) {
        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("key", "{key}")
                .queryParam("apiType", "json")
                .queryParam("srchTarget", "total")
                .queryParam("kwd", "{kwd}")
                .queryParam("category", "{category}")
                .queryParam("pageNum", upstreamPage)
                .queryParam("pageSize", UPSTREAM_PAGE_SIZE)
                .encode()
                .buildAndExpand(apiKey, keyword, "도서")
                .toUri();
        JsonNode body = fetch(uri);
        if (body.hasNonNull("errorCode")) {
            throw new UpstreamFailure("errorCode=" + body.path("errorCode").asText()
                    + " " + body.path("errorMsg").asText());
        }
        return body;
    }

    /** 소장자료 검색은 Content-Type이 text/json이라 기본 JSON 컨버터가 받지 않는다 — 바이트로 받아 직접 읽는다. */
    private JsonNode fetch(URI uri) {
        byte[] body;
        try {
            body = restTemplate.getForObject(uri, byte[].class);
        } catch (RestClientException e) {
            // 예외 메시지에 요청 URL(인증키 포함)이 들어 있어 로그에 남기기 전에 가린다
            throw new UpstreamFailure(e.getClass().getSimpleName() + ": " + maskKey(e.getMessage()));
        }
        if (body == null || body.length == 0) {
            throw new UpstreamFailure("빈 응답");
        }
        try {
            return JSON.readTree(body);
        } catch (IOException e) {
            throw new UpstreamFailure("JSON이 아닌 응답");
        }
    }

    private String maskKey(String message) {
        if (message == null || apiKey == null || apiKey.isBlank()) {
            return message;
        }
        return message.replace(apiKey, "****");
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return new RestTemplate(factory);
    }

    /** 외부 API 호출 실패. 메시지는 인증키를 가린 상태로만 만든다. */
    private static final class UpstreamFailure extends RuntimeException {
        UpstreamFailure(String message) {
            super(message);
        }
    }

    /** 검색 결과 한 건 — 같은 ISBN의 다른 레코드(디지털화본 등)를 흡수하며 빈 값을 채운다. */
    private static final class Candidate {
        private final String isbn;
        private String title;
        private String author;
        private String publisher;
        private String pubDate;
        private String coverUrl;
        /** 실물 소장(오프라인) 레코드 — 디지털화 레코드보다 표기가 깔끔하다. */
        private boolean catalogRecord;

        private Candidate(String isbn) {
            this.isbn = isbn;
        }

        static Candidate from(JsonNode item) {
            boolean online = "온라인자료".equals(item.path("menuName").asText());
            // 온라인 + 라이선스 뷰어는 전자책·웹 콘텐츠 판본 (알라딘 시절에도 eBook은 검색에서 뺐다)
            if (online && "LD_VIEWER".equals(item.path("docYn").asText())) {
                return null;
            }
            String isbn = NlRecordNormalizer.domesticIsbn13(item.path("isbn").asText(""));
            String title = NlRecordNormalizer.title(item.path("titleInfo").asText(""));
            if (isbn == null || title.isEmpty()) {
                return null;
            }
            Candidate candidate = new Candidate(isbn);
            candidate.title = title;
            candidate.author = NlRecordNormalizer.author(item.path("authorInfo").asText(""));
            candidate.publisher = NlRecordNormalizer.publisher(item.path("pubInfo").asText(""));
            candidate.pubDate = NlRecordNormalizer.pubDate(item.path("pubYearInfo").asText(""));
            candidate.coverUrl = NlRecordNormalizer.coverUrl(item.path("imageUrl").asText(""));
            candidate.catalogRecord = !online;
            return candidate;
        }

        Candidate absorb(Candidate other) {
            if (!catalogRecord && other.catalogRecord) {
                title = other.title;
                if (!other.author.isEmpty()) {
                    author = other.author;
                }
                if (!other.publisher.isEmpty()) {
                    publisher = other.publisher;
                }
                catalogRecord = true;
            }
            if (author.isEmpty()) {
                author = other.author;
            }
            if (publisher.isEmpty()) {
                publisher = other.publisher;
            }
            if (coverUrl.isEmpty()) {
                coverUrl = other.coverUrl;
            }
            // 연도만 있는 쪽보다 날짜까지 있는 쪽을 쓴다
            if (pubDate.length() < other.pubDate.length()) {
                pubDate = other.pubDate;
            }
            return this;
        }

        BookSearchDto toDto() {
            return BookSearchDto.builder()
                    .title(title)
                    .author(author)
                    .publisher(publisher)
                    .pubDate(pubDate)
                    .isbn(isbn)
                    .coverUrl(coverUrl)
                    .totalPage(0)
                    .build();
        }
    }
}
