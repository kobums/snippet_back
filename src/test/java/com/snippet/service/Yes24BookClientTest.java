package com.snippet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snippet.dto.BookSearchDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** YES24 Open API 응답을 MockRestServiceServer로 흉내 낸다. 필드 모양은 2026-11 실제 응답 기준. */
class Yes24BookClientTest {

    private static final String KEY = "test-yes24-key";
    private static final ObjectMapper JSON = new ObjectMapper();

    private Yes24BookClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        client = new Yes24BookClient();
        ReflectionTestUtils.setField(client, "apiKey", KEY);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    @DisplayName("검색 결과를 알라딘 시절 형식으로 바꾸고 키는 헤더로만 보낸다")
    void mapsItem() {
        ObjectNode demian = item("데미안", "9788937460449", "헤르만 헤세 저/전영애 역");
        demian.put("pages", 239);
        expectSearch(1, 1, demian);

        List<BookSearchDto> results = client.search("데미안", 1, 10);

        assertThat(results).singleElement().satisfies(book -> {
            assertThat(book.getTitle()).isEqualTo("데미안");
            assertThat(book.getAuthor()).isEqualTo("저자: 헤르만 헤세, 옮긴이: 전영애");
            assertThat(book.getPublisher()).isEqualTo("민음사");
            assertThat(book.getPubDate()).isEqualTo("2000-12-20");
            assertThat(book.getCoverUrl()).isEqualTo("https://image.yes24.com/goods/1/XL");
            assertThat(book.getTotalPage()).isEqualTo(239);
        });
    }

    @Test
    @DisplayName("세트(ISBN 없음)·외국 ISBN·성인 도서·중복 ISBN은 뺀다")
    void filtersItems() {
        ObjectNode adult = item("성인", "9791100000003", "가 저");
        adult.put("adultYn", "Y");
        expectSearch(1, 5,
                item("세트", "", "한강 저"),
                item("원서", "9780141036144", "George Orwell"),
                adult,
                item("소년이 온다", "9788936434120", "한강 저"),
                item("소년이 온다", "9788936434120", "한강 저"));

        assertThat(client.search("한강", 1, 10)).extracting(BookSearchDto::getTitle).containsExactly("소년이 온다");
    }

    @Test
    @DisplayName("걸러낸 뒤 모자라면 다음 페이지를 더 받아 채운다")
    void fillsFromNextPage() {
        List<ObjectNode> first = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            first.add(i < 6 ? item("국내 " + i, isbn(i), "가 저") : item("세트 " + i, "", "가 저"));
        }
        List<ObjectNode> second = new ArrayList<>();
        for (int i = 6; i < 20; i++) {
            second.add(item("국내 " + i, isbn(i), "가 저"));
        }
        expectSearch(1, 64, first.toArray(ObjectNode[]::new));
        expectSearch(2, 64, second.toArray(ObjectNode[]::new));

        assertThat(client.search("책", 1, 10)).hasSize(10);
    }

    @Test
    @DisplayName("결과 없음(404 SEARCH_001)은 빈 목록")
    void notFoundIsEmpty() {
        server.expect(requestTo(startsWith(Yes24BookClient.SEARCH_URL)))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"errorCode\":\"SEARCH_001\",\"message\":\"검색 결과가 없습니다\"}"));

        assertThat(client.search("없는책", 1, 10)).isEmpty();
    }

    @Test
    @DisplayName("한도 초과(429)는 실패로 올려 호출 쪽이 국립중앙도서관으로 넘어가게 한다")
    void rateLimitFails() {
        server.expect(requestTo(startsWith(Yes24BookClient.SEARCH_URL)))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.search("데미안", 1, 10)).isInstanceOf(Yes24BookClient.Yes24Failure.class);
    }

    @Test
    @DisplayName("ISBN 상세에서 쪽수·출판일을 읽는다")
    void lookupByIsbn() {
        ObjectNode demian = item("데미안", "9788937460449", "헤르만 헤세 저");
        demian.put("pages", 239);
        server.expect(requestTo(startsWith(Yes24BookClient.DETAIL_URL)))
                .andExpect(queryParam("query", "9788937460449"))
                .andRespond(withSuccess(body(1, demian), MediaType.APPLICATION_JSON));

        assertThat(client.lookupByIsbn("9788937460449"))
                .contains(new BookSearchService.IsbnLookup(239, LocalDate.of(2000, 12, 20)));
    }

    @Test
    @DisplayName("키가 없으면 꺼진 상태")
    void disabledWithoutKey() {
        ReflectionTestUtils.setField(client, "apiKey", "");
        assertThat(client.isEnabled()).isFalse();
        assertThat(client.lookupByIsbn("9788937460449")).isEmpty();
    }

    private void expectSearch(int page, int total, ObjectNode... items) {
        server.expect(requestTo(startsWith(Yes24BookClient.SEARCH_URL)))
                .andExpect(header("X-Api-Key", KEY))
                .andExpect(queryParam("page", String.valueOf(page)))
                .andExpect(queryParam("sort", "DEFAULT"))
                .andRespond(withSuccess(body(total, items), MediaType.APPLICATION_JSON));
    }

    private static String body(int total, ObjectNode... items) {
        ObjectNode root = JSON.createObjectNode();
        root.put("success", true);
        ObjectNode data = root.putObject("data");
        ArrayNode array = data.putArray("items");
        for (ObjectNode item : items) {
            array.add(item);
        }
        data.put("totalCount", total);
        return root.toString();
    }

    private static ObjectNode item(String title, String isbn13, String author) {
        ObjectNode item = JSON.createObjectNode();
        item.put("title", title);
        item.put("author", author);
        item.put("publisher", "민음사");
        item.put("isbn13", isbn13);
        item.put("publishDate", "20001220");
        item.put("adultYn", "N");
        item.put("cover", "https://image.yes24.com/goods/1/L");
        return item;
    }

    /** 체크섬이 맞는 979-11 ISBN. */
    private static String isbn(int n) {
        String body = String.format("97911%07d", n);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += (body.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        }
        return body + (10 - sum % 10) % 10;
    }
}
