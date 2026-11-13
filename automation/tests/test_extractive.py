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


def test_abbreviations_and_decimals_do_not_split():
    sentences = split_sentences("Dr. Silva mediu 2.5 horas. Depois saiu! Foi bom?")
    assert sentences == ["Dr. Silva mediu 2.5 horas.", "Depois saiu!", "Foi bom?"]


def test_single_huge_sentence_is_truncated():
    text = " ".join(f"palavra{chr(97 + i % 26)}{i}" for i in range(300)) + "."
    assert len(summarize_extractive(text.replace("0", "x")).split()) <= MAX_SUMMARY_WORDS


def test_only_stopwords_falls_back_to_head():
    text = "de a o e. " * 50
    assert 0 < len(summarize_extractive(text).split()) <= MAX_SUMMARY_WORDS
