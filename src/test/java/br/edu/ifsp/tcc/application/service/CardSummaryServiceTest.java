package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.CardSummary;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.repository.CardSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardSummaryServiceTest {

    @Mock private CardSummaryRepository repository;
    @InjectMocks private CardSummaryService service;

    private KanbanItem card() {
        KanbanItem item = new KanbanItem();
        item.setId(3L);
        return item;
    }

    @Test
    void overwrite_shouldCreateRowWhenNone() {
        KanbanItem item = card();
        when(repository.findByKanbanItem_Id(3L)).thenReturn(Optional.empty());
        when(repository.save(any(CardSummary.class))).thenAnswer(inv -> inv.getArgument(0));

        CardSummary saved = service.overwrite(item, "Novo");

        assertSame(item, saved.getKanbanItem());
        assertEquals("Novo", saved.getSummary());
        assertEquals(0, saved.getGeneratedAt().getNano());
    }

    @Test
    void overwrite_shouldReplaceExistingRow() {
        CardSummary existing = new CardSummary();
        existing.setId(9L);
        existing.setSummary("Antigo");
        existing.setGeneratedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        when(repository.findByKanbanItem_Id(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        CardSummary saved = service.overwrite(card(), "Novo");

        assertEquals(9L, saved.getId());
        assertEquals("Novo", saved.getSummary());
        assertTrue(saved.getGeneratedAt().getYear() >= 2026);
    }

    @Test
    void findByCard_shouldDelegate() {
        when(repository.findByKanbanItem_Id(3L)).thenReturn(Optional.empty());
        assertTrue(service.findByCard(3L).isEmpty());
    }

    @Test
    void plainText_shouldDropTagsAndDecodeEntities() {
        assertEquals("Título\nPrimeira linha & mais", CardSummaryService.plainText(
                "<h1>Título</h1><p>Primeira&nbsp;linha &amp; <strong>mais</strong></p>"));
        assertEquals("", CardSummaryService.plainText(null));
        assertEquals("", CardSummaryService.plainText("  "));
    }

    @Test
    void plainText_shouldKeepOneLinePerBlockIncludingNestedListsAndLooseText() {
        String html = "solto<ul><li>a<ul><li>b</li></ul></li></ul><blockquote>c<br>d</blockquote><p>  </p>";
        assertEquals("solto\na\nb\nc d", CardSummaryService.plainText(html));
    }

    @Test
    void countWords_shouldCountWhitespaceSeparatedWords() {
        assertEquals(3, CardSummaryService.countWords("um  dois\ntrês"));
        assertEquals(0, CardSummaryService.countWords(""));
        assertEquals(0, CardSummaryService.countWords(null));
    }

    @Test
    void toResponse_shouldBeBasedOnScript() {
        CardSummary row = new CardSummary();
        row.setKanbanItem(card());
        row.setSummary("Resumo");
        row.setGeneratedAt(LocalDateTime.of(2026, 11, 12, 10, 0));

        CardSummaryResponse response = service.toResponse(row);

        assertEquals(3L, response.getCardId());
        assertEquals("Resumo", response.getSummary());
        assertEquals("SCRIPT", response.getBasedOn());
    }
}
