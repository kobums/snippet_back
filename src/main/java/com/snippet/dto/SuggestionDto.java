package com.snippet.dto;

import com.snippet.entity.Suggestion;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SuggestionDto {
    private Long id;
    private String category;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createDate;

    public static SuggestionDto from(Suggestion s) {
        return SuggestionDto.builder()
                .id(s.getId())
                .category(s.getCategory())
                .title(s.getTitle())
                .content(s.getContent())
                .status(s.getStatus())
                .createDate(s.getCreateDate())
                .build();
    }
}
