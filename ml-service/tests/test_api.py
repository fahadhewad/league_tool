from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_ok():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_predict_returns_a_probability():
    response = client.post("/predict", json={"allies": [1, 2, 3, 4, 5], "enemies": [6, 7, 8, 9, 10]})
    assert response.status_code == 200
    body = response.json()
    assert 0.0 <= body["win_probability"] <= 1.0
    assert "model_loaded" in body


def test_predict_accepts_empty_teams():
    response = client.post("/predict", json={"allies": [], "enemies": []})
    assert response.status_code == 200
