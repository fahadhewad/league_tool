"""Request/response models for the ML API."""
from __future__ import annotations

from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    allies: list[int] = Field(default_factory=list, description="Ally champion ids")
    enemies: list[int] = Field(default_factory=list, description="Enemy champion ids")


class PredictResponse(BaseModel):
    win_probability: float = Field(description="Calibrated probability that the ally team wins")
    model_loaded: bool
    note: str | None = None


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool


class ModelInfo(BaseModel):
    model_loaded: bool
    known_champions: int = 0
    metrics: dict[str, float] = Field(default_factory=dict)
