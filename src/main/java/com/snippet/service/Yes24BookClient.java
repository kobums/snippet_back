package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.dto.BookSearchDto;
import com.snippet.util.NlRecordNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * YES24 Open API 도서 검색·ISBN 상세 — 알라딘 OpenAPI(2026-10-30 종료)와 가장 비슷한 정보를 준다.
 *
 * <p>국립중앙도서관보다 제목 검색 품질이 좋고 표지가 크며(XL 708×1200), 검색 결과에 쪽수·정확한 출판일까지 온다.
 * 키(`YES24_API_KEY`)가 없거나 호출이 실패하면 {@link BookSearchService}가 국립중앙도서관으로 넘어간다.
 * 한도는 키당 초당 10회·일 20,000회(응답 헤더 기준, 문서는 5회·5,000회).
 */
@Slf4j
@Component
public class Yes24BookClient {

    static final String SEARCH_URL = "https://apis.yes24.com/v1/goods/itemList";
    static final String DETAIL_URL = "https://apis.yes24.com/v1/goods/itemDetail";
    /** 세트·전자책·외국도서·성인 도서를 걸러내므로 한 번에 넉넉히 받는다. */
    static final int UPSTREAM_PAGE_SIZE = 50;
    static final int MAX_UPSTREAM_CALLS = 4;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate = createRestTemplate();

    @Value("${yes24.api-key:}")
    private String apiKey;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 인기도순(DEFAULT) 검색. 정확도순(RELATION)은 "데미안"에 다른 책이 먼저 오는 등 오히려 덜 정확했다.
     * 같은 ISBN은 한 번만, 순서는 YES24 순서 그대로 — 뒤 페이지를 더 받아도 앞쪽 순서가 바뀌지 않는다.
     *
     * @throws Yes24Failure 첫 요청부터 실패했을 때 (호출 쪽이 다른 출처로 넘어간다)
     */
    public List<BookSearchDto> search(String keyword, int page, int pageSize) {
        int wanted = page * pageSize;
        Map<String, BookSearchDto> merged = new LinkedHashMap<>();
        for (int upstreamPage = 1; upstreamPage <= MAX_UPSTREAM_CALLS && merged.size() < wanted; upstreamPage++) {
            JsonNode data;
            try {
                data = fetchSearchPage(keyword, upstreamPage);
            } catch (Yes24Failure e) {
                if (upstreamPage == 1) {
                    throw e;
                }
                log.warn("YES24 검색 {}페이지 실패, 앞 페이지까지만 사용: query='{}' {}", upstreamPage, keyword, e.getMessage());
                break;
            }
            JsonNode items = data.path("items");
            if (!items.isArray() || items.isEmpty()) {
                break;
            }
            for (JsonNode item : items) {
                BookSearchDto book = toDto(item);
                if (book != null) {
                    merged.putIfAbsent(book.getIsbn(), book);
                }
            }
            if ((long) upstreamPage * UPSTREAM_PAGE_SIZE >= data.path("totalCount").asLong(0)) {
                break;
            }
        }
        return merged.values().stream()
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .toList();
    }

    /** ISBN 상세에서 쪽수·출판일·분류. 없거나 실패하면 비어 있다 (책 추가는 막지 않는다). */
    public Optional<BookSearchService.IsbnLookup> lookupByIsbn(String isbn) {
        return lookupItem(isbn).map(item -> new BookSearchService.IsbnLookup(
                item.book().getTotalPage() > 0 ? item.book().getTotalPage() : null,
                NlRecordNormalizer.compactDate(item.book().getPubDate().replace("-", "")),
                item.category()));
    }

