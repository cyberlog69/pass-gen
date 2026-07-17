# 🔐 Offline Secure Password Generator

A **fully offline**, cryptographically secure password generator. No data ever leaves your browser.

![License](https://img.shields.io/github/license/cyberlog69/pass-gen)

---

## ✨ Features

- 🔒 **Cryptographically random** — uses `crypto.getRandomValues()` (CSPRNG), not `Math.random()`
- 🌐 **100% Offline** — all fonts and assets are bundled locally; no network requests at runtime
- 🛡️ **Guaranteed character inclusion** — at least one character from each enabled set is always included
- 📊 **Entropy meter** — displays bits of entropy and a visual strength indicator (Weak → Very Strong)
- 📋 **Copy to clipboard** — one-click copy with visual feedback
- 🕐 **Password history** — keeps the last 5 generated passwords, each individually copyable
- ⌨️ **Keyboard shortcut** — `Ctrl + Enter` (or `Cmd + Enter`) to generate instantly
- ♿ **Accessible** — ARIA roles, labels, and keyboard navigation throughout
- 📱 **Responsive** — works on mobile and desktop

---

## 🚀 Getting Started

No installation or build step required. Just open the file:

```bash
# Clone the repo
git clone https://github.com/cyberlog69/pass-gen.git

# Open in your browser
open index.html       # macOS
start index.html      # Windows
xdg-open index.html   # Linux
```

Or simply double-click `index.html` in your file explorer.

---

## 📁 Project Structure

```
pass-gen/
├── index.html          # Main app — markup only
├── css/
│   ├── styles.css      # All styles (dark theme, glassmorphism, animations)
│   └── fonts.css       # Local @font-face declarations
├── fonts/
│   ├── Inter.woff2     # Bundled Inter font
│   └── JetBrainsMono.woff2  # Bundled JetBrains Mono font
├── js/
│   └── app.js          # All logic (IIFE, strict mode, public PassGen API)
└── tests/
    └── test.html       # Browser-based unit test suite
```

---

## 🧪 Running Tests

Open `tests/test.html` in your browser. The test suite covers:

| Test | Description |
|------|-------------|
| `secureRandom` bounds | Verifies output is always within `[0, max)` |
| `secureRandom` distribution | Chi-squared uniformity check |
| `shuffle` correctness | Preserves all elements and length |
| `buildCharset` | Correct character set composition |
| `calcEntropy` | Accurate entropy bit calculation |
| Strength mapping | Correct Weak / Fair / Strong / Very Strong classification |

---

## 🔧 Character Sets

| Toggle | Characters |
|--------|-----------|
| Uppercase | `A–Z` (26) |
| Lowercase | `a–z` (26) |
| Numbers | `0–9` (10) |
| Symbols | `!@#$%^&*()-_=+[]{}|;:,.<>?` (28) |

---

## 🛡️ Security Notes

- All randomness comes from the **Web Crypto API** (`crypto.getRandomValues`), which is cryptographically strong.
- A **bias-rejection loop** in `secureRandom()` eliminates modulo bias.
- The **Fisher-Yates shuffle** (using `secureRandom`) is used to shuffle the final password.
- No data is sent anywhere — the app works fully offline after the initial file load.

---

## 📜 License

[MIT](LICENSE)
