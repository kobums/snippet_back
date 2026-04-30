package com.snippet.dto;

import lombok.Getter;

@Getter
public class SuggestionAddRequestDto {
    private String category;
    private String title;
    private String content;
}
