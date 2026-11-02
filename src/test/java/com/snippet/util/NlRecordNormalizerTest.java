package com.snippet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 국립중앙도서관 레코드 정규화 테스트. 입력은 전부 실제 소장자료 검색 응답에서 가져온 표기다.
 */
class NlRecordNormalizerTest {

    @Nested
    @DisplayName("저자 — 알라딘 시절 형식(\"저자: …, 옮긴이: …\")으로")
    class Author {

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', textBlock = """
                앤서니 버지스 지음;박시영 옮김                     | 저자: 앤서니 버지스, 옮긴이: 박시영
                게리 덱스터 지음 ;박중서 옮김                      | 저자: 게리 덱스터, 옮긴이: 박중서
                지은이: 사이토 다카시 ;옮긴이: 정현                | 저자: 사이토 다카시, 옮긴이: 정현
                Burgess, Anthony 지은이 박시영 옮긴이             | 저자: Burgess, Anthony, 옮긴이: 박시영
                임옥희[1956-] 지음                                | 저자: 임옥희
                야마사키 다케야 지음;김진수 옮김.                  | 저자: 야마사키 다케야, 옮긴이: 김진수
                정미숙 지은이 한정희 지은이                         | 저자: 정미숙, 한정희
                장샤를 베르티에 글 ;아멜리 퐁텐 그림 ;양진희 옮김    | 저자: 장샤를 베르티에, 그림: 아멜리 퐁텐, 옮긴이: 양진희
                김이숙 옮김 Lyman, Haward F.                      | 저자: Lyman, Haward F., 옮긴이: 김이숙
                [헤르만 헤세 지음];박주효 엮음;오기철 그림           | 저자: 헤르만 헤세, 엮은이: 박주효, 그림: 오기철
                韓江 저                                          | 저자: 韓江
                아르누 드 마이어 외 지음;신문영 옮김               | 저자: 아르누 드 마이어 외, 옮긴이: 신문영
                김영하                                           | 저자: 김영하
                """)
        void formats(String raw, String expected) {
            assertThat(NlRecordNormalizer.author(raw)).isEqualTo(expected);
        }

        @Test
        @DisplayName("쉼표로 붙은 공저자는 나눈다")
        void splitsCoAuthors() {
            assertThat(NlRecordNormalizer.author(
                    "데이비드 L. 도트리치,피터 C. 카이로,스티븐 H. 라인스미스 지음 ;김복리 옮김"))
                    .isEqualTo("저자: 데이비드 L. 도트리치, 피터 C. 카이로, 스티븐 H. 라인스미스, 옮긴이: 김복리");
        }

        @Test
        @DisplayName("검색어 강조 태그는 걷어낸다")
        void stripsHighlightTags() {
            assertThat(NlRecordNormalizer.author("<span class=\"searching_txt\">김영하</span>"))
                    .isEqualTo("저자: 김영하");
        }

        @Test
        @DisplayName("저자가 없으면 원래 순서를 지킨다")
        void keepsOrderWithoutMainAuthor() {
            assertThat(NlRecordNormalizer.author("기획 및 해설: 김영하 ;스토리: 김난영,박시연 ;그림: 박성일"))
                    .isEqualTo("기획 및 해설: 김영하, 스토리: 김난영, 박시연, 그림: 박성일");
            assertThat(NlRecordNormalizer.author("흔한남매 원작 ;차차 그림"))
                    .isEqualTo("원작: 흔한남매, 그림: 차차");
        }

        @Test
        @DisplayName("저자를 맨 앞에 둔다 (책 추가 시 첫 항목만 저장)")
        void putsMainAuthorFirst() {
            assertThat(NlRecordNormalizer.author("박시영 옮김;앤서니 버지스 지음"))
                    .isEqualTo("저자: 앤서니 버지스, 옮긴이: 박시영");
        }

        @Test
        void emptyStaysEmpty() {
            assertThat(NlRecordNormalizer.author(null)).isEmpty();
            assertThat(NlRecordNormalizer.author("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("제목")
    class Title {

        @Test
        void stripsTagsAndEntities() {
            assertThat(NlRecordNormalizer.title(
                    "<span class=\"searching_txt\">시계태엽</span> <span class=\"searching_txt\">오렌지</span>"))
                    .isEqualTo("시계태엽 오렌지");
            assertThat(NlRecordNormalizer.title("문체부 장관, 소설 &lt;채식주의자&gt;의 맨부커"))
                    .isEqualTo("문체부 장관, 소설 <채식주의자>의 맨부커");
        }

        @Test
        @DisplayName("부제목은 남기고 대등서명·책임표시는 뺀다")
        void dropsParallelTitleAndStatement() {
            assertThat(NlRecordNormalizer.title("채식주의자 : 한강 연작소설")).isEqualTo("채식주의자 : 한강 연작소설");
            assertThat(NlRecordNormalizer.title("포스트잇 : 김영하 산문집 = Post it")).isEqualTo("포스트잇 : 김영하 산문집");
            assertThat(NlRecordNormalizer.title("얄개전. 2-4 / 김영하 글·그림")).isEqualTo("얄개전. 2-4");
        }

        @Test
        @DisplayName("book_tb.b_title 길이(200)를 넘지 않는다")
        void truncatesToColumnLength() {
            assertThat(NlRecordNormalizer.title("가".repeat(250))).hasSize(200);
        }
    }

    @ParameterizedTest(name = "출판사 {0} → {1}")
    @CsvSource(delimiter = '|', textBlock = """
            서울 : 민음사, 20220427     | 민음사
            [서울] : 지혜문화사, 1983   | 지혜문화사
            서울 : 소년소녀사, 1979-99999999 | 소년소녀사
            민음사                     | 민음사
            허밍버드 : 백도씨           | 허밍버드 : 백도씨
            """)
    void publisher(String raw, String expected) {
        assertThat(NlRecordNormalizer.publisher(raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "발행년 {0} → {1}")
    @CsvSource(delimiter = '|', emptyValue = "", textBlock = """
            20220427        | 2022-04-27
            2005            | 2005
            [2021]          | 2021
            '[2020], ©2020' | 2020
            1979-99999999   | 1979
            ''              | ''
            """)
    void pubDate(String raw, String expected) {
        assertThat(NlRecordNormalizer.pubDate(raw)).isEqualTo(expected);
    }

    @Nested
    @DisplayName("ISBN")
    class Isbn {

        @Test
        @DisplayName("ISBN-10은 13자리로 바꾼다")
        void convertsIsbn10() {
            assertThat(NlRecordNormalizer.domesticIsbn13("8937461129")).isEqualTo("9788937461125");
            assertThat(NlRecordNormalizer.domesticIsbn13("891901017X")).isEqualTo("9788919010174");
        }

        @Test
        @DisplayName("\"세트 낱권\" 순서로 여러 개면 마지막(낱권)을 쓴다")
        void prefersLastIsbn() {
            assertThat(NlRecordNormalizer.domesticIsbn13("8937460009 8937461129")).isEqualTo("9788937461125");
            assertThat(NlRecordNormalizer.domesticIsbn13("9791191114355 9791191114065")).isEqualTo("9791191114065");
        }

        @Test
        @DisplayName("세트 ISBN뿐이거나 국외 도서·체크섬 오류·빈 값이면 null")
        void rejects() {
            assertThat(NlRecordNormalizer.domesticIsbn13("8936462997(세트)")).isNull();
            assertThat(NlRecordNormalizer.domesticIsbn13("9781846275623 1846275628")).isNull();
            assertThat(NlRecordNormalizer.domesticIsbn13("9788937461126")).isNull();
            assertThat(NlRecordNormalizer.domesticIsbn13("")).isNull();
            assertThat(NlRecordNormalizer.domesticIsbn13(null)).isNull();
        }
    }

    @Test
    @DisplayName("표지는 cover.nl.go.kr 상대 경로")
    void coverUrl() {
        assertThat(NlRecordNormalizer.coverUrl("kolis_on/2016/CNTS-00086720783_thumbnail.jpg"))
                .isEqualTo("https://cover.nl.go.kr/kolis_on/2016/CNTS-00086720783_thumbnail.jpg");
        assertThat(NlRecordNormalizer.coverUrl("")).isEmpty();
        assertThat(NlRecordNormalizer.coverUrl(null)).isEmpty();
    }

    @ParameterizedTest(name = "쪽수 {0} → {1}")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            302 p.       | 302
            181 p.       | 181
            152          | 152
            'xii, 302 p.' | 302
            1            | null
            ''           | null
            """)
    void pageCount(String raw, Integer expected) {
        assertThat(NlRecordNormalizer.pageCount(raw)).isEqualTo(expected);
    }

    @Test
    void compactDate() {
        assertThat(NlRecordNormalizer.compactDate("20041224")).isEqualTo(LocalDate.of(2004, 12, 24));
        assertThat(NlRecordNormalizer.compactDate("2004")).isNull();
        assertThat(NlRecordNormalizer.compactDate("")).isNull();
        assertThat(NlRecordNormalizer.compactDate("20041399")).isNull();
    }
}
