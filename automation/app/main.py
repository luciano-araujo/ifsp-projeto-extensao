import logging
import os

from fastapi import FastAPI
from pydantic import BaseModel, field_validator

from app.config import MAX_WORDS, MIN_WORDS, get_settings, load_env_file
from app.extractive import strip_html
from app.service import summarize

load_env_file()
logging.basicConfig(level=logging.INFO)

app = FastAPI(title="Creator Workbench - Resumo de roteiros")


class SummarizeRequest(BaseModel):
    text: str

    @field_validator("text")
    @classmethod
    def check_size(cls, v: str) -> str:
        words = len(strip_html(v).split())  # conta palavras do texto, não das tags
        if words < MIN_WORDS:
            raise ValueError(f"O texto deve ter pelo menos {MIN_WORDS} palavras.")
        if words > MAX_WORDS:
            raise ValueError(f"O texto deve ter no máximo {MAX_WORDS} palavras.")
        return v


class SummarizeResponse(BaseModel):
    summary: str
    engine: str


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/summarize", response_model=SummarizeResponse)
def summarize_route(body: SummarizeRequest) -> SummarizeResponse:
    summary, engine = summarize(body.text, get_settings())
    return SummarizeResponse(summary=summary, engine=engine)


if __name__ == "__main__":  # python -m app.main
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", "8000")))
