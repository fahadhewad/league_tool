VENV := .venv
PY := $(VENV)/bin/python
PIP := $(VENV)/bin/pip

.PHONY: venv train dev test clean

venv:
	python3 -m venv $(VENV)
	$(PIP) install -U pip
	$(PIP) install -r requirements.txt

train:
	$(PY) -m training.train

dev:
	$(VENV)/bin/uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload

test:
	$(PY) -m pytest -q

clean:
	rm -rf $(VENV) models/*.pkl .pytest_cache **/__pycache__
