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
    private Boolean isPublic; // 스와이프 카드 공개 (snippet 만, 생략하면 비공개)
}
