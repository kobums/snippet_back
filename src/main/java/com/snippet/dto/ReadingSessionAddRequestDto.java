package com.snippet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReadingSessionAddRequestDto {
    private Long userBookId;
    private Integer durationSeconds;
    private Integer startPage;
    private Integer endPage;
    private String sessionDate; // "YYYY-MM-DD"
}
