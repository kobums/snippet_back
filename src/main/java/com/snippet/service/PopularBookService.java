package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.dto.PopularBookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 인기 도서 — YES24 베스트셀러 우선, 실패하면 도서관정보나루 인기 대출 도서.
 *
 * <p>도서관정보나루는 필터(분야·연령)를 걸면 30초 넘게 응답이 없기도 해서 YES24(0.2~0.4초)로 바꿨다.
 * 앱은 아직 도서관정보나루 형식의 파라미터를 보내므로(클라이언트마다 코드도 다르다) 여기서 YES24 값으로 바꾼다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularBookService {

    /** YES24 국내도서 종합. */
    static final String YES24_ALL = "001";
    static final String YES24_INFANT = "001001027";
    static final String YES24_CHILDREN = "001001016";

    /** KDC 대분류 → YES24 국내도서 분야. 총류(0)는 맞는 분야가 없어 전체로 본다. */
    static final Map<String, String> KDC_TO_YES24 = Map.of(
            "1", "001001019",  // 철학 → 인문
            "2", "001001021",  // 종교
            "3", "001001022",  // 사회과학 → 사회 정치
            "4", "001001002",  // 자연과학
            "5", "001001003",  // 기술과학 → IT 모바일
            "6", "001001007",  // 예술
            "7", "001001004",  // 언어 → 국어 외국어 사전
            "8", "001001046",  // 문학 → 소설/시/희곡
            "9", "001001010"); // 역사

    /** 연령 코드 — iOS(child/young/adult)·Android(0~3)·웹(도서관정보나루 6/8/14/20…) 값을 모두 받는다. */
    private static final Set<String> INFANT_AGES = Set.of("0", "6");
    private static final Set<String> CHILD_AGES = Set.of("1", "8", "child");
    private static final Set<String> TEEN_AGES = Set.of("2", "14", "young");
    private static final Set<String> DECADE_AGES = Set.of("20", "30", "40", "50", "60");

    /** 이 기간보다 길면 월별 베스트셀러 (YES24는 종합·일별·월별만 있다). */
    static final int WEEKLY_MAX_DAYS = 14;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Yes24BookClient yes24;

    @Value("${data4library.auth-key:}")
    private String authKey;

    private static final String BASE_URL = "https://data4library.kr/api/loanItemSrch";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 캐시 키에는 결과에 영향을 주는 모든 파라미터가 들어가야 한다.
    // (pageSize가 빠져 있으면 pageSize=1 응답이 캐시된 뒤 pageSize=20 요청도 1건만 반환)
    @Cacheable(value = "popularBooks",
            key = "#startDt + '_' + #endDt + '_' + #kdc + '_' + #dtlKdc + '_' + #age + '_' + #gender + '_' + #region + '_' + #dtlRegion + '_' + #pageNo + '_' + #pageSize",
            unless = "#result.isEmpty()")
    public List<PopularBookDto> getPopularBooks(
            String startDt, String endDt,
            String kdc, String dtlKdc,
            String age, String gender,
            String region, String dtlRegion,
            int pageNo, int pageSize) {

        if (yes24.isEnabled()) {
            try {
                return fromYes24(startDt, endDt, kdc, age, gender, pageNo, pageSize);
            } catch (Yes24BookClient.Yes24Failure e) {
                log.warn("YES24 베스트셀러 실패, 도서관정보나루로 대체: {}", e.getMessage());
            }
        }
        return fromLibrary(startDt, endDt, kdc, dtlKdc, age, gender, region, dtlRegion, pageNo, pageSize);
    }

    private List<PopularBookDto> fromYes24(String startDt, String endDt, String kdc, String age, String gender,
                                           int pageNo, int pageSize) {
        return yes24.bestsellers(yes24Category(kdc, age), yes24Age(age), yes24Sex(gender),
                        isMonthly(startDt, endDt), pageNo, pageSize).stream()
                .map(b -> PopularBookDto.builder()
                        .rank(b.rank())
                        .title(b.item().book().getTitle())
                        .author(b.item().book().getAuthor())
                        .publisher(b.item().book().getPublisher())
                        .isbn13(b.item().book().getIsbn())
                        .kdc("")
                        // "국내도서>소설/시/희곡" → "소설/시/희곡"
                        .kdcName(b.item().category() == null ? ""
                                : b.item().category().substring(b.item().category().lastIndexOf('>') + 1))
                        .loanCount(0)
                        .coverUrl(b.item().book().getCoverUrl())
                        .build())
                .toList();
    }

    /** 영유아·초등 연령은 YES24 연령 필터(10대부터)에 없어 유아·어린이 분야로 대신한다. */
    static String yes24Category(String kdc, String age) {
        if (age != null && INFANT_AGES.contains(age)) {
            return YES24_INFANT;
        }
        if (age != null && CHILD_AGES.contains(age)) {
            return YES24_CHILDREN;
        }
        return kdc == null ? YES24_ALL : KDC_TO_YES24.getOrDefault(kdc.trim(), YES24_ALL);
    }

    static Integer yes24Age(String age) {
        if (age == null) {
            return null;
        }
        if (TEEN_AGES.contains(age)) {
            return 10;
        }
        return DECADE_AGES.contains(age) ? Integer.valueOf(age) : null;
    }

    /** 성별 — iOS male/female, Android M/F, 웹 1(남)/2(여). */
    static String yes24Sex(String gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender.trim()) {
            case "male", "M", "1" -> "M";
            case "female", "F", "2" -> "F";
            default -> null;
        };
    }

    /** 2주 넘는 기간은 월별, 그 외(기간 없음 포함 — Android 기본 "주간")는 종합 순위. */
    static boolean isMonthly(String startDt, String endDt) {
        if (startDt == null || startDt.isBlank()) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(startDt, DATE_FMT);
            LocalDate end = endDt == null || endDt.isBlank() ? LocalDate.now() : LocalDate.parse(endDt, DATE_FMT);
            return java.time.temporal.ChronoUnit.DAYS.between(start, end) > WEEKLY_MAX_DAYS;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    private List<PopularBookDto> fromLibrary(
            String startDt, String endDt,
            String kdc, String dtlKdc,
            String age, String gender,
            String region, String dtlRegion,
            int pageNo, int pageSize) {
        // 인증키 미설정 시 목업 데이터 대신 빈 목록 반환 (가짜 책이 노출되지 않도록)
        if (authKey == null || authKey.isBlank()) {
            log.warn("data4library auth-key 미설정 — 인기 도서 빈 목록 반환");
            return List.of();
        }

        // 날짜 기본값: 최근 30일
        if (startDt == null || startDt.isBlank()) {
            startDt = LocalDate.now().minusDays(30).format(DATE_FMT);
        }
        if (endDt == null || endDt.isBlank()) {
            endDt = LocalDate.now().format(DATE_FMT);
        }

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL)
                    .queryParam("authKey", authKey)
                    .queryParam("format", "json")
                    .queryParam("startDt", startDt)
                    .queryParam("endDt", endDt)
                    .queryParam("pageNo", pageNo)
                    .queryParam("pageSize", pageSize);

            addIfPresent(builder, "kdc", kdc);
            addIfPresent(builder, "dtl_kdc", dtlKdc);
            addIfPresent(builder, "age", age);
            addIfPresent(builder, "gender", gender);
            addIfPresent(builder, "region", region);
            addIfPresent(builder, "dtl_region", dtlRegion);

            java.net.URI uri = builder.encode().build().toUri();
            String raw = restTemplate.getForObject(uri, String.class);

            if (raw == null || raw.isBlank()) return List.of();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode response = mapper.readTree(raw);

            return parseJsonResponse(response);

        } catch (Exception e) {
            log.error("도서관정보나루 API 호출 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return List.of();
        }
    }

    private void addIfPresent(UriComponentsBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(key, value);
        }
    }

    private List<PopularBookDto> parseJsonResponse(JsonNode root) {
        List<PopularBookDto> results = new ArrayList<>();
        try {
            JsonNode docs = root.path("response").path("docs");
            if (!docs.isArray()) return results;

            for (JsonNode wrapper : docs) {
                JsonNode doc = wrapper.path("doc");
                results.add(PopularBookDto.builder()
                        .rank(doc.path("ranking").asInt(0))
                        .title(doc.path("bookname").asText("").trim())
                        .author(doc.path("authors").asText(""))
                        .publisher(doc.path("publisher").asText(""))
                        .isbn13(doc.path("isbn13").asText(""))
                        .kdc(doc.path("class_no").asText(""))
                        .kdcName(doc.path("class_nm").asText(""))
                        .loanCount(doc.path("loan_count").asInt(0))
                        .coverUrl(doc.path("bookImageURL").asText(""))
                        .build());
            }
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
        }
        return results;
    }

}
