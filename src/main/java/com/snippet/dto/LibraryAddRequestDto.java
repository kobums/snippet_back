package com.snippet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LibraryAddRequestDto {
    private String title;
    private String author;
    private String publisher;
    private String pubDate;
    private String isbn;
    private String coverUrl;
    private String status; // wish, reading, waiting
}
