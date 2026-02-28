package com.snippet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatsDto {
    private long monthlyCompletedCount;
    private long currentlyReadingCount;
    private long totalCompletedCount;
    // Add more stats as needed (e.g., yearly completed)
}