    /** ISBN 상세 한 건 — 기존 책 정보 갱신용(표지·분류·쪽수·상품 링크). */
    public Optional<Yes24Item> lookupItem(String isbn) {
        if (!isEnabled() || isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(DETAIL_URL)
                    .queryParam("searchType", "ISBN13")
                    .queryParam("query", "{isbn}")
                    .queryParam("detail", "Y")
                    .encode()
                    .buildAndExpand(isbn.trim())
                    .toUri();
            JsonNode item = fetch(uri).path("items").path(0);
            return item.isMissingNode() ? Optional.empty() : Optional.ofNullable(toItem(item));
        } catch (Yes24Failure e) {
            log.warn("YES24 ISBN 상세 조회 실패: isbn={} {}", isbn, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 제목(+저자)으로 책 한 권을 찾는다 — ISBN을 모르는 크롤링 문장의 책 정보 보강용.
     * 제목이 공백·문장부호를 빼고 같은 책 중 저자 이름이 겹치는 것을 우선하고, 제목이 같은 책이 없으면 비어 있다.
     */
    public Optional<Yes24Item> findByTitle(String title, String author) {
        if (!isEnabled() || title == null || title.isBlank()) {
            return Optional.empty();
        }
        JsonNode items;
        try {
            items = fetchSearchPage(title.trim(), 1).path("items");
        } catch (Yes24Failure e) {
            log.warn("YES24 제목 검색 실패: title='{}' {}", title, e.getMessage());
            return Optional.empty();
        }
        String wantedTitle = compact(title);
        String wantedAuthor = author == null ? "" : compact(author);
        Yes24Item sameTitle = null;
        for (JsonNode node : items) {
            Yes24Item item = toItem(node);
            if (item == null || !compact(item.book().getTitle()).equals(wantedTitle)) {
                continue;
            }
            if (!wantedAuthor.isEmpty() && sharesName(node.path("author").asText(""), wantedAuthor)) {
                return Optional.of(item);
            }
            if (sameTitle == null) {
                sameTitle = item;
            }
        }
        return Optional.ofNullable(sameTitle);
    }

    /** "정세랑 지음" vs "정세랑 저" — 역할 단어를 떼고 이름 하나라도 겹치면 같은 저자로 본다. */
    private static boolean sharesName(String yes24Author, String wantedAuthor) {
        for (String part : yes24Author.split("[/,]")) {
            String name = compact(part.replaceAll("\\s+(저|글|지음|역|옮김|그림|편|엮음|감수|원작)$", ""));
            if (name.length() >= 2 && wantedAuthor.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static String compact(String text) {
        return text.replaceAll("[\\s\\p{Punct}·:：]", "").toLowerCase();
    }

    /** YES24 상품 한 건 — 검색 결과 형식 + 분류("국내도서>소설/시/희곡")·상품 링크. */
    public record Yes24Item(BookSearchDto book, String category, String link) {
    }

    static Yes24Item toItem(JsonNode item) {
        BookSearchDto book = toDto(item);
        if (book == null) {
            return null;
        }
        // book_tb.b_category는 50자, 알라딘 시절 형식처럼 ">"로 잇는다
        String category = item.path("goodsSortNm").asText("").trim().replace("-", ">");
        if (category.length() > 50) {
            category = category.substring(0, 50);
        }
        return new Yes24Item(book, category.isEmpty() ? null : category, item.path("link").asText(""));
    }

    /** 세트(ISBN 없음)·외국도서·성인 도서는 뺀다. 알라딘 시절에도 외국도서·eBook은 검색에서 뺐다. */
    static BookSearchDto toDto(JsonNode item) {
        if ("Y".equals(item.path("adultYn").asText())) {
            return null;
        }
        String isbn = NlRecordNormalizer.domesticIsbn13(item.path("isbn13").asText(""));
        String title = NlRecordNormalizer.title(item.path("title").asText(""));
        if (isbn == null || title.isEmpty()) {
            return null;
        }
        int pages = item.path("pages").asInt(0);
        return BookSearchDto.builder()
                .title(title)
                // "헤르만 헤세 저/전영애 역" → "저자: 헤르만 헤세, 옮긴이: 전영애" (알라딘 시절 형식)
                .author(NlRecordNormalizer.author(item.path("author").asText("").replace("/", ";")))
                .publisher(NlRecordNormalizer.publisher(item.path("publisher").asText("")))
                .pubDate(NlRecordNormalizer.pubDate(item.path("publishDate").asText("")))
                .isbn(isbn)
                .coverUrl(coverUrl(item.path("cover").asText("")))
                .totalPage(Math.max(pages, 0))
                .build();
    }

    /** 기본 표지(…/goods/{id}/L)는 236×400이라 책 상세에서 흐리다 — XL(708×1200)을 쓴다. */
    static String coverUrl(String cover) {
        if (cover == null || cover.isBlank()) {
            return "";
        }
        return cover.endsWith("/L") ? cover.substring(0, cover.length() - 1) + "XL" : cover;
    }

    private JsonNode fetchSearchPage(String keyword, int upstreamPage) {
        URI uri = UriComponentsBuilder.fromUriString(SEARCH_URL)
                .queryParam("query", "{query}")
                .queryParam("category", "BOOK")
                .queryParam("sort", "DEFAULT")
                .queryParam("detail", "Y")
                .queryParam("page", upstreamPage)
                .queryParam("pageSize", UPSTREAM_PAGE_SIZE)
                .encode()
                .buildAndExpand(keyword)
                .toUri();
        return fetch(uri);
    }

    /** 응답의 data 노드. 검색 결과 없음(404 SEARCH_001)은 빈 data로 본다. */
    private JsonNode fetch(URI uri) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        byte[] body;
        try {
            body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), byte[].class).getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return JSON.createObjectNode();
            }
            throw new Yes24Failure("HTTP " + e.getStatusCode().value() + " " + errorCode(e.getResponseBodyAsByteArray()));
        } catch (RestClientException e) {
            // 키는 헤더로만 보내 예외 메시지(URL)에 들어가지 않는다
            throw new Yes24Failure(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (body == null || body.length == 0) {
            throw new Yes24Failure("빈 응답");
        }
        JsonNode root;
        try {
            root = JSON.readTree(body);
        } catch (IOException e) {
            throw new Yes24Failure("JSON이 아닌 응답");
        }
        if (!root.path("success").asBoolean(false)) {
            throw new Yes24Failure("errorCode=" + root.path("errorCode").asText() + " " + root.path("message").asText());
        }
        return root.path("data");
    }

    private static String errorCode(byte[] body) {
        try {
            return JSON.readTree(body).path("errorCode").asText("");
        } catch (IOException | RuntimeException e) {
            return "";
        }
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return new RestTemplate(factory);
    }

    /** YES24 호출 실패 (한도 초과 429 포함). */
    static final class Yes24Failure extends RuntimeException {
        Yes24Failure(String message) {
            super(message);
        }
    }
}
