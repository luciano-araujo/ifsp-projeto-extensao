package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardSummaryResponse;
import br.edu.ifsp.tcc.application.entity.CardSummary;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.repository.CardSummaryRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Data access, script text extraction and mapping of card summaries. Business rules live in the use cases. */
@Service
public class CardSummaryService {

    public static final String BASED_ON_SCRIPT = "SCRIPT";

    private final CardSummaryRepository cardSummaryRepository;

    public CardSummaryService(CardSummaryRepository cardSummaryRepository) {
        this.cardSummaryRepository = cardSummaryRepository;
    }

    public Optional<CardSummary> findByCard(Long cardId) {
        return cardSummaryRepository.findByKanbanItem_Id(cardId);
    }

    /** Creates or overwrites the summary of the card, in its own short transaction. */
    @Transactional
    public CardSummary overwrite(KanbanItem item, String summary) {
        CardSummary row = cardSummaryRepository.findByKanbanItem_Id(item.getId()).orElseGet(() -> {
            CardSummary created = new CardSummary();
            created.setKanbanItem(item);
            return created;
        });
        row.setSummary(summary);
        row.setGeneratedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return cardSummaryRepository.save(row);
    }

    private static final String BLOCKS = "p, h1, h2, h3, h4, h5, h6, li, blockquote, pre";

    /**
     * Plain text of the editor HTML: one line per block (p, h1-h6, li, blockquote, pre), joined by newline;
     * tags dropped and entities decoded. The word count and the text sent to the generator come from this same text.
     */
    public static String plainText(String html) {
        if (html == null || html.isBlank()) return "";
        Element body = Jsoup.parse(html).body();
        List<Element> containers = new ArrayList<>();
        containers.add(body);
        containers.addAll(body.select(BLOCKS));
        List<String> lines = new ArrayList<>();
        for (Element container : containers) {
            StringBuilder own = new StringBuilder();
            for (Node child : container.childNodes()) {
                if (child instanceof TextNode text) {
                    own.append(text.getWholeText());
                } else if (child instanceof Element element && !element.is(BLOCKS) && element.select(BLOCKS).isEmpty()) {
                    own.append(element.normalName().equals("br") ? " " : element.text());
                }
            }
            String line = own.toString().replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
            if (!line.isEmpty()) lines.add(line);
        }
        return String.join("\n", lines);
    }

    /**
     * Text sent to the generator: header lines (title, audience, objective; only the non-blank ones), a blank line,
     * then the plain text of the script.
     */
    public static String buildPayload(KanbanItem item, String scriptText) {
        StringBuilder out = new StringBuilder();
        appendHeader(out, "Título", item.getTitle());
        appendHeader(out, "Público-alvo", item.getTargetAudience());
        appendHeader(out, "Objetivo", item.getPedagogicalObjective());
        if (out.length() > 0) out.append('\n');
        return out.append(scriptText).toString();
    }

    private static void appendHeader(StringBuilder out, String label, String value) {
        if (value == null || value.isBlank()) return;
        out.append(label).append(": ").append(value.replaceAll("\\s+", " ").trim()).append('\n');
    }

    public static int countWords(String plainText) {
        return plainText == null || plainText.isBlank() ? 0 : plainText.trim().split("\\s+").length;
    }

    public CardSummaryResponse toResponse(CardSummary summary) {
        return new CardSummaryResponse(summary.getKanbanItemId(), summary.getSummary(), summary.getGeneratedAt(),
                BASED_ON_SCRIPT);
    }
}
