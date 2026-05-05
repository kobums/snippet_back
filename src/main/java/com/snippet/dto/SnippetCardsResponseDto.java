package com.snippet.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SnippetCardsResponseDto {
    private List<SnippetCardDto> cards;
    private int remainingToday; // -1 = 비로그인 (무제한)
}
