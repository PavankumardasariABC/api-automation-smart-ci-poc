@echo off
set PORT=%1
if "%PORT%"=="" set PORT=8000
echo Starting local server on http://localhost:%PORT%
python -m http.server %PORT%
