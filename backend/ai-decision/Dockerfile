FROM python:3.11-slim
WORKDIR /app
ENV QUYET_PYTHON=python PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 HF_HUB_OFFLINE=1 TRANSFORMERS_OFFLINE=1 TOKENIZERS_PARALLELISM=false HF_HUB_DISABLE_TELEMETRY=1
COPY requirements.lock.txt .
RUN pip install --no-cache-dir -r requirements.lock.txt && useradd --uid 10001 --create-home worker
COPY quyet_service.py rubrics.py run.sh ./
USER worker
EXPOSE 8091
CMD ["sh", "/app/run.sh"]
