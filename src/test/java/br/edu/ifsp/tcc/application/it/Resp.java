package br.edu.ifsp.tcc.application.it;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** HTTP response of the API under test: status, raw body and a lazily parsed JSON view. */
public record Resp(int status, String body, Map<String, List<String>> headers) {

    public Resp(int status, String body) {
        this(status, body, Map.of());
    }

    public String header(String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name))
                .findFirst().map(e -> String.join(",", e.getValue())).orElse(null);
    }


    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public JsonNode json() {
        if (body == null || body.isBlank()) return MAPPER.missingNode();
        return MAPPER.readTree(body);
    }

    /** Node at a dot separated path (array indexes allowed as numbers). */
    public JsonNode at(String path) {
        JsonNode node = json();
        for (String part : path.split("\\.")) {
            node = part.matches("\\d+") ? node.path(Integer.parseInt(part)) : node.path(part);
        }
        return node;
    }

    /** Value at the path as text; null when absent or JSON null. */
    public String text(String path) {
        JsonNode node = at(path);
        return node.isMissingNode() || node.isNull() ? null : node.asString();
    }

    public long num(String path) {
        return at(path).asLong();
    }

    public int size() {
        return json().size();
    }

    public String message() {
        return text("message");
    }

    public static JsonMapper mapper() {
        return MAPPER;
    }
}
