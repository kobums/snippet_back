package com.snippet.dto;

import com.snippet.entity.Snippet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SnippetCardDto {

    private Long id;
    private String text;
    private String tag;
    private String bookTitle;

    public static SnippetCardDto from(Snippet snippet) {
        return SnippetCardDto.builder()
                .id(snippet.getId())
                .text(snippet.getText())
                .tag(snippet.getTag())
                .bookTitle(snippet.getBook().getTitle())
                .build();
    }
}
