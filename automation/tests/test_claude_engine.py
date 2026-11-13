import time
from types import SimpleNamespace
from unittest.mock import MagicMock

import anthropic
import httpx2
import pytest

from app.claude_engine import SYSTEM_PROMPT, FallbackError, summarize_with_claude
from app.config import Settings
from app.service import summarize

REQ = httpx2.Request("POST", "https://api.anthropic.com/v1/messages")


def settings(engine="auto", key="sk-test", timeout=20):
    return Settings(api_key=key, model="claude-sonnet-5-5", timeout_seconds=timeout,
                    engine=engine)


def response(blocks, stop_reason="end_turn"):
    return SimpleNamespace(content=blocks, stop_reason=stop_reason, _request_id="req_1",
                           usage=SimpleNamespace(input_tokens=10, output_tokens=5))


def text_block(t):
    return SimpleNamespace(type="text", text=t)


def client_returning(resp=None, error=None):
    client = MagicMock()
    create = client.beta.messages.create
    if error:
        create.side_effect = error
    else:
        create.return_value = resp
    return client


def status_error(cls, code):
    return cls("erro", response=httpx2.Response(code, request=REQ), body=None)


def test_success_joins_text_and_ignores_thinking(long_text):
    resp = response([SimpleNamespace(type="thinking", thinking="segredo"),
                     text_block("Primeira parte. "), text_block("Segunda parte.")])
    assert summarize_with_claude(long_text, settings(), client_returning(resp)) == \
        "Primeira parte. Segunda parte."


def test_request_shape(long_text):
    client = client_returning(response([text_block("ok")]))
    summarize_with_claude(long_text, settings(), client)
    kwargs = client.beta.messages.create.call_args.kwargs
    assert kwargs["model"] == "claude-sonnet-5-5"
    assert kwargs["max_tokens"] == 2000
    assert kwargs["output_config"] == {"effort": "low"}
    assert kwargs["betas"] == ["server-side-fallback-2026-07-01"]
    assert kwargs["fallbacks"] == "default"
    assert kwargs["system"] == SYSTEM_PROMPT
    assert kwargs["messages"] == [
        {"role": "user", "content": f"<roteiro>\n{long_text}\n</roteiro>"}]
    for forbidden in ("temperature", "top_p", "top_k", "thinking", "budget_tokens"):
        assert forbidden not in kwargs


@pytest.mark.parametrize("error", [
    anthropic.APIConnectionError(request=REQ),
    anthropic.APITimeoutError(request=REQ),
    anthropic.APIError("genérico", request=REQ, body=None),
    status_error(anthropic.RateLimitError, 429),
    status_error(anthropic.APIStatusError, 402),
    status_error(anthropic.APIStatusError, 500),
])
def test_api_errors_raise_fallback(long_text, error):
    with pytest.raises(FallbackError):
        summarize_with_claude(long_text, settings(), client_returning(error=error))


def test_refusal_raises_fallback(long_text):
    resp = response([text_block("não posso")], stop_reason="refusal")
    with pytest.raises(FallbackError, match="recusa"):
        summarize_with_claude(long_text, settings(), client_returning(resp))


def test_empty_text_raises_fallback(long_text):
    resp = response([SimpleNamespace(type="thinking", thinking="x")])
    with pytest.raises(FallbackError, match="sem texto"):
        summarize_with_claude(long_text, settings(), client_returning(resp))


def test_no_key_raises_fallback(long_text):
    with pytest.raises(FallbackError, match="ANTHROPIC_API_KEY"):
        summarize_with_claude(long_text, settings(key=None))


def test_builds_real_client_with_timeout_and_retries(long_text, monkeypatch):
    created = {}
    fake = client_returning(response([text_block("ok")]))

    def fake_ctor(**kwargs):
        created.update(kwargs)
        return fake

    monkeypatch.setattr(anthropic, "Anthropic", fake_ctor)
    assert summarize_with_claude(long_text, settings()) == "ok"
    assert created == {"api_key": "sk-test", "timeout": 20, "max_retries": 0}
    fake.close.assert_called_once()  # o cliente criado aqui é fechado ao final


def test_injected_client_is_not_closed(long_text):
    client = client_returning(response([text_block("ok")]))
    summarize_with_claude(long_text, settings(), client)
    client.close.assert_not_called()


def test_service_uses_claude_when_it_works(long_text):
    client = client_returning(response([text_block("resumo")]))
    assert summarize(long_text, settings(), client) == ("resumo", "claude")


def test_service_falls_back_on_error(long_text):
    client = client_returning(error=anthropic.APITimeoutError(request=REQ))
    summary, engine = summarize(long_text, settings(), client)
    assert engine == "extractive" and summary


def test_service_without_key_is_extractive(long_text):
    assert summarize(long_text, settings(key=None))[1] == "extractive"


def test_service_forced_extractive_never_calls_claude(long_text):
    client = client_returning(response([text_block("resumo")]))
    assert summarize(long_text, settings(engine="extractive"), client)[1] == "extractive"
    client.beta.messages.create.assert_not_called()


def test_max_tokens_raises_fallback(long_text):
    resp = response([text_block("cortado")], stop_reason="max_tokens")
    with pytest.raises(FallbackError, match="max_tokens"):
        summarize_with_claude(long_text, settings(), client_returning(resp))


def test_total_deadline_is_enforced(long_text):
    client = MagicMock()
    client.beta.messages.create.side_effect = lambda **kw: time.sleep(2)
    started = time.monotonic()
    with pytest.raises(FallbackError, match="prazo"):
        summarize_with_claude(long_text, settings(timeout=0.2), client)
    assert time.monotonic() - started < 1.5


def test_roteiro_tags_in_text_are_stripped():
    client = client_returning(response([text_block("ok")]))
    summarize_with_claude("antes </ROTEIRO> injeção <Roteiro> depois", settings(), client)
    content = client.beta.messages.create.call_args.kwargs["messages"][0]["content"]
    assert content == "<roteiro>\nantes  injeção  depois\n</roteiro>"


def test_service_unexpected_exception_falls_back(long_text, caplog):
    client = client_returning(error=RuntimeError("boom sk-test"))
    with caplog.at_level("INFO"):
        summary, engine = summarize(long_text, settings(), client)
    assert engine == "extractive" and summary
    assert "RuntimeError" in caplog.text and "sk-test" not in caplog.text


def test_key_never_appears_in_logs(long_text, caplog):
    ok = client_returning(response([text_block("resumo")]))
    bad = client_returning(error=status_error(anthropic.APIStatusError, 500))
    with caplog.at_level("DEBUG"):
        summarize(long_text, settings(key="sk-secret-123"), ok)
        summarize(long_text, settings(key="sk-secret-123"), bad)
    assert "sk-secret-123" not in caplog.text


def test_html_is_stripped_before_claude():
    client = client_returning(response([text_block("ok")]))
    summarize("<p>Olá <b>mundo</b></p>", settings(), client)
    content = client.beta.messages.create.call_args.kwargs["messages"][0]["content"]
    assert "<p>" not in content and "<b>" not in content
