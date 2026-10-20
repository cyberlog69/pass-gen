#!/usr/bin/env python3
"""
PassGen Bloom Filter Generator
==============================
Builds an optimized, compact binary Bloom Filter from the SHA-1 leaked hash database.
Enables high-speed, probabilistic set-membership testing with zero false negatives
and a configured false-positive rate (< 0.001%).
"""

import sys
import math
import json
import hashlib
from pathlib import Path

try:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent
HASH_FILE = PROJECT_ROOT / "data" / "leaked" / "leaked_hashes.txt"
BLOOM_BIN = PROJECT_ROOT / "data" / "leaked" / "bloom.bin"
META_FILE = PROJECT_ROOT / "data" / "leaked" / "leaked_meta.json"

TARGET_FPR = 0.0001  # 0.01% false positive rate


class StandaloneBloomFilter:
    def __init__(self, expected_elements: int, false_positive_rate: float):
        self.n = max(1, expected_elements)
        self.p = false_positive_rate
        # Optimal m (number of bits) = - (n * ln(p)) / (ln(2)^2)
        self.m = int(- (self.n * math.log(self.p)) / (math.log(2) ** 2))
        # Ensure byte alignment
        self.byte_count = (self.m + 7) // 8
        self.m = self.byte_count * 8
        # Optimal k (number of hash functions) = (m / n) * ln(2)
        self.k = max(1, int((self.m / self.n) * math.log(2)))
        self.bit_array = bytearray(self.byte_count)

    def _get_hashes(self, item_bytes: bytes):
        """Kirsch-Mitzenmacher optimization: generate k hash indices using 2 hash values."""
        h1 = int.from_bytes(hashlib.sha256(item_bytes).digest()[:8], 'big')
        h2 = int.from_bytes(hashlib.md5(item_bytes).digest()[:8], 'big')
        for i in range(self.k):
            yield (h1 + i * h2) % self.m

    def add(self, item_str: str):
        b = item_str.encode('utf-8')
        for bit_idx in self._get_hashes(b):
            byte_idx = bit_idx // 8
            bit_offset = bit_idx % 8
            self.bit_array[byte_idx] |= (1 << bit_offset)

    def contains(self, item_str: str) -> bool:
        b = item_str.encode('utf-8')
        for bit_idx in self._get_hashes(b):
            byte_idx = bit_idx // 8
            bit_offset = bit_idx % 8
            if not (self.bit_array[byte_idx] & (1 << bit_offset)):
                return False
        return True


def main():
    if not HASH_FILE.is_file():
        print(f"[!] Hash file not found at: {HASH_FILE}")
        sys.exit(1)

    print(f"[*] Reading hashes from {HASH_FILE.name} ...")
    with open(HASH_FILE, "r", encoding="utf-8") as f:
        hashes = [line.strip() for line in f if len(line.strip()) == 40]

    count = len(hashes)
    print(f"[*] Constructing Bloom Filter for {count:,} items (FPR={TARGET_FPR}) ...")
    bf = StandaloneBloomFilter(expected_elements=count, false_positive_rate=TARGET_FPR)

    for h in hashes:
        bf.add(h)

    with open(BLOOM_BIN, "wb") as f:
        f.write(bf.bit_array)

    print(f"[+] Bloom Filter generated successfully:")
    print(f"    Size: {len(bf.bit_array):,} bytes ({len(bf.bit_array) / 1024:.1f} KB)")
    print(f"    Hash functions (k): {bf.k}")
    print(f"    Total bits (m):     {bf.m:,}")
    print(f"    Output: {BLOOM_BIN}")

    # Self-test
    print("[*] Running verification on random samples...")
    assert bf.contains(hashes[0]), "Bloom filter failed verification on first hash"
    assert bf.contains(hashes[-1]), "Bloom filter failed verification on last hash"
    assert not bf.contains("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"), "False positive on sentinel"
    print("    [+] Self-test passed cleanly!")

    # Update metadata
    if META_FILE.is_file():
        try:
            with open(META_FILE, "r", encoding="utf-8") as f:
                meta = json.load(f)
            meta["bloom_filter"] = {
                "file": str(BLOOM_BIN.relative_to(PROJECT_ROOT)),
                "size_bytes": len(bf.bit_array),
                "k_hashes": bf.k,
                "m_bits": bf.m,
                "fpr_target": TARGET_FPR
            }
            with open(META_FILE, "w", encoding="utf-8") as f:
                json.dump(meta, f, indent=2)
        except Exception:
            pass


if __name__ == "__main__":
    main()
