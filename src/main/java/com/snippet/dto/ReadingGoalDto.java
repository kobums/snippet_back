package com.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReadingGoalDto {
    private int year;
    private int targetBooks;
    private int completedBooks;
}
