from app.extractive import MAX_SUMMARY_WORDS, split_sentences, summarize_extractive
from tests.conftest import SENTENCES, make_text


def test_deterministic(long_text):
    assert summarize_extractive(long_text) == summarize_extractive(long_text)


def test_respects_word_cap():
    text = make_text(repeats=30)
    assert len(summarize_extractive(text).split()) <= MAX_SUMMARY_WORDS


def test_keeps_original_order(long_text):
    summary = summarize_extractive(long_text)
    picked = [s for s in SENTENCES if s in summary]
    assert picked
    positions = [summary.index(s) for s in picked]
    assert positions == sorted(positions)
    assert picked == [s for s in SENTENCES if s in picked]


def test_repetitive_input_has_no_duplicate_sentences():
    summary = summarize_extractive(make_text(repeats=10))
    for s in SENTENCES:
        assert summary.count(s) <= 1


def test_dedup_ignores_case_spacing_and_trailing_punctuation():
    text = "Gato bonito  mora aqui! gato bonito mora aqui. Cachorro feio mora lá."
    summary = summarize_extractive(text)
    assert summary.lower().count("gato bonito") == 1


def test_splits_on_line_breaks():
    assert split_sentences("Cena 1\nO apresentador entra…\n\nFim") == [
        "Cena 1", "O apresentador entra…", "Fim"]


def test_closing_quotes_and_parentheses_after_punctuation():
    assert split_sentences('Ele disse "fim." Depois saiu (rápido.) Voltou') == [
        'Ele disse "fim."', "Depois saiu (rápido.)", "Voltou"]


def test_empty_after_dedup_returns_empty():
    assert summarize_extractive("...") == ""


def test_abbreviations_and_decimals_do_not_split():
    sentences = split_sentences("Dr. Silva mediu 2.5 horas. Depois saiu! Foi bom?")
    assert sentences == ["Dr. Silva mediu 2.5 horas.", "Depois saiu!", "Foi bom?"]


def test_single_huge_sentence_is_truncated():
    text = " ".join(f"palavra{chr(97 + i % 26)}{i}" for i in range(300)) + "."
    assert len(summarize_extractive(text.replace("0", "x")).split()) <= MAX_SUMMARY_WORDS


def test_only_stopwords_falls_back_to_head():
    text = "de a o e. " * 50
    assert 0 < len(summarize_extractive(text).split()) <= MAX_SUMMARY_WORDS


def test_strip_html_breaks_blocks_and_decodes_entities():
    from app.extractive import strip_html
    out = strip_html("<p>Um &amp; dois</p><p>Três&nbsp;quatro</p><b>x</b><param>")
    assert split_sentences(out) == ["Um & dois", "Três\xa0quatro", "x"]
    assert "<" not in out and "&amp;" not in out



HEADER = "Título: Oficina de robótica\nPúblico-alvo: Alunos do ensino médio\nObjetivo: Ensinar sensores\n\n"


def test_header_lines_are_never_summary_sentences():
    body = ("Hoje vamos montar um carrinho com sensores. Os sensores medem a distância. "
            "O carrinho desvia dos obstáculos.")
    summary = summarize_extractive(HEADER + body)
    for label in ("Título:", "Público-alvo:", "Objetivo:"):
        assert label not in summary
    assert summary.startswith("Hoje vamos")


def test_header_is_case_insensitive_and_ignores_leading_blank_lines():
    summary = summarize_extractive("\nTÍTULO: X\npúblico-ALVO: Y\nobjetivo: Z\nUma frase real sobre o tema.")
    assert summary == "Uma frase real sobre o tema."


def test_header_words_still_count_towards_frequency():
    script = "Gatos dormem muito. Cachorros correm bastante."
    # sem cabeçalho vence a segunda frase; o título repetido faz a primeira vencer
    assert summarize_extractive(script, max_words=3) == "Cachorros correm bastante."
    assert summarize_extractive("Título: gatos gatos gatos\n\n" + script, max_words=3) == "Gatos dormem muito."


def test_header_label_inside_the_script_is_kept():
    text = HEADER + "Primeira frase do roteiro.\nObjetivo: aparece no meio do roteiro."
    assert "Objetivo: aparece no meio do roteiro." in summarize_extractive(text)


def test_header_only_text_gives_empty_summary():
    assert summarize_extractive("Título: Só cabeçalho\nObjetivo: nada mais") == ""


def test_long_script_without_punctuation_is_capped_at_120_words():
    text = HEADER + " ".join(f"palavra{i}" for i in range(500))
    assert len(summarize_extractive(text).split()) == MAX_SUMMARY_WORDS
