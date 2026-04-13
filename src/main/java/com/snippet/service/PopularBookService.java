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

    private static final String BASE_URL = "http://data4library.kr/api/loanItemSrch";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Cacheable(value = "popularBooks",
            key = "#startDt + '_' + #endDt + '_' + #kdc + '_' + #age + '_' + #gender + '_' + #region + '_' + #pageNo",
            unless = "#result.isEmpty()")
    public List<PopularBookDto> getPopularBooks(
            String startDt, String endDt,
            String kdc, String dtlKdc,
            String age, String gender,
            String region, String dtlRegion,
            int pageNo, int pageSize) {

        if (authKey == null || authKey.isBlank()) {
            return getMockResults();
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

            String url = builder.build(false).toUriString();
            String raw = restTemplate.getForObject(url, String.class);

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

    private List<PopularBookDto> getMockResults() {
        List<PopularBookDto> mocks = new ArrayList<>();
        String[] titles = {"채식주의자", "82년생 김지영", "아몬드", "파친코", "불편한 편의점"};
        String[] authors = {"지은이: 한강", "지은이: 조남주", "지은이: 손원평", "지은이: 이민진", "지은이: 김호연"};
        for (int i = 0; i < titles.length; i++) {
            mocks.add(PopularBookDto.builder()
                    .rank(i + 1)
                    .title(titles[i])
                    .author(authors[i])
                    .publisher("창비")
                    .isbn13("978895640" + String.format("%04d", i))
                    .kdc("813.62")
                    .kdcName("문학 > 한국문학 > 소설")
                    .loanCount(5000 - i * 300)
                    .coverUrl("")
                    .build());
        }
        return mocks;
    }
}
