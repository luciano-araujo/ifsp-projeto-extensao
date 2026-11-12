package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.repository.ScriptRepository;
import br.edu.ifsp.tcc.application.repository.ScriptVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptServiceTest {

    @Mock private ScriptRepository scriptRepository;
    @Mock private ScriptVersionRepository scriptVersionRepository;
    @InjectMocks private ScriptService scriptService;

    @Test
    void save_shouldDelegateToRepository() {
        Script script = new Script();
        when(scriptRepository.save(script)).thenReturn(script);
        assertEquals(script, scriptService.save(script));
    }

    @Test
    void findByKanbanItemId_shouldReturnScript() {
        Script script = new Script();
        when(scriptRepository.findByKanbanItem_Id(1L)).thenReturn(Optional.of(script));
        assertTrue(scriptService.findByKanbanItemId(1L).isPresent());
    }

    @Test
    void findByKanbanItemId_shouldReturnEmptyWhenNotFound() {
        when(scriptRepository.findByKanbanItem_Id(99L)).thenReturn(Optional.empty());
        assertTrue(scriptService.findByKanbanItemId(99L).isEmpty());
    }

    @Test
    void saveVersion_shouldDelegateToRepository() {
        ScriptVersion version = new ScriptVersion();
        when(scriptVersionRepository.save(version)).thenReturn(version);
        assertEquals(version, scriptService.saveVersion(version));
    }

    @Test
    void findVersionsByScriptId_shouldReturnOrderedVersions() {
        List<ScriptVersion> versions = List.of(new ScriptVersion(), new ScriptVersion());
        when(scriptVersionRepository.findByScript_IdOrderByCreatedAtDesc(1L)).thenReturn(versions);
        assertEquals(2, scriptService.findVersionsByScriptId(1L).size());
    }

    @Test
    void findVersionById_shouldReturnVersion() {
        ScriptVersion version = new ScriptVersion();
        when(scriptVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        assertTrue(scriptService.findVersionById(1L).isPresent());
    }

    // --- Empty script and export ---

    @Test
    void emptyScript_shouldHaveEmptyContentAndNoId() {
        KanbanItem item = new KanbanItem();
        item.setId(7L);

        Script empty = scriptService.emptyScript(item);

        assertNull(empty.getId());
        assertEquals("", empty.getContent());
        assertEquals(7L, empty.getKanbanItemId());
        assertNull(empty.getCreatedAt());
        assertNull(empty.getUpdatedAt());
    }

    @Test
    void toExportResponse_shouldFillTheCardDataWordCountAndMinutes() {
        KanbanItem item = new KanbanItem();
        item.setId(7L);
        item.setTitle("Aula 1");
        item.setTargetAudience("Iniciantes");
        item.setPedagogicalObjective("Entender variaveis");

        ScriptExportResponse response = scriptService.toExportResponse(item, "<h1>Titulo</h1><p>Um dois tres.</p>");

        assertEquals(7L, response.getCardId());
        assertEquals("Aula 1", response.getCardTitle());
        assertEquals("Iniciantes", response.getTargetAudience());
        assertEquals("Entender variaveis", response.getPedagogicalObjective());
        assertEquals("<h1>Titulo</h1><p>Um dois tres.</p>", response.getRawContent());
        assertEquals("# Titulo\n\nUm dois tres.", response.getMarkdownFormatted());
        assertEquals(4, response.getWordCount());
        assertEquals(1, response.getEstimatedMinutes());
    }

    @Test
    void toExportResponse_shouldHandleNullContent() {
        ScriptExportResponse response = scriptService.toExportResponse(new KanbanItem(), null);

        assertEquals("", response.getRawContent());
        assertEquals("", response.getMarkdownFormatted());
        assertEquals(0, response.getWordCount());
        assertEquals(1, response.getEstimatedMinutes());
    }

    @Test
    void estimateMinutes_shouldRoundUpAndNeverBeLessThanOne() {
        assertEquals(1, ScriptService.estimateMinutes(0));
        assertEquals(1, ScriptService.estimateMinutes(140));
        assertEquals(2, ScriptService.estimateMinutes(141));
        assertEquals(3, ScriptService.estimateMinutes(420));
        assertEquals(4, ScriptService.estimateMinutes(421));
    }

    @Test
    void countWords_shouldIgnoreTagsAndDecodeEntities() {
        assertEquals(0, ScriptService.countWords(null));
        assertEquals(0, ScriptService.countWords("<p> </p>"));
        assertEquals(5, ScriptService.countWords("<p>Ola&nbsp;mundo</p><p>tudo <strong>bem</strong> hoje</p>"));
        assertEquals(3, ScriptService.countWords("texto sem tags"));
    }

    @Test
    void toMarkdown_shouldReturnEmptyForBlank() {
        assertEquals("", ScriptService.toMarkdown(null));
        assertEquals("", ScriptService.toMarkdown("   "));
    }

    @Test
    void toMarkdown_shouldKeepPlainTextWithoutTags() {
        assertEquals("Linha 1\nLinha 2 & fim", ScriptService.toMarkdown("Linha 1\nLinha 2 &amp; fim"));
    }

    @Test
    void toMarkdown_shouldConvertHeadingsParagraphsAndInlineMarks() {
        String html = "<h1>Um</h1><h2>Dois</h2><h3>Tres</h3>"
                + "<p>Texto <strong>forte</strong>, <b>negrito</b>, <em>enfase</em> e <i>italico</i>.</p>"
                + "<p>Linha<br>quebrada</p>";

        assertEquals("# Um\n\n## Dois\n\n### Tres\n\n"
                + "Texto **forte**, **negrito**, *enfase* e *italico*.\n\nLinha\nquebrada",
                ScriptService.toMarkdown(html));
    }

    @Test
    void toMarkdown_shouldConvertUnorderedAndOrderedLists() {
        String html = "<p>Antes</p><ul><li>a</li><li>b</li></ul><ol><li><p>um</p></li><li>dois</li></ol><p>Depois</p>";

        assertEquals("Antes\n\n- a\n- b\n\n1. um\n2. dois\n\nDepois", ScriptService.toMarkdown(html));
    }

    @Test
    void toMarkdown_shouldIndentNestedLists() {
        String html = "<ul><li>pai<ul><li>filho</li></ul></li><li>irmao</li></ul>";

        assertEquals("- pai\n  - filho\n- irmao", ScriptService.toMarkdown(html));
    }

    @Test
    void toMarkdown_shouldPrefixBlockquoteLines() {
        String html = "<p>Antes</p><blockquote><p>citacao</p><p>segunda</p></blockquote><p>Depois</p>";

        assertEquals("Antes\n\n> citacao\n>\n> segunda\n\nDepois", ScriptService.toMarkdown(html));
    }

    @Test
    void toMarkdown_shouldNestBlockquotes() {
        assertEquals("> > interna", ScriptService.toMarkdown("<blockquote><blockquote><p>interna</p></blockquote></blockquote>"));
    }

    @Test
    void toMarkdown_shouldStripOtherTagsAndDecodeEntities() {
        String html = "<div class=\"x\"><p>Ver <a href=\"http://a.b\">link</a> &lt;ok&gt; &quot;a&quot; &#39;b&#39; &#x41; &#999999999;</p><hr/><img src=\"a.png\"/></div>";

        assertEquals("Ver link <ok> \"a\" 'b' A &#999999999;", ScriptService.toMarkdown(html));
    }

    @Test
    void toMarkdown_shouldIgnoreStrayClosingTagsAndListItemsOutsideLists() {
        assertEquals("texto\n\nfim",ScriptService.toMarkdown("<li>texto</li></blockquote></ul> fim"));
    }
}
