package com.snippet.service;

import com.snippet.dto.BookSearchDto;
import com.snippet.dto.PopularBookDto;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PopularBookServiceTest {

    @Nested
    class ParamMapping {

        @Test
        void kdcMapsToYes24Category() {
            assertThat(PopularBookService.yes24Category("8", "")).isEqualTo("001001046");
            assertThat(PopularBookService.yes24Category("", "")).isEqualTo(PopularBookService.YES24_ALL);
            assertThat(PopularBookService.yes24Category("0", null)).isEqualTo(PopularBookService.YES24_ALL);
        }

        @Test
        void youngAgesUseChildrenCategories() {
            // 웹(6/8)·Android(0/1)·iOS(child) 코드가 모두 유아·어린이 분야로 간다 — 분야 선택보다 우선
            assertThat(PopularBookService.yes24Category("8", "6")).isEqualTo(PopularBookService.YES24_INFANT);
            assertThat(PopularBookService.yes24Category("", "0")).isEqualTo(PopularBookService.YES24_INFANT);
            assertThat(PopularBookService.yes24Category("", "8")).isEqualTo(PopularBookService.YES24_CHILDREN);
            assertThat(PopularBookService.yes24Category("", "1")).isEqualTo(PopularBookService.YES24_CHILDREN);
            assertThat(PopularBookService.yes24Category("", "child")).isEqualTo(PopularBookService.YES24_CHILDREN);
        }

        @Test
        void ageMapsToYes24Decades() {
            assertThat(PopularBookService.yes24Age("14")).isEqualTo(10);
            assertThat(PopularBookService.yes24Age("young")).isEqualTo(10);
            assertThat(PopularBookService.yes24Age("2")).isEqualTo(10);
            assertThat(PopularBookService.yes24Age("30")).isEqualTo(30);
            assertThat(PopularBookService.yes24Age("adult")).isNull();
            assertThat(PopularBookService.yes24Age("3")).isNull();
            assertThat(PopularBookService.yes24Age("")).isNull();
        }

        @Test
        void genderFromEveryClient() {
            assertThat(PopularBookService.yes24Sex("male")).isEqualTo("M");
            assertThat(PopularBookService.yes24Sex("M")).isEqualTo("M");
            assertThat(PopularBookService.yes24Sex("1")).isEqualTo("M");
            assertThat(PopularBookService.yes24Sex("female")).isEqualTo("F");
            assertThat(PopularBookService.yes24Sex("2")).isEqualTo("F");
            assertThat(PopularBookService.yes24Sex("")).isNull();
        }

        @Test
        void longPeriodsUseMonthlyRanking() {
            assertThat(PopularBookService.isMonthly("2026-11-02", "2026-11-09")).isFalse();
            assertThat(PopularBookService.isMonthly("2026-10-10", "2026-11-09")).isTrue();
            assertThat(PopularBookService.isMonthly("", "")).isFalse();
            assertThat(PopularBookService.isMonthly("bad", "")).isFalse();
        }
    }

    @Test
    void yes24ResultIsMappedToPopularBook() {
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        when(yes24.isEnabled()).thenReturn(true);
        BookSearchDto book = BookSearchDto.builder()
                .title("가호").author("저자: 무라카미 하루키, 옮긴이: 홍은주").publisher("문학동네")
                .isbn("9791141691370").coverUrl("https://image.yes24.com/goods/197068949/XL").totalPage(0).build();
        when(yes24.bestsellers("001001046", 20, "F", false, 2, 20)).thenReturn(List.of(
                new Yes24BookClient.Bestseller(21, new Yes24BookClient.Yes24Item(book, "국내도서>소설/시/희곡", ""))));

        List<PopularBookDto> result = new PopularBookService(yes24)
                .getPopularBooks("2026-11-02", "2026-11-09", "8", "", "20", "female", "", "", 2, 20);

        assertThat(result).hasSize(1);
        PopularBookDto first = result.get(0);
        assertThat(first.getRank()).isEqualTo(21);
        assertThat(first.getTitle()).isEqualTo("가호");
        assertThat(first.getIsbn13()).isEqualTo("9791141691370");
        assertThat(first.getKdcName()).isEqualTo("소설/시/희곡");
        assertThat(first.getCoverUrl()).endsWith("/XL");
        assertThat(first.getLoanCount()).isZero();
    }

    @Test
    void yes24FailureFallsBackToLibrary() {
        Yes24BookClient yes24 = mock(Yes24BookClient.class);
        when(yes24.isEnabled()).thenReturn(true);
        when(yes24.bestsellers(any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenThrow(new Yes24BookClient.Yes24Failure("HTTP 429"));

        // 도서관정보나루 키도 없으면 빈 목록 (예외가 밖으로 나가지 않는다)
        List<PopularBookDto> result = new PopularBookService(yes24)
                .getPopularBooks("", "", "", "", "", "", "", "", 1, 20);

        assertThat(result).isEmpty();
        verify(yes24).bestsellers("001", null, null, false, 1, 20);
    }
}
