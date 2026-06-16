# LeagueTool ML service

FastAPI service that serves a **calibrated comp-vs-comp win-probability** model. The Spring backend
calls `POST /predict` during champ select; if no model is trained yet the service returns a neutral
`0.5` so the backend degrades gracefully.

## Layout
```
app/         FastAPI app, schemas, featurization, model loading/inference
training/    synthetic-data generator + train/calibrate/evaluate pipeline
models/      trained model artifact (git-ignored; produced by `make train`)
tests/       pytest suite (features, training pipeline, API)
```

## Quick start
```bash
make venv      # create .venv and install deps
make train     # train on synthetic data → models/winprob_model.pkl (+ metrics)
make dev       # serve on http://localhost:8001
make test      # run the test suite
```

## Endpoints
- `GET  /health` → `{status, model_loaded}`
- `GET  /model/info` → champion count + held-out metrics (accuracy, log-loss, ECE)
- `POST /predict` → `{win_probability, model_loaded}` for `{allies:[ids], enemies:[ids]}`

## Honest expectations
The bundled model trains on **synthetic** drafts with a known latent structure — it validates the
whole featurize → train → calibrate → serve pipeline and gives meaningful calibration/accuracy on
that synthetic task. It is **not** a claim about live play. Real accuracy comes once the Match-V5
crawler has built a corpus: comp-only prediction tops out around **51–55%**, rising to **~60%** with
player champion-mastery features. Probabilities are isotonic-calibrated so a displayed "58%" means
58%.
