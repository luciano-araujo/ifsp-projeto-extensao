import logging

from app.claude_engine import FallbackError, summarize_with_claude
from app.config import Settings
from app.extractive import summarize_extractive

log = logging.getLogger("summary")


def summarize(text: str, settings: Settings, client=None) -> tuple[str, str]:
    """Devolve (resumo, engine). Em auto/claude tenta o Claude e cai no extrativo se falhar."""
    if settings.engine != "extractive":
        try:
            return summarize_with_claude(text, settings, client), "claude"
        except FallbackError as e:
            log.warning("fallback para extrativo: %s", e.reason)
    return summarize_extractive(text), "extractive"
