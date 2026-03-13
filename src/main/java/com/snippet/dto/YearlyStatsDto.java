package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class YearlyStatsDto {
    private int year;
    private int completedCount;
    private int totalPages;
}
