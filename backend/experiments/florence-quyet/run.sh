#!/bin/sh
set -eu
FQ_SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
FLORENCE_PYTHON=${FLORENCE_PYTHON:-$HOME/.cache/danasea/florence-base-ft/.venv/bin/python}
QUYET_PYTHON=${QUYET_PYTHON:-$HOME/.cache/danasea/quyet-small/.venv/bin/python}
export HF_HUB_OFFLINE=1
export TRANSFORMERS_OFFLINE=1
export HF_HUB_DISABLE_TELEMETRY=1
export TOKENIZERS_PARALLELISM=false
FQ_COMMAND=${1:-batch}
if [ "$#" -gt 0 ]; then shift; fi
case "$FQ_COMMAND" in
    vision) exec "$FLORENCE_PYTHON" -u "$FQ_SCRIPT_DIR/run_florence.py" "$@" ;;
    decide) exec "$QUYET_PYTHON" -u "$FQ_SCRIPT_DIR/run_quyet.py" "$@" ;;
    batch)
        if [ "$#" -ne 0 ]; then printf '%s\n' 'Batch accepts no arguments; use vision or decide for options.' >&2; exit 2; fi
        "$FLORENCE_PYTHON" -u "$FQ_SCRIPT_DIR/run_florence.py" --device cpu
        exec "$QUYET_PYTHON" -u "$FQ_SCRIPT_DIR/run_quyet.py"
        ;;
    *) printf '%s\n' 'Usage: run.sh [batch | vision ARGS | decide ARGS]' >&2; exit 2 ;;
esac
