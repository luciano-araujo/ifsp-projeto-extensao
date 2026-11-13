import pytest

SENTENCES = [
    "O canal de culinária vai ensinar a fazer pão caseiro em casa.",
    "Primeiro, o apresentador explica que o pão caseiro precisa de farinha, água e fermento.",
    "Dr. Silva afirma que a fermentação do pão leva cerca de 2.5 horas.",
    "Depois, o vídeo mostra como sovar a massa do pão com calma.",
    "A câmera passeia pela cozinha enquanto a música toca ao fundo.",
    "O pão caseiro assa no forno por quarenta minutos até dourar.",
    "Por fim, o apresentador prova o pão e convida o público a comentar.",
]


def make_text(repeats: int = 3) -> str:
    return " ".join(SENTENCES * repeats)


@pytest.fixture
def long_text() -> str:
    text = make_text()
    assert len(text.split()) >= 100
    return text


@pytest.fixture(autouse=True)
def no_real_key(monkeypatch):
    """Garante que nenhum teste enxergue uma chave real da máquina."""
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
    monkeypatch.delenv("SUMMARY_ENGINE", raising=False)
