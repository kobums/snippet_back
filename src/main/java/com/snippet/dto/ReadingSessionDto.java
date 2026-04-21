package com.snippet.dto;

import com.snippet.entity.ReadingSession;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReadingSessionDto {
    private Long id;
    private Long userBookId;
    private Long bookId;
    private String bookTitle;
    private String bookCoverUrl;
    private Integer durationSeconds;
    private Integer startPage;
    private Integer endPage;
    private Integer pagesRead;
    private Double secondsPerPage;
    private String sessionDate;
    private String createDate;

    public static ReadingSessionDto from(ReadingSession rs) {
        return ReadingSessionDto.builder()
                .id(rs.getId())
                .userBookId(rs.getUserBook().getId())
                .bookId(rs.getBook().getId())
                .bookTitle(rs.getBook().getTitle())
                .bookCoverUrl(rs.getBook().getCoverUrl())
                .durationSeconds(rs.getDurationSeconds())
                .startPage(rs.getStartPage())
                .endPage(rs.getEndPage())
                .pagesRead(rs.getPagesRead())
                .secondsPerPage(rs.getSecondsPerPage())
                .sessionDate(rs.getSessionDate().toString())
                .createDate(rs.getCreateDate().toString())
                .build();
    }
}
