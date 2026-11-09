package com.snippet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snippet.dto.BookSearchDto;
import com.snippet.exception.BookSearchUnavailableException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 도서 검색 테스트 — 국립중앙도서관 응답을 MockRestServiceServer로 흉내 낸다.
 *
 * <p>클라이언트 세 곳이 모두 "10개 미만이면 마지막 페이지"로 판단하므로, 걸러낸 뒤에도
 * 페이지를 10개로 채우는지와 페이지 사이 순서가 흔들리지 않는지를 중점으로 본다.
 */
@ExtendWith(OutputCaptureExtension.class)
class BookSearchServiceTest {

    private static final String KEY = "test-national-library-key";
    private static final ObjectMapper JSON = new ObjectMapper();
    // 소장자료 검색 API의 실제 Content-Type (기본 JSON 컨버터가 받지 않는 타입)
    private static final MediaType TEXT_JSON = MediaType.parseMediaType("text/json;charset=UTF-8");

    private BookSearchService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        service = new BookSearchService(new Yes24BookClient());
        ReflectionTestUtils.setField(service, "apiKey", KEY);
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Nested
    @DisplayName("페이지 채우기")
    class Paging {

        @Test
        @DisplayName("첫 페이지에서 국내 도서가 모자라면 다음 페이지를 더 받아 10개를 채운다")
        void fillsFromNextUpstreamPage() {
            List<ObjectNode> first = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                first.add(book("국내 " + i, isbn("97911", i)));
            }
            for (int i = 0; i < 44; i++) {
                first.add(book("외국 " + i, isbn("97800", i)));
            }
            List<ObjectNode> second = new ArrayList<>();
            for (int i = 6; i < 20; i++) {
                second.add(book("국내 " + i, isbn("97911", i)));
            }
            expectSearch(1, page(120, first));
            expectSearch(2, page(120, second));

            List<BookSearchDto> results = service.searchBooks("소설", 1);

            assertThat(results).extracting(BookSearchDto::getTitle)
                    .containsExactly("국내 0", "국내 1", "국내 2", "국내 3", "국내 4",
                            "국내 5", "국내 6", "국내 7", "국내 8", "국내 9");
        }

        @Test
        @DisplayName("두 번째 페이지는 걸러낸 목록의 11~20번째 — 첫 페이지와 겹치지 않는다")
        void secondPageContinuesFirst() {
            List<ObjectNode> records = new ArrayList<>();
            for (int i = 0; i < 25; i++) {
                records.add(book("책 " + i, isbn("97911", i)));
            }
            expectSearch(1, page(25, records));

            List<BookSearchDto> results = service.searchBooks("책", 2);

            assertThat(results).hasSize(10);
            assertThat(results.get(0).getTitle()).isEqualTo("책 10");
            assertThat(results.get(9).getTitle()).isEqualTo("책 19");
        }

        @Test
        @DisplayName("전체 건수를 다 받았으면 더 요청하지 않는다")
        void stopsAtUpstreamTotal() {
            expectSearch(1, page(3, List.of(
                    book("a", isbn("97911", 1)), book("b", isbn("97911", 2)), book("c", isbn("97911", 3)))));

            assertThat(service.searchBooks("abc", 1)).hasSize(3);
        }

