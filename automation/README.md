# Serviço de resumo de roteiros (RF08)

Serviço Python (FastAPI) que resume o roteiro de um vídeo. O backend Java chama `POST /summarize` e lê somente o campo `summary`.

- Motor generativo: Claude Sonnet 5.5 (`claude-sonnet-5-5`) pelo SDK oficial `anthropic`.
- Motor extrativo (fallback): Python puro, sem bibliotecas de ML.

## Endpoints

| Rota | Entrada | Saída |
|---|---|---|
| `GET /health` | - | `{"status":"ok"}` |
| `POST /summarize` | `{"text": "..."}` | `{"summary": "...", "engine": "claude" \| "extractive"}` |

`text` é obrigatório, com no mínimo 100 e no máximo 20.000 palavras; fora disso a resposta é 422.

## Como rodar (Windows / PowerShell)

```powershell
cd automation
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements-dev.txt   # em produção basta requirements.txt
uvicorn app.main:app --port 8000
pytest                                # testes (nunca chamam a API real)
```

## Variáveis de ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `ANTHROPIC_API_KEY` | (vazia) | Chave da API. Opcional: sem ela o serviço usa o motor extrativo. Nunca é logada nem versionada. |
| `SUMMARY_MODEL` | `claude-sonnet-5-5` | Modelo usado. |
| `SUMMARY_TIMEOUT_SECONDS` | `25` | Timeout da chamada ao Claude. |
| `SUMMARY_ENGINE` | `auto` | `auto` (Claude com fallback), `claude` (igual ao `auto`, mas pretende só Claude; ainda cai no extrativo em caso de falha) ou `extractive` (nunca chama o Claude). |
| `PORT` | `8000` | Porta (usada ao rodar `python -m app.main`; com uvicorn use `--port`). |

### Arquivo `.env`

Crie `automation/.env` (ignorado pelo git; há um modelo em `.env.example`):

```
ANTHROPIC_API_KEY=sk-ant-...
```

Variáveis já definidas no ambiente têm prioridade sobre o `.env`.

## Chave da API e cobrança

- A chave vem do **Claude Console** (console.anthropic.com) e é paga à parte: a assinatura Max **não** cobre uso de API.
- **Não defina `ANTHROPIC_API_KEY` como variável de ambiente da máquina no Windows.** Com ela definida, o Claude Code passa a cobrar créditos de API em vez de usar o plano Max. Use apenas o arquivo `automation/.env` (ou o segredo do contêiner em produção).
- Recomenda-se um limite de gasto mensal de US$ 5 no Console.

## Custo

Cerca de US$ 0,013 por resumo com o Sonnet 5.5 (esforço baixo, saída limitada a 2.000 tokens).

## Como o fallback funciona

O serviço usa o motor extrativo, e responde `engine: "extractive"`, quando:

- não há `ANTHROPIC_API_KEY`;
- a chamada falha (conexão, timeout, limite de requisições, qualquer erro HTTP, inclusive crédito insuficiente);
- o modelo recusa (`stop_reason == "refusal"`);
- a resposta não traz texto.

O motivo é registrado no log (sem segredos), junto do `request_id` e do uso de tokens quando há resposta. O extrativo divide o texto em frases, pontua cada uma pela frequência normalizada das palavras (ignorando stopwords em português), escolhe as melhores até cerca de 120 palavras e as devolve na ordem original. O resultado é determinístico.
