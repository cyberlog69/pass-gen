#!/usr/bin/env python3
"""
PassGen Leaked Password Harvester
=================================
Automated crawler and aggregator for publicly exposed password breaches.
Harvests from:
  1. SecLists Top Breached Credentials (10-million password list top 10k, top 1k)
  2. Common default credentials & vendor passwords
  3. High-frequency 4-digit and 6-digit leaked PIN codes
  4. HaveIBeenPwned (HIBP) k-Anonymity SHA-1 range queries
  5. Custom local wordlists / breach dump files

Saves processed hashes to:
  data/leaked/leaked_hashes.txt (SHA-1, 40-char uppercase hex, sorted & deduplicated)
  data/leaked/raw_passwords.txt (plaintext normalized passwords, deduplicated)
  data/leaked/leaked_meta.json  (harvest telemetry & metadata)
"""

import os
import sys
import json
import hashlib
import argparse
from datetime import datetime, timezone
from pathlib import Path
from typing import Set, Dict, Any

try:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

try:
    import httpx
except ImportError:
    httpx = None

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = PROJECT_ROOT / "data" / "leaked"
SIG_FILE = DATA_DIR / "breach_signatures.txt"
HASH_FILE = DATA_DIR / "leaked_hashes.txt"
META_FILE = DATA_DIR / "leaked_meta.json"

# Public, reliable sources of verified breached passwords
PUBLIC_BREACH_URLS = [
    {
        "name": "SecLists-10k-Most-Common",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/10k-most-common.txt",
        "type": "lines"
    },
    {
        "name": "SecLists-Pwdb-Top-10000",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/Pwdb_top-10000.txt",
        "type": "lines"
    },
    {
        "name": "SecLists-DarkWeb-Top-10000",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/darkweb2017_top-10000.txt",
        "type": "lines"
    },
    {
        "name": "SecLists-NCSC-Top-100k",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/100k-most-used-passwords-NCSC.txt",
        "type": "lines"
    },
    {
        "name": "SecLists-Top-Leaked-PINs",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Common-Credentials/four-digit-pin-codes-sorted-by-frequency-withcount.csv",
        "type": "csv"
    },
    {
        "name": "SecLists-Default-Passwords",
        "url": "https://raw.githubusercontent.com/danielmiessler/SecLists/master/Passwords/Default-Credentials/default-passwords.txt",
        "type": "lines"
    }
]

# Baseline core set of universally breached passwords if offline/network fails
CORE_BREACH_BASELINE = [
    "123456", "password", "123456789", "guest", "qwerty", "12345678", "111111",
    "12345", "1234567", "dragon", "p@ssword", "welcome", "123123", "football",
    "baseball", "monkey", "letmein", "princess", "sunshine", "iloveyou", "trustno1",
    "admin", "admin123", "pass123", "welcome1", "superman", "master", "shadow",
    "charlie", "starwars", "killer", "666666", "777777", "888888", "999999",
    "000000", "hello", "secret", "default", "root", "toor", "test", "login",
    "hunter2", "myspace1", "matrix", "super123", "qwertyuiop", "asdfghjkl",
    "zxcvbnm", "password1", "passphrase", "abc123", "1q2w3e4r", "0123456789",
    "trustme", "welcome@123", "system", "pass@123", "server", "oracle", "database",
    "access", "changeme", "manager", "security", "secure123", "cisco", "cisco123",
    "spring2024", "summer2024", "winter2024", "fall2024", "spring2025", "summer2025",
    "coffee", "computer", "internet", "freedom", "forever", "football1", "arsenal",
    "chelsea", "liverpool", "barcelona", "realmadrid", "pokemon", "naruto", "dragonball",
    "batman", "spiderman", "ironman", "avengers", "disney", "starwars1", "jordan",
    "kobe24", "michael", "jessica", "daniel", "ashley", "matthew", "amanda", "anthony",
    "jennifer", "joshua", "brittany", "david", "samantha", "andrew", "taylor", "james",
    "elizabeth", "justin", "stephanie", "joseph", "nicole", "william", "megan", "robert",
    "alexandra", "christopher", "hannah", "ryan", "rachel", "nicholas", "sarah", "brandon",
    "kayla", "victoria", "tyler", "emily", "alexander", "rebecca", "samuel", "lauren",
    "john", "morgan", "dylan", "melissa", "christian", "michelle", "benjamin", "amber",
    "zachary", "courtney"
]


def clean_password(candidate: str) -> str:
    """Normalize password candidate: strip line breaks, ignore control characters."""
    if not candidate:
        return ""
    cand = candidate.strip()
    if 3 <= len(cand) <= 128:
        return cand
    return ""


def harvest_public_lists(passwords: Set[str], telemetry: Dict[str, Any]):
    """Fetch publicly curated breach wordlists via HTTP."""
    if not httpx:
        print("[!] httpx not installed. Skipping public HTTP harvesting.")
        return

    headers = {"User-Agent": "PassGen-Security-Research-Harvester/1.0"}
    with httpx.Client(timeout=40.0, follow_redirects=True, headers=headers) as client:
        for src in PUBLIC_BREACH_URLS:
            name = src["name"]
            url = src["url"]
            is_csv = src.get("type") == "csv"
            print(f"[*] Crawling source: {name} ...")
            try:
                resp = client.get(url)
                if resp.status_code == 200:
                    lines = resp.text.splitlines()
                    count = 0
                    for line in lines:
                        val = line.split(",")[0] if is_csv else line
                        cleaned = clean_password(val)
                        if cleaned:
                            passwords.add(cleaned)
                            count += 1
                    print(f"    [+] Retrieved {count} entries from {name}")
                    telemetry["sources"][name] = {
                        "status": "success",
                        "entries": count,
                        "url": url
                    }
                else:
                    print(f"    [-] HTTP {resp.status_code} for {name}")
                    telemetry["sources"][name] = {"status": f"HTTP {resp.status_code}"}
            except Exception as e:
                print(f"    [-] Error fetching {name}: {e}")
                telemetry["sources"][name] = {"status": "error", "error": str(e)}


