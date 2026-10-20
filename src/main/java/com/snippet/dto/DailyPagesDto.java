package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DailyPagesDto {
    private String date; // "YYYY-MM-DD"
    private int pages;
}
