# 🔐 Offline Secure Password & Passphrase Generator (PWA)

A **100% offline**, cryptographically secure Password & Passphrase Generator built with modern UI aesthetics, PWA installability, and privacy-first security features.

![License](https://img.shields.io/github/license/cyberlog69/pass-gen)
![PWA Ready](https://img.shields.io/badge/PWA-Ready-7c3aed)
![Offline](https://img.shields.io/badge/Offline-100%25-22c55e)

---

## ✨ Key Features

### 🔑 Dual Generator Modes
- **Password Mode**: Configurable length (4–128 characters) with toggles for Uppercase, Lowercase, Numbers, and Symbols.
- **Passphrase Mode (Diceware)**: Generate memorable multi-word passphrases using an offline EFF wordlist. Custom separators (`-`, ` `, `.`, `_`), capitalization, and optional number injection.

### 🛡️ Advanced Security
- **Cryptographically Random**: Uses `crypto.getRandomValues()` (Web Crypto API CSPRNG) with modulo-bias elimination.
- **Exclude Ambiguous Characters**: Option to strip easily confused characters (`l, 1, I, O, 0, o, Q, S, 5, Z, 2`).
- **Auto-Wipe Clipboard**: Automatically clears the clipboard after 30 seconds to prevent password leakage.
- **Masked History**: Recent history items are masked (`••••••••••••`) by default with an eye icon toggle to reveal/hide.
- **Entropy & Crack Time Estimator**: Displays bit entropy alongside real-world estimated GPU cluster crack times.

### 📱 Progressive Web App (PWA)
- **Installable**: Includes `manifest.json` for "Add to Home Screen" on iOS, Android, macOS, and Windows.
- **Service Worker (`sw.js`)**: Cache-first strategy for static assets. Works 100% offline with zero external network requests.
- **Bundled Fonts**: `Inter.woff2` and `JetBrainsMono.woff2` preloaded locally.

### 🎨 Themes & Bulk Tools
- **Theme Picker**: 4 built-in theme presets (*Neon Violet*, *Emerald Matrix*, *Sapphire Glass*, *Cyberpunk Amber*).
- **Bulk Password Generator**: Modal to batch generate 5 to 50 passwords/passphrases at once, copy all, or download as `.txt`.
- **Haptic Feedback**: Subtle vibration response on mobile devices.

---

## 🚀 Getting Started

No build step or dependencies required!

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
├── index.html          # Clean HTML markup & PWA metadata
├── manifest.json       # Web App Manifest for PWA installation
├── sw.js               # Service Worker for offline asset caching
├── icon.svg            # PWA App Icon & Favicon
├── css/
│   ├── styles.css      # Themes, glassmorphic card, tabs, modals
│   └── fonts.css       # Local @font-face rules
├── fonts/
│   ├── Inter.woff2     # Bundled Inter font
│   └── JetBrainsMono.woff2  # Bundled JetBrains Mono font
├── js/
│   ├── words.js        # Offline EFF wordlist for passphrase mode
│   └── app.js          # Main app logic, CSPRNG algorithms & PassGen API
└── tests/
    └── test.html       # Comprehensive unit test suite
```

---

## 🧪 Unit Tests

Open `tests/test.html` in your web browser. The test suite covers:

- EFF Wordlist loading and integrity
- `secureRandom` bounds and CSPRNG behavior
- Ambiguous character stripping (`AMBIGUOUS` regex)
- Passphrase generation, separators, and capitalization
- Bit entropy calculations and crack time estimations

---

## 📜 License

[MIT](LICENSE)
