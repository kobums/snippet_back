package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingInsightsDto {
    private double averageReadingDays;  // 평균 독서 일수
    private String topCategory;         // 가장 많이 읽은 장르
    private int longestReadingDays;     // 최장 독서 기록 (일)
    private String longestBook;         // 최장 독서 책 제목
}
