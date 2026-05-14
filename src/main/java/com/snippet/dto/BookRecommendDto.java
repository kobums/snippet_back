package com.snippet.dto;

import com.snippet.entity.Book;
import lombok.Getter;

@Getter
public class BookRecommendDto {
    private final Long id;
    private final String title;
    private final String author;
    private final String coverUrl;
    private final String category;
    private final String reason;

    public BookRecommendDto(Book book, String reason) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.coverUrl = book.getCoverUrl();
        this.category = book.getCategory();
        this.reason = reason;
    }
}
