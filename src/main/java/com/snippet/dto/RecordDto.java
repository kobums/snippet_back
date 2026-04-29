package com.snippet.dto;

import com.snippet.entity.Snippet;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecordDto {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookCoverUrl;
    private String type;
    private String text;
    private String tag;
    private Integer relatedPage;
    private LocalDateTime createDate;

    public static RecordDto from(Snippet snippet) {
        return RecordDto.builder()
                .id(snippet.getId())
                .bookId(snippet.getBook().getId())
                .bookTitle(snippet.getBook().getTitle())
                .bookAuthor(snippet.getBook().getAuthor())
                .bookCoverUrl(snippet.getBook().getCoverUrl())
                .type(snippet.getType())
                .text(snippet.getText())
                .tag(snippet.getTag())
                .relatedPage(snippet.getRelatedPage())
                .createDate(snippet.getCreateDate())
                .build();
    }
}
