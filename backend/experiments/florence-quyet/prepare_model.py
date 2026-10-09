"""Download the exact Microsoft Florence release and verify its LFS weight hash."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

from huggingface_hub import snapshot_download

MODEL_ID = "microsoft/Florence-2-base-ft"
REVISION = "f6c1a25888ffc1d945ee8a1a77ac833c7303d46e"
WEIGHTS_SHA256 = "58757d657ff44051314c8030b68e04cb1bb618ca9a4885418f111f6fb708185a"
MODEL_DIR = Path.home() / ".cache/danasea/florence-base-ft/models" / REVISION


def main() -> None:
    snapshot_download(
        MODEL_ID,
        revision=REVISION,
        local_dir=MODEL_DIR,
        ignore_patterns=["pytorch_model.bin"],
        max_workers=4,
    )
    with (MODEL_DIR / "model.safetensors").open("rb") as stream:
        actual = hashlib.file_digest(stream, "sha256").hexdigest()
    if actual != WEIGHTS_SHA256:
        raise ValueError(
            "The weight hash does not match the published Hugging Face LFS hash"
        )
    hashes = {}
    for path in sorted(MODEL_DIR.iterdir()):
        if path.is_file():
            with path.open("rb") as stream:
                hashes[path.name] = hashlib.file_digest(stream, "sha256").hexdigest()
    result = {
        "model_id": MODEL_ID,
        "revision": REVISION,
        "model_dir": str(MODEL_DIR),
        "weights_bytes": (MODEL_DIR / "model.safetensors").stat().st_size,
        "weights_verified_against_published_lfs": True,
        "local_file_hashes": hashes,
    }
    (Path(__file__).parent / "download.json").write_text(
        json.dumps(result, indent=2) + "\n"
    )
    print(json.dumps(result, indent=2), flush=True)


if __name__ == "__main__":
    main()
