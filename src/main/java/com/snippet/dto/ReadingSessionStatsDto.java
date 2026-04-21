package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingSessionStatsDto {
    private Long totalSessions;
    private Long totalSeconds;
    private Long totalPagesRead;
    private Double avgSecondsPerPage;
}
