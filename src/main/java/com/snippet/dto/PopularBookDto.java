package com.snippet.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopularBookDto {
    private int rank;
    private String title;
    private String author;
    private String publisher;
    private String isbn13;
    private String kdc;
    private String kdcName;
    private int loanCount;
    private String coverUrl;
}
