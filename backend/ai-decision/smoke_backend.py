"""Run the packaged backend with isolated Docker data and the real local worker."""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RESULT = Path(__file__).with_name("backend-smoke-results.json")
LOG = Path("/tmp/danasea-ai-runtime-smoke.log")


def command(*args: str, data: str | None = None) -> str:
    result = subprocess.run(
        args, input=data, capture_output=True, text=True, check=True
    )
    return result.stdout.strip()


def jwt(email: str, role: str, secret: str) -> str:
    def encoded(value: dict) -> bytes:
        return base64.urlsafe_b64encode(
            json.dumps(value, separators=(",", ":")).encode()
        ).rstrip(b"=")

    now = int(time.time())
    body = (
        encoded({"alg": "HS256", "typ": "JWT"})
        + b"."
        + encoded(
            {
                "sub": email,
                "role": role,
                "session_version": 0,
                "iat": now,
                "exp": now + 600,
            }
        )
    )
    signature = hmac.new(secret.encode(), body, hashlib.sha256).digest()
    return (body + b"." + base64.urlsafe_b64encode(signature).rstrip(b"=")).decode()


def free_port() -> int:
    with socket.socket() as handle:
        handle.bind(("127.0.0.1", 0))
        return handle.getsockname()[1]


def request(
    port: int, path: str, body: dict | None = None, token: str | None = None
) -> tuple[int, dict]:
    headers = {"Content-Type": "application/json", "Accept-Language": "en"}
    if token:
        headers["Authorization"] = "Bearer " + token
    payload = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(
        f"http://127.0.0.1:{port}{path}", data=payload, headers=headers
    )
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return response.status, json.loads(response.read())
    except urllib.error.HTTPError as response:
        return response.code, json.loads(response.read())


