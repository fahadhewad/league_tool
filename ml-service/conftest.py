"""Ensure the ml-service root is importable as ``app`` / ``training`` under any test runner."""
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
