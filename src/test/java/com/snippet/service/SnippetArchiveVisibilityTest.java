package com.snippet.service;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.entity.Book;
import com.snippet.entity.Snippet;
import com.snippet.entity.SnippetArchive;
import com.snippet.entity.User;
import com.snippet.repository.SnippetArchiveRepository;
import com.snippet.repository.SnippetDailyViewRepository;
import com.snippet.repository.SnippetRepository;
import com.snippet.repository.SnippetSeenRepository;
import com.snippet.repository.UserBookRepository;
import com.snippet.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 보관함: 작성자가 비공개로 둔 남의 밑줄은 보이지도, 보관되지도 않아야 한다. */
class SnippetArchiveVisibilityTest {

    private final SnippetRepository snippetRepository = mock(SnippetRepository.class);
    private final SnippetArchiveRepository archiveRepository = mock(SnippetArchiveRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SnippetDailyViewRepository dailyViewRepository = mock(SnippetDailyViewRepository.class);
    private final SnippetService service = new SnippetService(snippetRepository, archiveRepository,
            mock(SnippetSeenRepository.class), mock(UserBookRepository.class), userRepository, dailyViewRepository);

    private final User me = user(1L);
    private final User other = user(2L);
    private final Book book = Book.builder().title("책").author("저자").coverUrl("").affiliateUrl("").build();

    private static User user(long id) {
        User u = User.builder().email(id + "@example.com").password("x").name("n").build();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Snippet snippet(long id, User owner, boolean isPublic) {
        Snippet s = Snippet.builder().book(book).user(owner).type("snippet").text("문장" + id).isPublic(isPublic).build();
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    @DisplayName("보관함: 남의 비공개 밑줄은 숨기고, 공개 밑줄과 내 기록은 보여준다")
    void archiveHidesOthersPrivateSnippets() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(archiveRepository.findByUserOrderByCreateDateDesc(me)).thenReturn(List.of(
                SnippetArchive.builder().user(me).snippet(snippet(10, other, true)).build(),
                SnippetArchive.builder().user(me).snippet(snippet(11, other, false)).build(),
                SnippetArchive.builder().user(me).snippet(snippet(12, me, false)).build()));

        assertThat(service.getArchive(1L)).extracting(SnippetArchiveDto::getId).containsExactly(10L, 12L);
    }

    @Test
    @DisplayName("남의 비공개 밑줄은 id 를 알아도 보관할 수 없다")
    void cannotArchiveOthersPrivateSnippet() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(snippetRepository.findById(11L)).thenReturn(Optional.of(snippet(11, other, false)));

        assertThatThrownBy(() -> service.addArchive(1L, 11L)).isInstanceOf(IllegalArgumentException.class);
        verify(archiveRepository, never()).save(any());
    }

    @Test
    @DisplayName("공개 밑줄은 보관할 수 있다")
    void canArchivePublicSnippet() {
        Snippet pub = snippet(10, other, true);
        SnippetArchive saved = SnippetArchive.builder().user(me).snippet(pub).build();
        ReflectionTestUtils.setField(saved, "id", 99L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(snippetRepository.findById(10L)).thenReturn(Optional.of(pub));
        when(archiveRepository.save(any())).thenReturn(saved);
        when(dailyViewRepository.findByUserAndDate(eq(me), any())).thenReturn(Optional.empty());

        assertThat(service.addArchive(1L, 10L)).isEqualTo(99L);
    }
}
