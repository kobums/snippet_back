package com.snippet.dto;

import com.snippet.entity.UserBook;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserBookDto {
    private Long id;
    private Long bookId;
    private String title;
    private String author;
    private String coverUrl;
    private String status;
    private Integer readPage;
    private Integer totalPage;
    private LocalDateTime createDate;

    public static UserBookDto from(UserBook userBook) {
        return UserBookDto.builder()
                .id(userBook.getId())
                .bookId(userBook.getBook().getId())
                .title(userBook.getBook().getTitle())
                .author(userBook.getBook().getAuthor())
                .coverUrl(userBook.getBook().getCoverUrl())
                .status(userBook.getStatus())
                .readPage(userBook.getReadPage())
                .totalPage(userBook.getBook().getTotalPage() != null ? userBook.getBook().getTotalPage() : 0)
                .createDate(userBook.getCreateDate())
                .build();
    }
}
