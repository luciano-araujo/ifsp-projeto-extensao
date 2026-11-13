"""Configuração por variáveis de ambiente (lidas a cada chamada, facilita os testes)."""
import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

MIN_WORDS = 100
MAX_WORDS = 20_000

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


def load_env_file() -> None:
    """Carrega automation/.env se existir, sem sobrescrever variáveis já definidas."""
    load_dotenv(_ENV_FILE, override=False)


@dataclass(frozen=True)
class Settings:
    api_key: str | None
    model: str
    timeout_seconds: float
    engine: str


def _positive_float(name: str, default: float) -> float:
    try:
        value = float(os.environ.get(name, default))
    except ValueError:
        return default
    return value if value > 0 else default


def get_settings() -> Settings:
    engine = os.environ.get("SUMMARY_ENGINE", "auto").strip().lower()
    if engine not in ("auto", "extractive"):
        engine = "auto"
    return Settings(
        api_key=(os.environ.get("ANTHROPIC_API_KEY") or "").strip() or None,
        model=os.environ.get("SUMMARY_MODEL", "claude-sonnet-5-5"),
        timeout_seconds=_positive_float("SUMMARY_TIMEOUT_SECONDS", 20),
        engine=engine,
    )
