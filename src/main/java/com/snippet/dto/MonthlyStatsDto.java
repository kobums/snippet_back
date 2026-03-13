package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatsDto {
    private int month;                      // 1~12
    private int completedCount;             // 완료한 책 권수
    private int totalPages;                 // 읽은 총 페이지 수
    private Map<String, Integer> categoryCount = new HashMap<>(); // 카테고리별 권수
}
