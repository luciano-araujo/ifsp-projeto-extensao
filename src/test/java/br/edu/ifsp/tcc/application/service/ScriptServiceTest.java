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

    // --- HTML sanitization (BE-13) ---

    @Test
    void sanitizeContent_shouldDropImgWithEventHandler() {
        String clean = scriptService.sanitizeContent("<p>ok</p><img src=x onerror=alert(1)>");
        assertEquals("<p>ok</p>", clean);
    }

    @Test
    void sanitizeContent_shouldDropScriptTagAndItsBody() {
        String clean = scriptService.sanitizeContent("<p>ok</p><script>alert(1)</script>");
        assertEquals("<p>ok</p>", clean);
    }

    @Test
    void sanitizeContent_shouldDropJavascriptUrlButKeepLinkText() {
        String clean = scriptService.sanitizeContent("<a href=\"javascript:alert(1)\">click</a>");
        assertFalse(clean.contains("javascript"));
        assertTrue(clean.contains("click"));
    }

    @Test
    void sanitizeContent_shouldDropEventHandlerAttributesFromAllowedTags() {
        String clean = scriptService.sanitizeContent("<p onclick=\"alert(1)\" style=\"x\">hi</p>");
        assertEquals("<p>hi</p>", clean);
    }

    @Test
    void sanitizeContent_shouldKeepEditorTagsAndSafeLinks() {
        String html = "<h1>T</h1><h2>S</h2><h3>U</h3><p><strong>b</strong> <b>b</b> <em>i</em> <i>i</i> <u>u</u> <s>s</s><br></p>"
                + "<ul><li>a</li></ul><ol><li>b</li></ol><blockquote>q</blockquote><pre><code>c</code></pre><hr>"
                + "<p><a href=\"https://example.com\" target=\"_blank\" rel=\"noopener noreferrer nofollow\">l</a> "
                + "<a href=\"mailto:a@b.com\" rel=\"noopener noreferrer nofollow\">m</a> "
                + "<a href=\"http://example.com\" rel=\"noopener noreferrer nofollow\">h</a></p>";
        String clean = scriptService.sanitizeContent(html);
        assertEquals(html, clean);
    }

    @Test
    void sanitizeContent_shouldEnforceSafeRelOnLinks() {
        String clean = scriptService.sanitizeContent("<a href=\"https://x.com\" rel=\"opener\">l</a>");
        assertTrue(clean.contains("rel=\"noopener noreferrer nofollow\""), clean);
        assertFalse(clean.contains("rel=\"opener\""), clean);
    }

    @Test
    void sanitizeContent_shouldKeepH4ToH6AndOrderedListStart() {
        String html = "<h4>a</h4><h5>b</h5><h6>c</h6><ol start=\"3\"><li>x</li></ol>";
        assertEquals(html, scriptService.sanitizeContent(html));
    }

    @Test
    void sanitizeContent_shouldKeepOnlyLanguageClassesOnCode() {
        assertEquals("<pre><code class=\"language-java\">x</code></pre>",
                scriptService.sanitizeContent("<pre><code class=\"language-java\">x</code></pre>"));
        assertEquals("<pre><code class=\"language-java\">x</code></pre>",
                scriptService.sanitizeContent("<pre><code class=\"evil language-java other\">x</code></pre>"));
        assertEquals("<code>x</code>", scriptService.sanitizeContent("<code class=\"evil\">x</code>"));
        assertEquals("<p>x</p>", scriptService.sanitizeContent("<p class=\"language-java\">x</p>"));
    }

    @Test
    void sanitizeForRead_shouldCleanScriptAndVersions() {
        Script script = new Script();
        script.setContent("<p>ok</p><script>x</script>");
        assertSame(script, scriptService.sanitizeForRead(script));
        assertEquals("<p>ok</p>", script.getContent());

        ScriptVersion version = new ScriptVersion();
        version.setContent("<img src=x onerror=alert(1)><p>v</p>");
        List<ScriptVersion> versions = List.of(version);
        assertSame(versions, scriptService.sanitizeForRead(versions));
        assertEquals("<p>v</p>", version.getContent());
    }

    @Test
    void sanitizeContent_shouldKeepAccentsAndWhitespace() {
        assertEquals("<p>Olá, mundo  ção</p>", scriptService.sanitizeContent("<p>Olá, mundo  ção</p>"));
    }

    @Test
    void sanitizeContent_shouldReturnEmptyForNullOrEmpty() {
        assertEquals("", scriptService.sanitizeContent(null));
        assertEquals("", scriptService.sanitizeContent(""));
    }
}
