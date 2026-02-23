package com.snippet.service;

import com.snippet.dto.SnippetArchiveDto;
import com.snippet.dto.SnippetCardDto;
import com.snippet.repository.SnippetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SnippetService {

    private final SnippetRepository snippetRepository;

    public List<SnippetCardDto> getCards(int count, List<Long> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return snippetRepository.findRandomSnippets(count)
                    .stream()
                    .map(SnippetCardDto::from)
                    .toList();
        }

        return snippetRepository.findRandomSnippets(count, excludeIds)
                .stream()
                .map(SnippetCardDto::from)
                .toList();
    }

    public List<SnippetArchiveDto> getArchive(List<Long> ids) {
        return snippetRepository.findByIdIn(ids)
                .stream()
                .map(SnippetArchiveDto::from)
                .toList();
    }
}
