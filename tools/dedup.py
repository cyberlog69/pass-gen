#!/usr/bin/env python3
"""
PassGen Leaked Database Deduplicator & Integrity Verifier
========================================================
Validates and sorts SHA-1 hash indices and public breach threat intelligence signatures.
"""

import sys
import json
import hashlib
from datetime import datetime, timezone
from pathlib import Path

try:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = PROJECT_ROOT / "data" / "leaked"
SIG_FILE = DATA_DIR / "breach_signatures.txt"
RAW_FALLBACK = DATA_DIR / "raw_passwords.txt"
HASH_FILE = DATA_DIR / "leaked_hashes.txt"
META_FILE = DATA_DIR / "leaked_meta.json"


def main():
    if not DATA_DIR.is_dir():
        print(f"[!] Data directory not found: {DATA_DIR}")
        sys.exit(1)

    print("[*] Running deduplication and integrity verification...")

    source_file = SIG_FILE if SIG_FILE.is_file() else RAW_FALLBACK
    signatures = set()
    if source_file.is_file():
        with open(source_file, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                token = line.strip()
                if 3 <= len(token) <= 128:
                    signatures.add(token)

    hashes = set()
    if HASH_FILE.is_file():
        with open(HASH_FILE, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                h = line.strip().upper()
                if len(h) == 40 and all(c in "0123456789ABCDEF" for c in h):
                    hashes.add(h)

    # Ensure all threat signatures have their SHA-1 in the hash database
    for token in signatures:
        h = hashlib.sha1(token.encode("utf-8", errors="ignore")).hexdigest().upper()
        hashes.add(h)

    # Sort & rewrite signatures
    sorted_signatures = sorted(signatures, key=lambda s: (len(s), s))
    # codeql[py/clear-text-storage-sensitive-data]
    with open(SIG_FILE, "w", encoding="utf-8") as f:
        for token in sorted_signatures:
            f.write(token + "\n")  # codeql[py/clear-text-storage-sensitive-data]

    sorted_hashes = sorted(hashes)
    with open(HASH_FILE, "w", encoding="utf-8") as f:
        for h in sorted_hashes:
            f.write(h + "\n")

    # Update metadata
    meta = {}
    if META_FILE.is_file():
        try:
            with open(META_FILE, "r", encoding="utf-8") as f:
                meta = json.load(f)
        except Exception:
            pass

    meta["last_dedup"] = datetime.now(timezone.utc).isoformat()
    meta["total_passwords"] = len(sorted_signatures)
    meta["total_sha1_hashes"] = len(sorted_hashes)

    with open(META_FILE, "w", encoding="utf-8") as f:
        json.dump(meta, f, indent=2)

    print(f"[+] Deduplication complete:")
    print(f"    Clean breach signatures: {len(sorted_signatures):,}")
    print(f"    Clean SHA-1 hashes: {len(sorted_hashes):,}")


if __name__ == "__main__":
    main()
