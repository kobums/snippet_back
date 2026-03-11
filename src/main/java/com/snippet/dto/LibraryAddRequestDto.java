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
    private Integer totalPage;
    private String type; // wish, have, borrow, return
    private String status; // none(wish 전용), waiting, reading, completed, dropped
    private String startDate; // yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss
    private String endDate; // yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss
}
