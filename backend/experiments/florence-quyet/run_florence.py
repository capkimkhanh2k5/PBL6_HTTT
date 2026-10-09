"""Run the pinned Microsoft Florence checkpoint offline on diagnostic images."""

from __future__ import annotations

import argparse
import hashlib
import importlib.metadata
import json
import os
import platform
import resource
import statistics
import time
from pathlib import Path
from typing import Any

import torch
from PIL import Image, ImageDraw, ImageFont
from transformers import AutoModelForCausalLM, AutoProcessor

DIRECTORY = Path(__file__).resolve().parent
REVISION = "f6c1a25888ffc1d945ee8a1a77ac833c7303d46e"
DEFAULT_MODEL = Path.home() / ".cache/danasea/florence-base-ft/models" / REVISION
PHOTO_TASKS = ["<CAPTION>", "<MORE_DETAILED_CAPTION>", "<OD>", "<OCR>"]
TEXT_TASKS = ["<OCR>", "<OCR_WITH_REGION>"]


def verify_model(path: Path) -> None:
    manifest = json.loads((DIRECTORY / "download.json").read_text())
    if manifest["revision"] != REVISION:
        raise ValueError("The model manifest does not match the pinned revision")
    for name, expected in manifest["local_file_hashes"].items():
        with (path / name).open("rb") as stream:
            actual = hashlib.file_digest(stream, "sha256").hexdigest()
        if actual != expected:
            raise ValueError(f"The local model file has changed: {name}")


def synchronize(device: str) -> None:
    if device == "mps":
        torch.mps.synchronize()


