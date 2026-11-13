"""Resumo extrativo em Python puro: frequência normalizada de palavras, determinístico."""
import html
import re
from collections import Counter

MAX_SUMMARY_WORDS = 120

STOPWORDS = frozenset("""
a o as os um uma uns umas de do da dos das em no na nos nas por para pra com sem sob sobre entre
e ou mas que se como quando onde porque pois porém contudo todavia então assim também já ainda
mais menos muito muita muitos muitas pouco pouca poucos poucas todo toda todos todas outro outra
outros outras mesmo mesma mesmos mesmas esse essa esses essas este esta estes estas isso isto
aquele aquela aqueles aquelas aquilo ele ela eles elas eu tu você vocês nós me te lhe lhes
meu minha meus minhas teu tua seu sua seus suas nosso nossa nossos nossas dele dela deles delas
ser é são era eram foi foram será sendo sido está estão estava estavam estar estou estamos
ter tem têm tinha tinham teve tenho temos haver há havia houve ir vai vão fui vamos
ao aos à às pelo pela pelos pelas num numa nuns numas até desde após ante contra durante
não sim só tão tanto tanta quanto qual quais quem cujo cuja aí aqui lá ali cá
""".split())

_ABBREVIATIONS = ("sr", "sra", "srta", "dr", "dra", "prof", "profa", "exmo", "exma", "etc",
                  "ex", "obs", "pág", "pag", "av", "cia", "ltda", "vs", "tel", "fig")
_PLACEHOLDER = "\u0001"
_ABBR_RE = re.compile(r"\b(" + "|".join(_ABBREVIATIONS) + r")\.", re.IGNORECASE)
_DECIMAL_RE = re.compile(r"(?<=\d)\.(?=\d)")
_SENTENCE_END_RE = re.compile(
    r"\s*\n\s*|(?:(?<=[.!?…])|(?<=[.!?…][\"'”’)\]]))\s+")
_BLOCK_TAG_RE = re.compile(
    r"<\s*(br|/?\s*(p|div|li|ul|ol|h[1-6]|blockquote|tr|pre))\b[^>]*>", re.IGNORECASE)
_TAG_RE = re.compile(r"<[^>]*>")
_HEADER_RE = re.compile(r"^\s*(título|público-alvo|objetivo)\s*:", re.IGNORECASE)
_KEY_STRIP = " .!?…,;:\"'”“’)(]["
_WORD_RE = re.compile(r"[^\W\d_]+", re.UNICODE)


def strip_html(text: str) -> str:
    """Tags de bloco viram quebra de linha (fim de frase), as demais viram espaço; entidades são decodificadas."""
    text = _BLOCK_TAG_RE.sub("\n", text)
    return html.unescape(_TAG_RE.sub(" ", text))


def _dedup_key(sentence: str) -> str:
    return " ".join(sentence.lower().split()).strip(_KEY_STRIP)


def _unique(sentences: list[str]) -> list[str]:
    seen: set[str] = set()
    out = []
    for s in sentences:
        key = _dedup_key(s)
        if key and key not in seen:
            seen.add(key)
            out.append(s)
    return out


def split_sentences(text: str) -> list[str]:
    protected = _ABBR_RE.sub(lambda m: m.group(1) + _PLACEHOLDER, text)
    protected = _DECIMAL_RE.sub(_PLACEHOLDER, protected)
    parts = _SENTENCE_END_RE.split(protected.strip())
    return [p.replace(_PLACEHOLDER, ".").strip() for p in parts if p.strip()]


def _content_words(sentence: str) -> list[str]:
    words = (m.lower() for m in _WORD_RE.findall(sentence))
    return [w for w in words if w not in STOPWORDS and len(w) > 2]


def _truncate(sentence: str, limit: int) -> str:
    return " ".join(sentence.split()[:limit])


def _split_header(text: str) -> tuple[list[str], str]:
    """Separa as linhas de cabeçalho (Título/Público-alvo/Objetivo) do início do texto do restante."""
    lines = text.split("\n")
    header: list[str] = []
    i = 0
    while i < len(lines) and (not lines[i].strip() or _HEADER_RE.match(lines[i])):
        if lines[i].strip():
            header.append(lines[i])
        i += 1
    return header, "\n".join(lines[i:])


def summarize_extractive(text: str, max_words: int = MAX_SUMMARY_WORDS) -> str:
    header, body = _split_header(text)
    sentences = _unique(split_sentences(body))
    if not sentences:
        return ""
    tokens = [_content_words(s) for s in sentences]
    # As palavras do cabeçalho contam na frequência, mas as linhas nunca viram frases do resumo
    freq = Counter(w for ws in tokens for w in ws)
    freq.update(w for line in header for w in _content_words(line))
    if not freq:
        return _truncate(" ".join(sentences), max_words)
    top = max(freq.values())
    norm = {w: c / top for w, c in freq.items()}

    scored = []
    for i, ws in enumerate(tokens):
        score = sum(norm[w] for w in ws) / (len(ws) ** 0.5) if ws else 0.0
        scored.append((-score, i))
    scored.sort()  # maior pontuação primeiro; empate resolvido pela ordem original

    chosen: list[int] = []
    used = 0
    for _, i in scored:
        n = len(sentences[i].split())
        if used + n <= max_words:
            chosen.append(i)
            used += n
    if not chosen:  # a melhor frase sozinha já passa do limite
        return _truncate(sentences[scored[0][1]], max_words)
    return " ".join(sentences[i] for i in sorted(chosen))
