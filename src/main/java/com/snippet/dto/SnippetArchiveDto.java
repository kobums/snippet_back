package com.snippet.dto;

import com.snippet.entity.Snippet;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SnippetArchiveDto {

    private Long id;
    private String text;
    private String tag;
    private String bookTitle;
    private String bookAuthor;
    private String coverUrl;
    private String affiliateUrl;

    public static SnippetArchiveDto from(Snippet snippet) {
        return SnippetArchiveDto.builder()
                .id(snippet.getId())
                .text(snippet.getText())
                .tag(snippet.getTag())
                .bookTitle(snippet.getBook().getTitle())
                .bookAuthor(snippet.getBook().getAuthor())
                .coverUrl(snippet.getBook().getCoverUrl())
                .affiliateUrl(snippet.getBook().getAffiliateUrl())
                .build();
    }
}
