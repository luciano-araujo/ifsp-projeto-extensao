import pytest
from fastapi.testclient import TestClient

from app.config import get_settings
from app.main import app
from tests.conftest import make_text

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200 and r.json() == {"status": "ok"}


def test_summarize_without_key_is_extractive(long_text):
    r = client.post("/summarize", json={"text": long_text})
    assert r.status_code == 200
    body = r.json()
    assert body["engine"] == "extractive" and body["summary"]


def test_text_is_required():
    assert client.post("/summarize", json={}).status_code == 422


def test_short_text_is_422():
    r = client.post("/summarize", json={"text": "muito curto " * 10})
    assert r.status_code == 422


def test_exactly_100_words_is_accepted():
    r = client.post("/summarize", json={"text": "gato "
                                        * 99 + "gato."})
    assert r.status_code == 200


def test_too_long_is_422():
    r = client.post("/summarize", json={"text": "palavra " * 20_001})
    assert r.status_code == 422


def test_20000_words_is_accepted():
    r = client.post("/summarize", json={"text": make_text(repeats=1) + " x" * (20_000 - 100)})
    assert r.status_code == 200


def test_settings_defaults_and_invalid_values(monkeypatch):
    s = get_settings()
    assert (s.model, s.timeout_seconds, s.engine, s.api_key) == \
        ("claude-sonnet-5-5", 25, "auto", None)
    monkeypatch.setenv("SUMMARY_ENGINE", "bogus")
    monkeypatch.setenv("SUMMARY_TIMEOUT_SECONDS", "abc")
    monkeypatch.setenv("ANTHROPIC_API_KEY", "  ")
    s = get_settings()
    assert (s.engine, s.timeout_seconds, s.api_key) == ("auto", 25, None)
    monkeypatch.setenv("SUMMARY_TIMEOUT_SECONDS", "-3")
    monkeypatch.setenv("SUMMARY_ENGINE", "Extractive")
    monkeypatch.setenv("SUMMARY_MODEL", "outro")
    s = get_settings()
    assert (s.engine, s.timeout_seconds, s.model) == ("extractive", 25, "outro")
