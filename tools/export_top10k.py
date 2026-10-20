#!/usr/bin/env python3
"""
PassGen Leak Exporter
====================
Compiles the harvested leaked password database into an optimized, offline,
zero-dependency JavaScript Set for client-side inclusion in `js/leaks.js`.
Preserves all pattern-checking heuristics (walks, sequences, repeats) and
maintains strict compatibility with PassGen's zero-network offline guarantee.
"""

import sys
import json
from pathlib import Path

try:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RAW_FILE = PROJECT_ROOT / "data" / "leaked" / "raw_passwords.txt"
LEAKS_JS_FILE = PROJECT_ROOT / "js" / "leaks.js"
META_FILE = PROJECT_ROOT / "data" / "leaked" / "leaked_meta.json"

TARGET_COUNT = 20000

# High-frequency universal baseline passwords guaranteed to be included
CORE_BASELINE = [
    "password", "123456", "123456789", "guest", "qwerty", "12345678", "111111",
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


def escape_js_string(s: str) -> str:
    """Safely escape string for JavaScript single-quote string literal."""
    return s.replace('\\', '\\\\').replace("'", "\\'").replace('\n', '\\n').replace('\r', '')


def main():
    if not RAW_FILE.is_file():
        print(f"[!] Raw passwords file not found at: {RAW_FILE}")
        sys.exit(1)

    print(f"[*] Reading raw leaked passwords from {RAW_FILE.name} ...")
    selected = []
    seen = set()

    # 1. Add core baseline first
    for pw in CORE_BASELINE:
        clean = pw.strip().lower()
        if clean and clean not in seen:
            seen.add(clean)
            selected.append(clean)

    # 2. Add all harvested passwords
    with open(RAW_FILE, "r", encoding="utf-8", errors="ignore") as f:
        for line in f:
            pw = line.strip()
            if 3 <= len(pw) <= 64:
                lower = pw.lower()
                if lower not in seen:
                    seen.add(lower)
                    selected.append(lower)
                    if len(selected) >= TARGET_COUNT:
                        break
    print(f"[+] Selected top {len(selected):,} dangerous leaked passwords for offline JS runtime.")

    # Format into JS array items (8 items per line for readability)
    formatted_items = []
    line_buf = []
    for pw in selected:
        line_buf.append(f"'{escape_js_string(pw)}'")
        if len(line_buf) >= 8:
            formatted_items.append("    " + ", ".join(line_buf))
            line_buf = []
    if line_buf:
        formatted_items.append("    " + ", ".join(line_buf))

    passwords_js_block = ",\n".join(formatted_items)

    js_template = f"""(function () {{
  'use strict';

  // ── Curated Offline Database of Most Common Leaked & Dangerous Passwords ──
  // Automatically compiled from public breach intelligence (SecLists 10k, DarkWeb dumps, HIBP digests).
  // Total compiled breach signatures: {len(selected):,} entries.
  const COMMON_LEAKED_PASSWORDS = new Set([
{passwords_js_block}
  ]);

  /**
   * Checks if a password string is in the offline common leak dictionary
   * or matches trivial sequential/repeated patterns.
   * @param {{string}} password
   * @returns {{{{ isLeaked: boolean, reason?: string }}}}
   */
  function checkLeakedPassword(password) {{
    if (!password || typeof password !== 'string') {{
      return {{ isLeaked: false }};
    }}

    const clean = password.trim().toLowerCase();

    // 1. Direct match in compiled breach database
    if (COMMON_LEAKED_PASSWORDS.has(clean)) {{
      return {{
        isLeaked: true,
        reason: 'Known breached password found in public leak databases.'
      }};
    }}

    // 2. Pure single-character repetition (e.g., 'aaaaaa', '1111111')
    if (password.length >= 4 && /^([\\s\\S])\\1+$/.test(password)) {{
      return {{
        isLeaked: true,
        reason: 'Trivial repeated character pattern.'
      }};
    }}

    // 3. Trivial sequential digits (e.g., '12345678', '987654321')
    if (/^(0123456789|123456789|9876543210|987654321)+$/.test(clean)) {{
      return {{
        isLeaked: true,
        reason: 'Trivial numeric keyboard sequence.'
      }};
    }}

    // 4. Keyboard walk patterns
    const walks = ['qwerty', 'asdfgh', 'zxcvbn', '1qaz2wsx'];
    for (const walk of walks) {{
      if (clean.includes(walk)) {{
        return {{
          isLeaked: true,
          reason: 'Common keyboard walk pattern detected.'
        }};
      }}
    }}

    return {{ isLeaked: false }};
  }}

  function isExcluded(password) {{
    return checkLeakedPassword(password).isLeaked;
  }}

  const LeakedPasswordChecker = Object.freeze({{
    checkLeakedPassword,
    isExcluded,
    COMMON_LEAK_COUNT: COMMON_LEAKED_PASSWORDS.size
  }});

  if (typeof window !== 'undefined') {{
    window.LeakedPasswordChecker = LeakedPasswordChecker;
  }}
  if (typeof global !== 'undefined') {{
    global.LeakedPasswordChecker = LeakedPasswordChecker;
  }}
}})();
"""

    with open(LEAKS_JS_FILE, "w", encoding="utf-8") as f:
        f.write(js_template)

    print(f"[+] Successfully wrote {len(selected):,} breach signatures to {LEAKS_JS_FILE}")

    # Update metadata
    if META_FILE.is_file():
        try:
            with open(META_FILE, "r", encoding="utf-8") as f:
                meta = json.load(f)
            meta["exported_js_signatures"] = len(selected)
            with open(META_FILE, "w", encoding="utf-8") as f:
                json.dump(meta, f, indent=2)
        except Exception:
            pass


if __name__ == "__main__":
    main()
