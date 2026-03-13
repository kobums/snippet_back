package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDto {
    private String category;
    private int totalCount;         // 시작한 책 권수
    private int completedCount;     // 완료한 책 권수
    private double completionRate;  // 완독률 (%)
}
