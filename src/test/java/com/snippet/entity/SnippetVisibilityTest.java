package com.snippet.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 스와이프 카드 공개 규칙: 밑줄만 공개 가능, 기본은 비공개. */
class SnippetVisibilityTest {

    private Snippet record(String type, boolean isPublic) {
        return Snippet.builder().type(type).text("문장").isPublic(isPublic).build();
    }

    @Test
    @DisplayName("기본은 비공개다 (작성자가 고르지 않으면 카드에 안 나온다)")
    void privateByDefault() {
        assertThat(Snippet.builder().type("snippet").text("문장").build().isPublic()).isFalse();
    }

    @Test
    @DisplayName("밑줄은 공개를 고를 수 있다")
    void snippetCanBePublic() {
        assertThat(record("snippet", true).isPublic()).isTrue();
    }

    @Test
    @DisplayName("일기·리뷰는 공개로 만들어지지 않는다")
    void diaryAndReviewStayPrivate() {
        assertThat(record("diary", true).isPublic()).isFalse();
        assertThat(record("review", true).isPublic()).isFalse();
    }

    @Test
    @DisplayName("유형이 비면(저장 시 snippet 으로 채워짐) 공개 선택이 유지된다")
    void missingTypeMeansSnippet() {
        assertThat(record(null, true).isPublic()).isTrue();
    }

    @Test
    @DisplayName("수정: 공개를 켜고 끌 수 있고, null 이면 그대로 둔다")
    void toggleOnUpdate() {
        Snippet s = record("snippet", false);
        s.update(null, null, null, null, true);
        assertThat(s.isPublic()).isTrue();
        s.update(null, "고친 문장", null, null, null);
        assertThat(s.isPublic()).isTrue();
        s.update(null, null, null, null, false);
        assertThat(s.isPublic()).isFalse();
    }

    @Test
    @DisplayName("수정: 일기로 바꾸면 카드에서 내리고, 일기 상태에선 다시 켤 수 없다")
    void changingTypeUnpublishes() {
        Snippet s = record("snippet", true);
        s.update("diary", null, null, null, null);
        assertThat(s.isPublic()).isFalse();
        s.update(null, null, null, null, true);
        assertThat(s.isPublic()).isFalse();
    }
}
