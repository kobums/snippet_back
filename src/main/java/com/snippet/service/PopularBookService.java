package com.snippet.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snippet.dto.PopularBookDto;
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

@Slf4j
@Service
public class PopularBookService {

    private final RestTemplate restTemplate = new RestTemplate();

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