def main() -> None:
    key = (Path.home() / ".cache/danasea/quyet-small/service.key").read_text().strip()
    with urllib.request.urlopen("http://127.0.0.1:8091/health", timeout=3) as health:
        assert json.loads(health.read())["status"] == "UP"
    suffix = uuid.uuid4().hex[:10]
    postgres, redis = (
        "danasea-ai-smoke-pg-" + suffix,
        "danasea-ai-smoke-redis-" + suffix,
    )
    password, signing_secret = secrets.token_hex(16), secrets.token_hex(16)
    created: list[str] = []
    process = None
    log_handle = None
    results: list[dict] = []
    try:
        command(
            "docker",
            "run",
            "-d",
            "--name",
            postgres,
            "-p",
            "127.0.0.1::5432",
            "-e",
            "POSTGRES_DB=ai_smoke",
            "-e",
            "POSTGRES_USER=smoke",
            "-e",
            "POSTGRES_PASSWORD=" + password,
            "postgres:16-alpine",
        )
        created.append(postgres)
        command(
            "docker",
            "run",
            "-d",
            "--name",
            redis,
            "-p",
            "127.0.0.1::6379",
            "redis:7.0-alpine",
        )
        created.append(redis)
        for _ in range(60):
            check = subprocess.run(
                [
                    "docker",
                    "exec",
                    postgres,
                    "pg_isready",
                    "-U",
                    "smoke",
                    "-d",
                    "ai_smoke",
                ],
                capture_output=True,
                check=False,
            )
            if check.returncode == 0:
                break
            time.sleep(0.5)
        pg_port = command("docker", "port", postgres, "5432/tcp").rsplit(":", 1)[1]
        redis_port = command("docker", "port", redis, "6379/tcp").rsplit(":", 1)[1]
        port = free_port()
        env = os.environ.copy()
        env.update(
            SPRING_DATASOURCE_URL=f"jdbc:postgresql://127.0.0.1:{pg_port}/ai_smoke",
            SPRING_DATASOURCE_USERNAME="smoke",
            SPRING_DATASOURCE_PASSWORD=password,
            SPRING_REDIS_HOST="127.0.0.1",
            SPRING_REDIS_PORT=redis_port,
            APP_JWT_SECRET=signing_secret,
            SPRING_MAIL_USERNAME="smoke@example.test",
            SPRING_MAIL_PASSWORD="unused",
            GROQ_API_KEYS="",
            AI_QUYET_ENABLED="true",
            AI_QUYET_BASE_URL="http://127.0.0.1:8091",
            AI_QUYET_API_KEY=key,
        )
        descriptor = os.open(LOG, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
        log_handle = os.fdopen(descriptor, "w")
        process = subprocess.Popen(
            [
                "java",
                "-jar",
                str(ROOT / "backend/target/backend-0.0.1-SNAPSHOT.jar"),
                "--server.port=" + str(port),
                "--app.scheduler.enabled=false",
                "--spring.rabbitmq.listener.simple.auto-startup=false",
                "--spring.rabbitmq.listener.direct.auto-startup=false",
                "--management.health.rabbit.enabled=false",
                "--management.health.mail.enabled=false",
            ],
            cwd=ROOT,
            env=env,
            stdout=log_handle,
            stderr=subprocess.STDOUT,
        )
        for _ in range(90):
            if process.poll() is not None:
                raise RuntimeError(
                    "Packaged backend exited; inspect the private runtime log"
                )
            try:
                status, health_body = request(port, "/actuator/health")
                if status == 200 and health_body.get("status") == "UP":
                    break
            except (OSError, urllib.error.URLError):
                pass
            time.sleep(0.5)
        else:
            raise RuntimeError("Packaged backend did not become healthy")
        owner, other, admin, service, option, slot = [
            str(uuid.uuid4()) for _ in range(6)
        ]
        sql = f"""
            INSERT INTO users(id,email,role,full_name,password_hash,is_email_verified,is_locked,created_at,updated_at)
            VALUES ('{owner}','owner@example.test','CUSTOMER','Smoke owner','unused',true,false,now(),now()),
                   ('{other}','other@example.test','CUSTOMER','Smoke other','unused',true,false,now(),now()),
                   ('{admin}','admin@example.test','ADMIN','Smoke admin','unused',true,false,now(),now());
            INSERT INTO services(id,name,description,status,weather_sensitive,duration_minutes,view_count,created_at,updated_at)
            VALUES ('{service}','Kayak smoke','Guided coastal kayaking','PUBLISHED',false,60,7,now(),now());
            INSERT INTO service_options(id,service_id,name,option_type,pricing_unit,price,max_pax_per_package,status)
            VALUES ('{option}','{service}','Private kayak','PRIVATE','PER_PACKAGE',150000,2,'ACTIVE');
            INSERT INTO service_slots(id,service_id,date,start_time,end_time,capacity,booked_count,status,inventory_type,created_at,updated_at)
            VALUES ('{slot}','{service}',(now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+1,'09:00','10:00',10,0,'OPEN','SHARED_CAPACITY_UNITS',now(),now());
            INSERT INTO service_slot_units(id,slot_id,unit_number,capacity,booked_count)
            VALUES ('{uuid.uuid4()}','{slot}',1,2,0),('{uuid.uuid4()}','{slot}',2,2,0);
        """
        command(
            "docker",
            "exec",
            "-i",
            postgres,
            "psql",
            "-v",
            "ON_ERROR_STOP=1",
            "-U",
            "smoke",
            "-d",
            "ai_smoke",
            data=sql,
        )
        owner_token, other_token, admin_token = (
            jwt("owner@example.test", "CUSTOMER", signing_secret),
            jwt("other@example.test", "CUSTOMER", signing_secret),
            jwt("admin@example.test", "ADMIN", signing_secret),
        )
        status, classification = request(
            port,
            "/api/ai/classifications/service",
            {"text": "Tour kayak có hướng dẫn viên"},
            owner_token,
        )
        assert (
            status == 200
            and classification["available"]
            and classification["suggestionOnly"]
        )
        results.append(
            {
                "check": "JWT_AND_REAL_QUYET_CLASSIFICATION",
                "httpStatus": status,
                "model": classification["model"],
                "revision": classification["revision"],
            }
        )
        criteria = {"query": "Kayak ngày mai cho 3 người", "totalBudget": 500000}
        status, found = request(port, "/api/ai/recommendations", criteria, owner_token)
        assert status == 200 and found["candidates"][0]["minimumPartyTotal"] == 300000
        assert (
            found["intent"]["available"]
            and found["candidates"][0]["relevance"]["available"]
        )
        results.append(
            {
                "check": "CURRENT_PACKAGE_QUOTE_AND_REAL_QUYET_RANKING",
                "partyTotal": 300000,
                "intentAvailable": True,
                "relevanceAvailable": True,
            }
        )
        status, plan = request(port, "/api/ai/itineraries", criteria, owner_token)
        assert (
            status == 200
            and len(plan["plan"]["items"]) == 1
            and not plan["plan"]["inventoryReserved"]
        )
        status, _ = request(
            port, "/api/ai/itineraries/" + plan["id"], token=other_token
        )
        assert status == 404
        results.append(
            {
                "check": "PERSISTED_PLAN_AND_FOREIGN_OWNER_BLOCKED",
                "foreignReadStatus": status,
            }
        )
        status, assessment = request(
            port,
            "/api/ai/content-assessments",
            {"text": "Không chuyển tiền ngoài hệ thống DANASEA."},
            owner_token,
        )
        assert (
            status == 200
            and assessment["moderation"]["available"]
            and not assessment["publicationAuthorized"]
        )
        case_id = assessment["caseRecord"]["id"]
        status, _ = request(port, "/api/admin/ai/assessment-cases", token=owner_token)
        assert status == 403
        status, resolved = request(
            port,
            "/api/admin/ai/assessment-cases/" + case_id + "/resolve",
            {"resolution": "DISMISSED", "note": "Synthetic warning manually reviewed"},
            admin_token,
        )
        assert status == 200 and resolved["resolvedBy"] == admin
        results.append(
            {
                "check": "REAL_QUYET_MODERATION_AND_ADMIN_REVIEW",
                "publicationAuthorized": False,
                "modelSuggestionReasons": assessment["reasonCodes"],
            }
        )
        count = command(
            "docker",
            "exec",
            postgres,
            "psql",
            "-tAc",
            f"select view_count from services where id='{service}'",
            "-U",
            "smoke",
            "-d",
            "ai_smoke",
        )
        assert count == "7"
        results.append({"check": "AI_READS_PRESERVE_VIEW_COUNT", "viewCount": 7})
        RESULT.write_text(
            json.dumps(
                {
                    "modelQualityEvaluation": False,
                    "isolatedData": True,
                    "checks": results,
                },
                ensure_ascii=False,
                indent=2,
            )
            + "\n"
        )
        print(
            json.dumps(
                {
                    "checksPassed": len(results),
                    "resultFile": str(RESULT),
                    "existingDatabaseChanged": False,
                }
            )
        )
    finally:
        if process is not None and process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=20)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
        if log_handle is not None:
            log_handle.close()
        for name in reversed(created):
            subprocess.run(
                ["docker", "rm", "-f", name], capture_output=True, check=False
            )


if __name__ == "__main__":
    main()