def overlay(image: Image.Image, parsed: dict, output: Path) -> None:
    preview = image.copy()
    draw = ImageDraw.Draw(preview)
    width = max(2, image.width // 300)
    font = ImageFont.load_default(size=max(16, image.width // 64))

    def label_at(x: float, y: float, label: str) -> None:
        label = label.replace("<s>", "").replace("</s>", "")
        bounds = draw.textbbox((0, 0), label, font=font)
        height = bounds[3] - bounds[1] + 8
        position = (x, max(0, y - height))
        background = draw.textbbox(position, label, font=font)
        draw.rectangle(background, fill="white")
        draw.text(position, label, fill="red", font=font)

    for box, label in zip(
        parsed.get("bboxes", []),
        parsed.get("labels", []) if "bboxes" in parsed else [],
        strict=True,
    ):
        draw.rectangle(box, outline="red", width=width)
        label_at(box[0], box[1], label)
    for box, label in zip(
        parsed.get("quad_boxes", []),
        parsed.get("labels", []) if "quad_boxes" in parsed else [],
        strict=True,
    ):
        points = list(zip(box[::2], box[1::2], strict=True))
        draw.line(points + [points[0]], fill="red", width=width)
        label_at(points[0][0], points[0][1], label)
    preview.thumbnail((1280, 1280))
    preview.save(output)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--device", choices=["cpu", "mps"], default="cpu")
    parser.add_argument("--threads", type=int, default=4)
    parser.add_argument("--model-dir", type=Path, default=DEFAULT_MODEL)
    parser.add_argument("--output", type=Path)
    parser.add_argument(
        "--image",
        type=Path,
        help="Process one local image instead of the fixture batch",
    )
    parser.add_argument(
        "--task",
        choices=["all", *dict.fromkeys(PHOTO_TASKS + TEXT_TASKS)],
        default="<MORE_DETAILED_CAPTION>",
    )
    args = parser.parse_args()
    if args.threads < 1:
        parser.error("threads must be positive")
    if args.device == "mps" and not torch.backends.mps.is_available():
        parser.error("Apple MPS is unavailable")
    torch.set_num_threads(args.threads)
    torch.set_num_interop_threads(1)
    torch.manual_seed(0)
    verify_model(args.model_dir)
    started = time.perf_counter()
    # Verified local pinned snapshot; no Hub request is allowed.
    processor = AutoProcessor.from_pretrained(  # nosec B615
        args.model_dir, trust_remote_code=True, local_files_only=True
    )
    model = (
        AutoModelForCausalLM.from_pretrained(  # nosec B615
            args.model_dir,
            trust_remote_code=True,
            local_files_only=True,
            torch_dtype=torch.float32,
            attn_implementation="eager",
        )
        .to(args.device)
        .eval()
    )
    synchronize(args.device)
    metadata = {
        "model_id": "microsoft/Florence-2-base-ft",
        "revision": REVISION,
        "model_dir": str(args.model_dir),
        "device": args.device,
        "dtype": "float32",
        "parameters": sum(p.numel() for p in model.parameters()),
        "cpu_threads": args.threads,
        "python": platform.python_version(),
        "machine": platform.machine(),
        "versions": {
            name: importlib.metadata.version(name)
            for name in ["torch", "transformers", "timm", "pillow", "einops"]
        },
        "offline_mode": os.getenv("HF_HUB_OFFLINE") == "1"
        and os.getenv("TRANSFORMERS_OFFLINE") == "1",
        "load_seconds": round(time.perf_counter() - started, 3),
        "generation": {
            "max_new_tokens": 512,
            "num_beams": 3,
            "do_sample": False,
            "use_cache": True,
        },
    }
    print(json.dumps({"loaded": metadata}), flush=True)
    fixtures = (
        [{"id": "custom", "path": str(args.image.resolve()), "kind": "custom"}]
        if args.image
        else json.loads((DIRECTORY / "fixtures.json").read_text())["fixtures"]
    )
    rows: list[dict[str, Any]] = []
    overlays = DIRECTORY / "overlays" / args.device
    overlays.mkdir(parents=True, exist_ok=True)
    for fixture in fixtures:
        path = DIRECTORY / fixture["path"]
        with Image.open(path) as opened:
            image = opened.convert("RGB")
        tasks = (
            (
                list(dict.fromkeys(PHOTO_TASKS + TEXT_TASKS))
                if args.task == "all"
                else [args.task]
            )
            if args.image
            else (PHOTO_TASKS if fixture["kind"] == "photo" else TEXT_TASKS)
        )
        row: dict[str, Any] = {
            "id": fixture["id"],
            "path": str(path.resolve()),
            "image_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "size": list(image.size),
            "tasks": {},
        }
        for task in tasks:
            synchronize(args.device)
            started = time.perf_counter()
            inputs = processor(text=task, images=image, return_tensors="pt").to(
                args.device
            )
            with torch.inference_mode():
                generated = model.generate(**inputs, **metadata["generation"])
            synchronize(args.device)
            latency_ms = (time.perf_counter() - started) * 1000
            raw = processor.batch_decode(generated, skip_special_tokens=False)[0]
            parsed = processor.post_process_generation(
                raw, task=task, image_size=image.size
            )[task]
            token_count = generated.shape[1] - 1
            truncated = (
                token_count >= metadata["generation"]["max_new_tokens"]
                and int(generated[0, -1]) != model.generation_config.eos_token_id
            )
            result = {
                "prompt": task,
                "raw": raw,
                "parsed": parsed,
                "latency_ms": round(latency_ms, 2),
                "generated_tokens": token_count,
                "truncated": truncated,
            }
            row["tasks"][task] = result
            if isinstance(parsed, dict) and task in {"<OD>", "<OCR_WITH_REGION>"}:
                preview = overlays / f"{fixture['id']}-{task.strip('<>').lower()}.png"
                overlay(image, parsed, preview)
                result["overlay"] = str(preview.resolve())
            print(
                json.dumps(
                    {
                        "id": fixture["id"],
                        "task": task,
                        "latency_ms": result["latency_ms"],
                        "parsed": parsed,
                        "truncated": truncated,
                    },
                    ensure_ascii=False,
                ),
                flush=True,
            )
        rows.append(row)
    latencies = [task["latency_ms"] for row in rows for task in row["tasks"].values()]
    output = (
        args.output
        or DIRECTORY
        / f"{'custom-vision' if args.image else 'florence'}-{args.device}.json"
    )
    report = {
        "metadata": metadata,
        "summary": {
            "images": len(rows),
            "task_calls": len(latencies),
            "task_latency_ms": {
                "median": round(statistics.median(latencies), 2),
                "min": min(latencies),
                "max": max(latencies),
            },
            "timing_note": "One sequential pass, including first invocation; not a warmed production benchmark",
            "peak_process_rss_mib": round(
                resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024**2, 1
            ),
        },
        "images": rows,
    }
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(
        json.dumps({"saved": str(output.resolve()), "summary": report["summary"]}),
        flush=True,
    )


if __name__ == "__main__":
    main()