        @Test
        @DisplayName("뒤 페이지가 JSON이 아니면(마지막 페이지 초과 시 HTML) 모은 것까지만 돌려준다")
        void keepsCollectedWhenLaterPageIsHtml() {
            expectSearch(1, page(200, List.of(book("a", isbn("97911", 1)), book("b", isbn("97911", 2)))));
            server.expect(requestTo(startsWith(BookSearchService.SEARCH_URL)))
                    .andExpect(queryParam("pageNum", "2"))
                    .andRespond(withSuccess("<!DOCTYPE html><html></html>", MediaType.TEXT_HTML));

            assertThat(service.searchBooks("ab", 1)).extracting(BookSearchDto::getTitle).containsExactly("a", "b");
        }
    }

    @Nested
    @DisplayName("걸러내기·합치기")
    class Filtering {

        @Test
        @DisplayName("전자책(온라인 라이선스)·국외 도서·ISBN 없음·세트 ISBN뿐인 자료는 뺀다")
        void excludesNonDomesticPrintBooks() {
            expectSearch(1, page(5, List.of(
                    record("전자책", isbn("97911", 1), "온라인자료", "LD_VIEWER", ""),
                    book("외국 도서", isbn("97800", 2)),
                    book("ISBN 없음", ""),
                    book("세트만", "8936462997(세트)"),
                    book("종이책", isbn("97911", 3)))));

            assertThat(service.searchBooks("책", 1)).extracting(BookSearchDto::getTitle).containsExactly("종이책");
        }

        @Test
        @DisplayName("같은 ISBN은 첫 자리에 합치고, 표기는 실물 소장 레코드·표지와 날짜는 있는 쪽을 쓴다")
        void mergesSameIsbn() {
            String isbn = "9788937461125";
            ObjectNode digitized = record("시계태엽 오렌지", isbn, "온라인자료", "NL_VIEWER", "");
            digitized.put("authorInfo", "Burgess, Anthony 지은이 박시영 옮긴이");
            digitized.put("pubInfo", "서울 : 민음사, 20050101");
            digitized.put("pubYearInfo", "20050101");
            ObjectNode catalog = record("시계태엽 오렌지", "8937460009 8937461129", "오프라인자료", "N",
                    "kolis_on/2016/CNTS-00086720783_thumbnail.jpg");
            catalog.put("authorInfo", "앤서니 버지스 지음;박시영 옮김");
            catalog.put("pubInfo", "민음사");
            catalog.put("pubYearInfo", "2005");
            expectSearch(1, page(3, List.of(digitized, book("다른 책", isbn("97911", 1)), catalog)));

            List<BookSearchDto> results = service.searchBooks("시계태엽 오렌지", 1);

            assertThat(results).extracting(BookSearchDto::getTitle).containsExactly("시계태엽 오렌지", "다른 책");
            BookSearchDto merged = results.get(0);
            assertThat(merged.getIsbn()).isEqualTo(isbn);
            assertThat(merged.getAuthor()).isEqualTo("저자: 앤서니 버지스, 옮긴이: 박시영");
            assertThat(merged.getPublisher()).isEqualTo("민음사");
            assertThat(merged.getPubDate()).isEqualTo("2005-01-01");
            assertThat(merged.getCoverUrl())
                    .isEqualTo("https://cover.nl.go.kr/kolis_on/2016/CNTS-00086720783_thumbnail.jpg");
            assertThat(merged.getTotalPage()).isZero();
        }
    }

    @Nested
    @DisplayName("요청")
    class Request {

        @Test
        @DisplayName("검색어·분류를 UTF-8로 인코딩하고 '+'도 그대로 보낸다")
        void encodesQuery() {
            server.expect(requestTo(containsString("kwd=C%2B%2B%20%EC%9E%85%EB%AC%B8")))
                    .andExpect(requestTo(containsString("category=%EB%8F%84%EC%84%9C")))
                    .andExpect(queryParam("pageSize", String.valueOf(BookSearchService.UPSTREAM_PAGE_SIZE)))
                    .andRespond(withSuccess(page(0, List.of()), TEXT_JSON));

            assertThat(service.searchBooks("  C++ 입문 ", 1)).isEmpty();
        }

        @Test
        @DisplayName("빈 검색어는 외부 API를 부르지 않는다")
        void blankQuery() {
            assertThat(service.searchBooks("   ", 1)).isEmpty();
        }
    }

    @Nested
    @DisplayName("장애는 빈 목록이 아니라 예외(502)로")
    class Failures {

        @Test
        void errorPayload() {
            expectSearch(1, "{\"errorCode\":\"011\",\"errorMsg\":\"INVALID KEY:인증키값이 유효하지 않습니다.\"}");

            assertThatThrownBy(() -> service.searchBooks("데미안", 1))
                    .isInstanceOf(BookSearchUnavailableException.class);
        }

        @Test
        void missingKey() {
            ReflectionTestUtils.setField(service, "apiKey", "");

            assertThatThrownBy(() -> service.searchBooks("데미안", 1))
                    .isInstanceOf(BookSearchUnavailableException.class);
        }

        @Test
        @DisplayName("요청 URL에 든 인증키가 로그에 남지 않는다")
        void doesNotLogKey(CapturedOutput output) {
            server.expect(requestTo(startsWith(BookSearchService.SEARCH_URL)))
                    .andRespond(withException(new SocketTimeoutException("Read timed out")));

            assertThatThrownBy(() -> service.searchBooks("데미안", 1))
                    .isInstanceOf(BookSearchUnavailableException.class);
            assertThat(output).contains("ResourceAccessException").doesNotContain(KEY);
        }
    }

    @Nested
    @DisplayName("YES24 우선, 국립중앙도서관 폴백")
    class Yes24First {

        private final Yes24BookClient yes24 = mock(Yes24BookClient.class);

        @BeforeEach
        void useYes24() {
            when(yes24.isEnabled()).thenReturn(true);
            ReflectionTestUtils.setField(service, "yes24", yes24);
        }

        @Test
        @DisplayName("YES24 결과가 있으면 국립중앙도서관을 부르지 않는다")
        void usesYes24() {
            when(yes24.search("데미안", 1, BookSearchService.PAGE_SIZE))
                    .thenReturn(List.of(BookSearchDto.builder().title("데미안").isbn("9788937460449").build()));

            assertThat(service.searchBooks("데미안", 1)).extracting(BookSearchDto::getTitle).containsExactly("데미안");
        }

        @Test
        @DisplayName("YES24 장애·한도 초과면 국립중앙도서관으로")
        void fallsBackOnFailure() {
            when(yes24.search(anyString(), anyInt(), anyInt())).thenThrow(new Yes24BookClient.Yes24Failure("HTTP 429"));
            expectSearch(1, page(1, List.of(book("데미안", isbn("97911", 1)))));

            assertThat(service.searchBooks("데미안", 1)).extracting(BookSearchDto::getTitle).containsExactly("데미안");
        }

        @Test
        @DisplayName("첫 페이지가 비면 국립중앙도서관에서 찾고, 뒤 페이지가 비면 그대로 끝")
        void fallsBackOnlyForEmptyFirstPage() {
            when(yes24.search(anyString(), anyInt(), anyInt())).thenReturn(List.of());
            expectSearch(1, page(1, List.of(book("옛 책", isbn("97911", 2)))));

            assertThat(service.searchBooks("옛 책", 1)).extracting(BookSearchDto::getTitle).containsExactly("옛 책");
            assertThat(service.searchBooks("옛 책", 2)).isEmpty();
        }

        @Test
        @DisplayName("YES24 상세에 쪽수가 없으면 ISBN 서지정보로 채운다")
        void lookupMergesSources() {
            when(yes24.lookupByIsbn("9788937461125"))
                    .thenReturn(Optional.of(new BookSearchService.IsbnLookup(null, LocalDate.of(2005, 1, 1))));
            expectIsbn("{\"TOTAL_COUNT\":\"1\",\"docs\":[{\"PAGE\":\"244 p.\",\"REAL_PUBLISH_DATE\":\"20041224\"}]}");

            assertThat(service.lookupByIsbn("9788937461125"))
                    .contains(new BookSearchService.IsbnLookup(244, LocalDate.of(2005, 1, 1)));
        }
    }

    @Nested
    @DisplayName("ISBN 서지정보 조회")
    class LookupByIsbn {

        @Test
        @DisplayName("쪽수와 실제 발행일을 읽는다")
        void readsPagesAndRealDate() {
            expectIsbn("{\"TOTAL_COUNT\":\"1\",\"docs\":[{\"PAGE\":\"302 p.\",\"REAL_PUBLISH_DATE\":\"20251027\","
                    + "\"PUBLISH_PREDATE\":\"20251020\"}]}");

            assertThat(service.lookupByIsbn("9791193262757"))
                    .contains(new BookSearchService.IsbnLookup(302, LocalDate.of(2025, 10, 27)));
        }

        @Test
        @DisplayName("실제 발행일이 없으면 발행예정일, 쪽수가 없으면 null")
        void fallsBackToPlannedDate() {
            expectIsbn("{\"TOTAL_COUNT\":\"1\",\"docs\":[{\"PAGE\":\"\",\"REAL_PUBLISH_DATE\":\"\","
                    + "\"PUBLISH_PREDATE\":\"20041224\"}]}");

            assertThat(service.lookupByIsbn("9788937461125"))
                    .contains(new BookSearchService.IsbnLookup(null, LocalDate.of(2004, 12, 24)));
        }

        @Test
        @DisplayName("없거나 오류면 비어 있다 (책 추가는 막지 않는다)")
        void emptyWhenMissingOrError() {
            expectIsbn("{\"TOTAL_COUNT\":\"0\",\"docs\":[]}");
            expectIsbn("{\"RESULT\":\"ERROR\",\"ERR_CODE\":\"011\",\"ERR_MESSAGE\":\"유효하지 않은 인증키 값입니다.\"}");

            assertThat(service.lookupByIsbn("9788937461125")).isEmpty();
            assertThat(service.lookupByIsbn("9788937461125")).isEmpty();
        }
    }

    private void expectSearch(int pageNum, String body) {
        server.expect(requestTo(startsWith(BookSearchService.SEARCH_URL)))
                .andExpect(queryParam("pageNum", String.valueOf(pageNum)))
                .andRespond(withSuccess(body, TEXT_JSON));
    }

    private void expectIsbn(String body) {
        server.expect(requestTo(startsWith(BookSearchService.ISBN_URL)))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static String page(long total, List<ObjectNode> records) {
        ObjectNode root = JSON.createObjectNode();
        root.put("total", total);
        ArrayNode result = root.putArray("result");
        records.forEach(result::add);
        return root.toString();
    }

    private static ObjectNode book(String title, String isbn) {
        return record(title, isbn, "오프라인자료", "N", "");
    }

    private static ObjectNode record(String title, String isbn, String menuName, String docYn, String imageUrl) {
        ObjectNode node = JSON.createObjectNode();
        node.put("titleInfo", title);
        node.put("authorInfo", "홍길동 지음");
        node.put("pubInfo", "출판사");
        node.put("pubYearInfo", "2020");
        node.put("isbn", isbn);
        node.put("menuName", menuName);
        node.put("docYn", docYn);
        node.put("imageUrl", imageUrl);
        return node;
    }

    /** 체크섬이 맞는 ISBN-13 (prefix 5자리 + 일련번호). */
    private static String isbn(String prefix, int serial) {
        String body = prefix + String.format("%07d", serial);
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = body.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return body + (10 - sum % 10) % 10;
    }
}
