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
| `SUMMARY_TIMEOUT_SECONDS` | `20` | Prazo total (relógio) da chamada ao Claude, sem novas tentativas. |
| `SUMMARY_ENGINE` | `auto` | `auto` (Claude com fallback para o extrativo) ou `extractive` (nunca chama o Claude). |
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
- O SDK também lê `ANTHROPIC_AUTH_TOKEN`, `ANTHROPIC_PROFILE` e `ANTHROPIC_BASE_URL`: nenhuma delas deve ficar definida na máquina. O serviço passa a chave explicitamente ao SDK, então os dois primeiros são ignorados, mas `ANTHROPIC_BASE_URL` ainda redirecionaria as chamadas.
- Recomenda-se um limite de gasto mensal de US$ 5 no Console.

## Custo

Cerca de US$ 0,013 por resumo com o Sonnet 5.5 (esforço baixo, saída limitada a 2.000 tokens).

## Orçamento de tempo

O backend Java desiste após 30 s. O serviço respeita: Claude no máximo 20 s (prazo total, sem retentativas) + extrativo em menos de 1 s, ou seja, bem abaixo dos 30 s do backend.

## Como o fallback funciona

O serviço usa o motor extrativo, e responde `engine: "extractive"`, quando:

- não há `ANTHROPIC_API_KEY`;
- a chamada falha (conexão, timeout, limite de requisições, qualquer erro HTTP, inclusive crédito insuficiente);
- o prazo total de 20 s é excedido;
- o modelo recusa (`stop_reason == "refusal"`) ou corta a resposta (`stop_reason == "max_tokens"`);
- qualquer erro inesperado no motor generativo (apenas o nome da classe do erro é registrado);
- a resposta não traz texto.

O motivo é registrado no log (sem segredos), junto do `request_id` e do uso de tokens quando há resposta. Antes de qualquer motor, tags HTML são removidas do texto. O extrativo (pontuação por frequência de palavras, não TextRank) divide o texto em frases (também por quebras de linha), descarta frases repetidas, pontua cada uma pela frequência normalizada das palavras (ignorando stopwords em português), escolhe as melhores até cerca de 120 palavras e as devolve na ordem original. O resultado é determinístico.
