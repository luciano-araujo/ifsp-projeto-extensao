import logging

from app.claude_engine import FallbackError, summarize_with_claude
from app.config import Settings
from app.extractive import strip_html, summarize_extractive

log = logging.getLogger("summary")


def summarize(text: str, settings: Settings, client=None) -> tuple[str, str]:
    """Devolve (resumo, engine). Em auto tenta o Claude e cai no extrativo se falhar."""
    text = strip_html(text)
    if settings.engine != "extractive":
        try:
            return summarize_with_claude(text, settings, client), "claude"
        except FallbackError as e:
            log.warning("fallback para extrativo: %s", e.reason)
        except Exception as e:  # nunca devolver 500 por falha do motor generativo
            log.warning("fallback para extrativo: erro inesperado %s", type(e).__name__)
    return summarize_extractive(text), "extractive"
