"""FastAPI app exposing comp-vs-comp win-probability inference.

If no trained model is present the service still answers, returning a neutral 0.5 with
``model_loaded=false`` so the backend degrades gracefully rather than failing.
"""
from __future__ import annotations

import logging

from fastapi import FastAPI

from app.config import MODEL_PATH
from app.model import WinProbabilityModel
from app.schemas import HealthResponse, ModelInfo, PredictRequest, PredictResponse

logger = logging.getLogger("leaguetool.ml")

app = FastAPI(
    title="LeagueTool ML service",
    version="0.1.0",
    description="Comp-vs-comp win-probability model (calibrated).",
)

_model: WinProbabilityModel | None = None


def get_model() -> WinProbabilityModel | None:
    global _model
    if _model is None and MODEL_PATH.exists():
        try:
            _model = WinProbabilityModel.load()
            logger.info("Loaded win-probability model (%d champions)", _model.known_champions)
        except Exception:  # pragma: no cover - defensive
            logger.exception("Failed to load model from %s", MODEL_PATH)
            _model = None
    return _model


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(status="ok", model_loaded=get_model() is not None)


@app.get("/model/info", response_model=ModelInfo)
def model_info() -> ModelInfo:
    model = get_model()
    if model is None:
        return ModelInfo(model_loaded=False)
    return ModelInfo(model_loaded=True, known_champions=model.known_champions, metrics=model.metrics)


@app.post("/predict", response_model=PredictResponse)
def predict(request: PredictRequest) -> PredictResponse:
    model = get_model()
    if model is None:
        return PredictResponse(
            win_probability=0.5,
            model_loaded=False,
            note="Model not trained yet; returning a neutral 0.5.",
        )
    probability = round(model.predict(request.allies, request.enemies), 4)
    return PredictResponse(win_probability=probability, model_loaded=True)
