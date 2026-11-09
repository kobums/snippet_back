package com.snippet.service;

import com.snippet.dto.BookSearchDto;
import com.snippet.entity.Book;
import com.snippet.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookInfoRefreshServiceTest {

    private final BookRepository repository = mock(BookRepository.class);
    private final Yes24BookClient yes24 = mock(Yes24BookClient.class);
    private final BookInfoRefreshService service = new BookInfoRefreshService(repository, yes24);

    private static final Yes24BookClient.Yes24Item DEMIAN = new Yes24BookClient.Yes24Item(
            BookSearchDto.builder().isbn("9788937460449").title("데미안").author("저자: 헤르만 헤세")
                    .publisher("민음사").pubDate("2000-12-20").totalPage(239)
                    .coverUrl("https://image.yes24.com/goods/176787/XL").build(),
            "국내도서>소설/시/희곡", "https://www.yes24.com/product/goods/176787");

    @Test
    @DisplayName("알라딘 표지·빈 분류·빈 쪽수만 채우고 사용자가 정한 쪽수는 두고, 임시 ISBN 책은 건너뛴다")
    void refreshesOnlyReplaceableFields() {
        Book aladin = book("9788937460449", "https://image.aladin.co.kr/product/1/cover/a.jpg", 300, null);
        Book crawled = book("AL_12345", "https://image.aladin.co.kr/product/2/cover/b.jpg", null, null);
        when(yes24.isEnabled()).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of(aladin, crawled));
        when(yes24.lookupItem("9788937460449")).thenReturn(Optional.of(DEMIAN));

        BookInfoRefreshService.Result result = service.refreshFromYes24(false);

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.covers()).isEqualTo(1);
        assertThat(result.categories()).isEqualTo(1);
        assertThat(result.pages()).isZero();
        assertThat(aladin.getCoverUrl()).isEqualTo("https://image.yes24.com/goods/176787/XL");
        assertThat(aladin.getCategory()).isEqualTo("국내도서>소설/시/희곡");
        assertThat(aladin.getTotalPage()).isEqualTo(300);
        assertThat(aladin.getAffiliateUrl()).isEqualTo("https://www.yes24.com/product/goods/176787");
        assertThat(crawled.getCoverUrl()).contains("aladin");
    }

    @Test
    @DisplayName("dryRun이면 건수만 세고 저장하지 않는다")
    void dryRunDoesNotSave() {
        Book aladin = book("9788937460449", "https://image.aladin.co.kr/product/1/cover/a.jpg", 0, null);
        when(yes24.isEnabled()).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of(aladin));
        when(yes24.lookupItem("9788937460449")).thenReturn(Optional.of(DEMIAN));

        BookInfoRefreshService.Result result = service.refreshFromYes24(true);

        assertThat(result.covers()).isEqualTo(1);
        assertThat(result.pages()).isEqualTo(1);
        assertThat(aladin.getCoverUrl()).contains("aladin");
        verify(repository, never()).save(any());
    }

    private static Book book(String isbn, String cover, Integer pages, String category) {
        return Book.builder().isbn(isbn).title("데미안").author("헤르만 헤세").coverUrl(cover)
                .affiliateUrl("").totalPage(pages).category(category).build();
    }
}
