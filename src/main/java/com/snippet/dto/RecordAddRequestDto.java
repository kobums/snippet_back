package com.snippet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecordAddRequestDto {
    private Long bookId;
    private String type; // "snippet", "diary", "review"
    private String text;
    private String tag;
    private Integer relatedPage;
}
