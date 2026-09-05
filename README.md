# 🔐 Offline Secure Password, Passphrase & PIN Generator (PWA)

A **100% offline**, cryptographically secure Password, Passphrase & PIN Generator built with modern UI aesthetics, PWA installability, privacy-first architecture, and offline QR code sharing.

![License](https://img.shields.io/github/license/cyberlog69/pass-gen)
![PWA Ready](https://img.shields.io/badge/PWA-Ready-7c3aed)
![Offline](https://img.shields.io/badge/Offline-100%25-22c55e)
![Zero Dependencies](https://img.shields.io/badge/Dependencies-0-blue)

---

## ✨ Key Features

### 🔑 3 Generator Modes
- **Password Mode**: Configurable length (4–128 characters) with toggles for Uppercase, Lowercase, Numbers, Symbols, and Ambiguous character exclusion (`l, 1, I, O, 0, o, Q, S, 5, Z, 2`).
- **Passphrase Mode (Diceware)**: Generate memorable multi-word passphrases using an offline EFF wordlist. Custom separators (`-`, ` `, `.`, `_`), capitalization, and optional number injection.
- **PIN Mode**: Pure numeric PIN generation (4–16 digits) using CSPRNG for banking cards, locks, and device passcodes.

### 🛡️ Advanced Privacy & Security
- **Cryptographically Secure**: All randomness comes strictly from `crypto.getRandomValues()` (Web Crypto API CSPRNG) with modulo-bias elimination.
- **Custom Symbol Sets**: Customize allowed symbols or reset to default `!@#$%^&*()-_=+[]{}|;:,.<>?` to match strict website password rules.
- **Offline QR Code Transfer**: Generate an offline QR code SVG to transfer passwords directly to mobile devices without copy-pasting across networks.
- **Auto-Wipe Clipboard**: Automatically clears the clipboard after 30 seconds to prevent clipboard surveillance or history leaks.
- **Masked History with Timestamps**: Recent outputs are masked (`••••••••••••`) by default with relative creation timestamps (*"Just now"*, *"2m ago"*) and eye toggle buttons.
- **Smart Strength Guidance**: Dynamic contextual recommendations to optimize password length and complexity.
- **Settings Persistence**: Remembers your preferred mode, length, toggles, separator, and theme across sessions using `localStorage`.

### 📱 Progressive Web App (PWA)
- **Installable**: Full `manifest.json` for "Add to Home Screen" on iOS, Android, macOS, Windows, and Linux.
- **Cache-Only Service Worker (`sw.js`)**: Fast, reliable offline caching with zero external network calls.
- **Bundled Fonts**: `Inter.woff2` and `JetBrainsMono.woff2` preloaded locally.

### 🎨 Themes & UI Polish
- **5 Theme Presets**: *Neon Violet*, *Emerald Matrix*, *Sapphire Glass*, *Cyberpunk Amber*, and *Light Mode* (with automatic `prefers-color-scheme` detection).
- **One-Click Regenerate**: Click or tap anywhere on the generated output display to immediately roll a new password.
- **Bulk Generator**: Batch generate 5 to 50 passwords/passphrases/PINs at once, copy all, or download as `.txt`.
- **Full Accessibility**: Keyboard focus trap inside modals, `prefers-reduced-motion` animation support, and `<noscript>` warning.

---

## 🚀 Getting Started

No build tools or installation needed!

```bash
# Clone the repository
git clone https://github.com/cyberlog69/pass-gen.git

# Open index.html in your browser
start index.html      # Windows
open index.html       # macOS
xdg-open index.html   # Linux
```

---

## 📁 Project Structure

```
pass-gen/
├── .github/
│   └── workflows/
│       └── test.yml    # GitHub Actions automated CI testing
├── index.html          # Clean semantic markup, CSP, & PWA metadata
├── manifest.json       # Web App Manifest for PWA installation
├── sw.js               # Service Worker for offline asset caching
├── icon.svg            # PWA App Icon & Favicon
├── css/
│   ├── styles.css      # Themes, glassmorphism, responsive styles
│   └── fonts.css       # Local @font-face rules
├── fonts/
│   ├── Inter.woff2     # Bundled Inter font
│   └── JetBrainsMono.woff2  # Bundled JetBrains Mono font
├── js/
│   ├── qrcode.js       # Zero-dependency offline SVG QR code generator
│   ├── words.js        # Offline EFF wordlist for passphrase mode
│   └── app.js          # Core app logic, CSPRNG algorithms, & PassGen API
└── tests/
    ├── run-tests.js    # Node.js automated test runner for CI
    └── test.html       # In-browser unit test suite
```

---

## 🧪 Automated Testing

### CLI (Node.js)
```bash
node tests/run-tests.js
```

### Browser
Open `tests/test.html` in any web browser.

The test suite validates:
- EFF Wordlist integrity and immutability (`Object.freeze`)
- CSPRNG `secureRandom` bounds and uniformity
- Password, Passphrase, and PIN generator accuracy
- Ambiguous character stripping and Custom Symbol overrides
- Entropy bits and crack time calculations
- Relative time formatting and Strength Guidance mapping
- QR Code matrix generation and SVG rendering

---

## 📜 License

[MIT](LICENSE)