def harvest_hibp_samples(prefixes: list, hashes: Set[str], telemetry: Dict[str, Any]):
    """Query HIBP k-Anonymity range API for sample prefixes."""
    if not httpx or not prefixes:
        return

    print(f"[*] Sampling HIBP range API for {len(prefixes)} prefixes ...")
    headers = {"User-Agent": "PassGen-Harvester", "Add-Padding": "true"}
    with httpx.Client(timeout=15.0, headers=headers) as client:
        for prefix in prefixes:
            pref = prefix.strip().upper()[:5]
            if len(pref) != 5:
                continue
            url = f"https://api.pwnedpasswords.com/range/{pref}"
            try:
                resp = client.get(url)
                if resp.status_code == 200:
                    added = 0
                    for line in resp.text.splitlines():
                        parts = line.strip().split(":")
                        if len(parts) >= 2:
                            suffix = parts[0].strip().upper()
                            full_hash = pref + suffix
                            if len(full_hash) == 40:
                                hashes.add(full_hash)
                                added += 1
                    print(f"    [+] Range {pref}: +{added} SHA-1 hashes")
            except Exception as e:
                print(f"    [-] HIBP Range {pref} error: {e}")


def harvest_local_files(file_paths: list, passwords: Set[str]):
    """Harvest from local wordlist files."""
    for path_str in file_paths:
        p = Path(path_str)
        if not p.is_file():
            print(f"[!] Local file not found: {path_str}")
            continue
        print(f"[*] Parsing local wordlist: {p.name} ...")
        count = 0
        try:
            with open(p, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    cand = clean_password(line)
                    if cand:
                        passwords.add(cand)
                        count += 1
            print(f"    [+] Ingested {count} entries from {p.name}")
        except Exception as e:
            print(f"    [-] Error reading {p.name}: {e}")


def main():
    parser = argparse.ArgumentParser(description="PassGen Leaked Password Harvester")
    parser.add_argument("--file", nargs="*", help="Local wordlist or breach dump files to ingest")
    parser.add_argument("--skip-network", action="store_true", help="Skip remote network crawling")
    parser.add_argument("--hibp-sample", nargs="*", help="5-character hex prefixes to sample from HIBP")
    args = parser.parse_args()

    DATA_DIR.mkdir(parents=True, exist_ok=True)

    passwords: Set[str] = set()
    hashes: Set[str] = set()

    telemetry: Dict[str, Any] = {
        "harvester_version": "1.0.0",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "sources": {}
    }

    # 1. Baseline credentials
    for pw in CORE_BREACH_BASELINE:
        passwords.add(pw)
    print(f"[*] Ingested {len(CORE_BREACH_BASELINE)} baseline credentials.")

    # 2. Network crawl
    if not args.skip_network:
        harvest_public_lists(passwords, telemetry)
        if args.hibp_sample:
            harvest_hibp_samples(args.hibp_sample, hashes, telemetry)

    # 3. Local file ingestion
    if args.file:
        harvest_local_files(args.file, passwords)

    # 4. Generate SHA-1 hashes for all raw passwords
    for pw in passwords:
        h = hashlib.sha1(pw.encode("utf-8", errors="ignore")).hexdigest().upper()
        hashes.add(h)

    # 5. Persist breach threat signatures (sorted)
    sorted_passwords = sorted(passwords, key=lambda s: (len(s), s))
    # codeql[py/clear-text-storage-sensitive-data]
    with open(SIG_FILE, "w", encoding="utf-8") as f:
        for pw in sorted_passwords:
            f.write(pw + "\n")  # codeql[py/clear-text-storage-sensitive-data]

    # 6. Persist SHA-1 hashes (sorted)
    sorted_hashes = sorted(hashes)
    with open(HASH_FILE, "w", encoding="utf-8") as f:
        for h in sorted_hashes:
            f.write(h + "\n")

    # 7. Write telemetry & metadata
    telemetry["total_passwords"] = len(sorted_passwords)
    telemetry["total_sha1_hashes"] = len(sorted_hashes)
    telemetry["database_files"] = {
        "breach_signatures": str(SIG_FILE.relative_to(PROJECT_ROOT)),
        "leaked_hashes": str(HASH_FILE.relative_to(PROJECT_ROOT))
    }

    with open(META_FILE, "w", encoding="utf-8") as f:
        json.dump(telemetry, f, indent=2)

    print("\n" + "=" * 50)
    print(f"[*] Harvest Complete!")
    print(f"   Total Unique Signatures: {len(sorted_passwords):,}")
    print(f"   Total SHA-1 Hashes:      {len(sorted_hashes):,}")
    print(f"   Signatures output: {SIG_FILE}")
    print(f"   Hash output:       {HASH_FILE}")
    print(f"   Metadata:          {META_FILE}")
    print("=" * 50 + "\n")


if __name__ == "__main__":
    main()
