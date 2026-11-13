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
