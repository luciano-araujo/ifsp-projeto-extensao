package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.ScriptExportResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.repository.ScriptRepository;
import br.edu.ifsp.tcc.application.repository.ScriptVersionRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ScriptService {

    /** Average speaking pace used for the estimated duration (words per minute). */
    static final int WORDS_PER_MINUTE = 140;

    private static final Pattern TAG = Pattern.compile("<(/?)([a-zA-Z][a-zA-Z0-9]*)(?:\\s[^>]*)?/?>");
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]*>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?)([0-9a-fA-F]+);");

    /** Tags the TipTap editor produces; everything else (scripts, event handlers, javascript: URLs) is dropped. */
    private static final Safelist SAFELIST = Safelist.none()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "strong", "b", "em", "i", "u", "s", "ul", "ol", "li",
                    "blockquote", "code", "pre", "hr", "a")
            .addAttributes("a", "href", "target")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer nofollow")
            .addAttributes("ol", "start")
            .addAttributes("code", "class")
            .addProtocols("a", "href", "http", "https", "mailto");

    /** The only class values kept on code blocks (syntax highlighting language). */
    private static final Pattern CODE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#-]{1,32}");

    private final ScriptRepository scriptRepository;
    private final ScriptVersionRepository scriptVersionRepository;

    public ScriptService(ScriptRepository scriptRepository, ScriptVersionRepository scriptVersionRepository) {
        this.scriptRepository = scriptRepository;
        this.scriptVersionRepository = scriptVersionRepository;
    }

    /** Cleans editor HTML against the safelist (applied on save and on restore). Null becomes an empty string. */
    public String sanitizeContent(String html) {
        if (html == null || html.isEmpty()) return "";
        Document.OutputSettings output = new Document.OutputSettings().prettyPrint(false);
        String clean = Jsoup.clean(html, "", SAFELIST, output);
        if (!clean.contains("class=")) return clean;

        // class is allowed on code only, and only as language-*: drop any other class token
        Document fragment = Jsoup.parseBodyFragment(clean);
        fragment.outputSettings(output);
        for (Element code : fragment.select("code[class]")) {
            String kept = Arrays.stream(code.className().split("\\s+")).filter(c -> CODE_CLASS.matcher(c).matches())
                    .collect(Collectors.joining(" "));
            if (kept.isEmpty()) code.removeAttr("class"); else code.attr("class", kept);
        }
        return fragment.body().html();
    }

    /**
     * Cleans the content of a script about to be returned, so rows stored before sanitization existed are safe.
     * Call it inside a read-only transaction: the entity must not be flushed.
     */
    public Script sanitizeForRead(Script script) {
        script.setContent(sanitizeContent(script.getContent()));
        return script;
    }

    public List<ScriptVersion> sanitizeForRead(List<ScriptVersion> versions) {
        versions.forEach(version -> version.setContent(sanitizeContent(version.getContent())));
        return versions;
    }

    public Script save(Script script) {
        return scriptRepository.save(script);
    }

    public Optional<Script> findByKanbanItemId(Long kanbanItemId) {
        return scriptRepository.findByKanbanItem_Id(kanbanItemId);
    }

    public ScriptVersion saveVersion(ScriptVersion version) {
        return scriptVersionRepository.save(version);
    }

    public List<ScriptVersion> findVersionsByScriptId(Long scriptId) {
        return scriptVersionRepository.findByScript_IdOrderByCreatedAtDesc(scriptId);
    }

    public Optional<ScriptVersion> findVersionById(Long versionId) {
        return scriptVersionRepository.findById(versionId);
    }

    // --- Empty script and export ---

    /** Script returned by GET when the card has none yet: nothing is persisted. */
    public Script emptyScript(KanbanItem item) {
        Script script = new Script();
        script.setKanbanItem(item);
        script.setContent("");
        return script;
    }

    public ScriptExportResponse toExportResponse(KanbanItem item, String content) {
        String raw = content == null ? "" : content;
        int words = countWords(raw);
        return new ScriptExportResponse(item.getId(), item.getTitle(), item.getTargetAudience(),
                item.getPedagogicalObjective(), raw, toMarkdown(raw), words, estimateMinutes(words));
    }

    public static int estimateMinutes(int wordCount) {
        return Math.max(1, (int) Math.ceil(wordCount / (double) WORDS_PER_MINUTE));
    }

    /** Words of the visible text: tags are dropped and entities decoded. */
    public static int countWords(String html) {
        if (html == null) return 0;
        String text = decodeEntities(ANY_TAG.matcher(html).replaceAll(" ")).replace('\u00A0', ' ').trim();
        return text.isEmpty() ? 0 : text.split("\\s+").length;
    }

    /**
     * Converts the editor HTML to Markdown: h1-h3, p, strong/em, ul/ol/li, blockquote and br.
     * Any other tag is stripped. Content without tags is returned as is.
     */
    public static String toMarkdown(String html) {
        if (html == null || html.isBlank()) return "";
        if (html.indexOf('<') < 0) return decodeEntities(html).trim();

        StringBuilder out = new StringBuilder();
        Deque<int[]> lists = new ArrayDeque<>();   // {1 = ordered, item counter}
        Deque<Integer> quotes = new ArrayDeque<>(); // output position where each blockquote starts
        Matcher matcher = TAG.matcher(html);
        int last = 0;
        while (matcher.find()) {
            appendText(out, html.substring(last, matcher.start()));
            last = matcher.end();
            handleTag(out, lists, quotes, !matcher.group(1).isEmpty(), matcher.group(2).toLowerCase());
        }
        appendText(out, html.substring(last));

        return out.toString().replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    private static void handleTag(StringBuilder out, Deque<int[]> lists, Deque<Integer> quotes,
                                  boolean closing, String name) {
        switch (name) {
            case "h1", "h2", "h3" -> {
                if (closing) {
                    out.append("\n\n");
                } else {
                    ensureBlankLine(out);
                    out.append("#".repeat(name.charAt(1) - '0')).append(' ');
                }
            }
            case "p" -> {
                if (closing) {
                    out.append(lists.isEmpty() ? "\n\n" : "\n");
                } else if (lists.isEmpty()) {
                    ensureBlankLine(out);
                }
            }
            case "strong", "b" -> out.append("**");
            case "em", "i" -> out.append('*');
            case "br" -> out.append('\n');
            case "ul", "ol" -> {
                if (closing) {
                    lists.poll();
                    if (lists.isEmpty()) out.append("\n\n");
                } else {
                    if (lists.isEmpty()) {
                        ensureBlankLine(out);
                    } else {
                        ensureLineStart(out);
                    }
                    lists.push(new int[]{name.equals("ol") ? 1 : 0, 0});
                }
            }
            case "li" -> {
                if (closing) {
                    ensureLineStart(out);
                } else if (!lists.isEmpty()) {
                    ensureLineStart(out);
                    int[] list = lists.peek();
                    out.append("  ".repeat(lists.size() - 1));
                    out.append(list[0] == 1 ? (++list[1]) + ". " : "- ");
                }
            }
            case "blockquote" -> {
                if (closing) {
                    if (!quotes.isEmpty()) {
                        int start = quotes.pop();
                        String inner = out.substring(start).trim();
                        out.setLength(start);
                        out.append(inner.replaceAll("(?m)^(?=.)", "> ").replaceAll("(?m)^$", ">")).append("\n\n");
                    }
                } else {
                    ensureBlankLine(out);
                    quotes.push(out.length());
                }
            }
            default -> { }
        }
    }

    private static void appendText(StringBuilder out, String raw) {
        String text = decodeEntities(raw.replaceAll("\\s+", " "));
        if (text.isEmpty()) return;
        char previous = out.length() == 0 ? '\n' : out.charAt(out.length() - 1);
        if (previous == '\n' || previous == ' ') text = text.stripLeading();
        out.append(text);
    }

    private static void ensureLineStart(StringBuilder out) {
        if (out.length() > 0 && out.charAt(out.length() - 1) != '\n') out.append('\n');
    }

    private static void ensureBlankLine(StringBuilder out) {
        if (out.length() == 0) return;
        int newlines = 0;
        while (newlines < 2 && out.length() - 1 - newlines >= 0 && out.charAt(out.length() - 1 - newlines) == '\n') {
            newlines++;
        }
        out.append("\n".repeat(2 - newlines));
    }

    private static String decodeEntities(String text) {
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            int radix = matcher.group(1).isEmpty() ? 10 : 16;
            String replacement;
            try {
                replacement = new String(Character.toChars(Integer.parseInt(matcher.group(2), radix)));
            } catch (IllegalArgumentException e) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(decoded);
        return decoded.toString()
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
    }
}
