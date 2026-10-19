# 🔐 Offline Secure Password, Passphrase & PIN Generator

A **100% offline**, cryptographically secure Password, Passphrase & PIN Generator built as a **Progressive Web App (PWA)**. Engineered with zero network permissions, client-side AES-256-GCM vault encryption, encrypted backup export/import, offline breached password detection, zero external dependencies, and modern aesthetic themes.

[![Live Web App](https://img.shields.io/badge/Live%20PWA-passcrypt.live-brightgreen?logo=firebase&logoColor=white)](https://passcrypt.live/)
![Version](https://img.shields.io/badge/Version-v1.2.2-blue)
![License](https://img.shields.io/github/license/cyberlog69/pass-gen)
![PWA Ready](https://img.shields.io/badge/PWA-Ready-7c3aed)
![Offline](https://img.shields.io/badge/Offline-100%25-22c55e)
![Zero Dependencies](https://img.shields.io/badge/Dependencies-0-blue)
![Firebase Hosting](https://img.shields.io/badge/Deploy-Firebase%20Hosting-FFA611?logo=firebase&logoColor=white)

---

## 🌐 Live Web App (PWA)

Access the live, installable Progressive Web App directly from anywhere:

👉 **[https://passcrypt.live/](https://passcrypt.live/)** *(Primary Domain)*  
👉 **[https://passgen-pwa-3903.web.app](https://passgen-pwa-3903.web.app)** *(Firebase CDN Fallback)*  
*(Hosted on Google Cloud / Firebase Global CDN with automated SSL and HTTP clickjacking defense)*

---

## ✨ Key Features

### 🔑 3 Generator Modes
- **Password Mode**: Configurable length (4–128 characters) with toggles for Uppercase, Lowercase, Numbers, Symbols, and Ambiguous character exclusion (`l, 1, I, O, 0, o, Q, S, 5, Z, 2`).
- **Passphrase Mode (Diceware)**: Generate memorable multi-word passphrases using an offline EFF wordlist. Custom separators (`-`, ` `, `.`, `_`), capitalization, and optional number injection.
- **PIN Mode**: Pure numeric PIN generation (4–16 digits) using CSPRNG for banking cards, SIM cards, and device passcodes.

### 🛡️ Offline Leaked & Compromised Password Detection
- **100% Offline Intelligence**: Instantly detects known breached passwords, keyboard walks (`qwerty`, `asdfgh`), trivial numeric sequences (`12345678`), and single-character repetitions without making network requests.
- **Dynamic Guidance**: Immediately downgrades entropy rating and warns the user if a password candidate or manual entry matches breach digests.

### 🗄️ Encrypted Offline Password Vault & Backup
- **AES-256-GCM Authenticated Encryption**: Passwords saved to the vault are protected with 256-bit AES-GCM and a 128-bit authentication tag.
- **PBKDF2 Key Derivation**: Uses 100,000 PBKDF2-HMAC-SHA256 iterations with a 128-bit random salt derived from your Master Passcode.
- **Encrypted Backup Export & Restore**: Export and import your encrypted vault (`passgen-vault-backup`) across devices with non-destructive merge support.
- **Zero Disk Exposure**: The Master Passcode is never persisted to disk or browser storage.
- **Inactivity Auto-Lock**: Vault items are automatically scrubbed from memory after 60 seconds of inactivity.

### 🔄 In-App Updater & Service Worker Lifecycle
- **Web PWA Service Worker Updater**: Automatically detects new deployments in the background, displays an interactive "Update Ready" banner, and instantly swaps to the new version with zero page disruption.
- **Manual "Check for Updates"**: Header button and About modal trigger explicit background checks via `registration.update()`.

### 🎨 Aesthetic Themes & Micro-Interactions
- **5 Aesthetic Themes**: *Neon Violet*, *Emerald Matrix*, *Sapphire Glass*, *Cyberpunk Amber*, and *Light Mode*.
- **One-Click Regenerate**: Tap the generated password display card to instantly roll a new password.
- **Bulk Generator**: Batch generate 5 to 50 passwords at once with one-click copy or download.
- **Offline QR Code Sharing**: Transfer credentials device-to-device via high-contrast SVG QR codes without network transfer.

---

## 🔒 Security Architecture & Guarantees

| Security Layer | Implementation | Benefit |
| :--- | :--- | :--- |
| **CSPRNG** | `crypto.getRandomValues()` + Rejection Sampling | Eliminates modulo bias across all character sets. |
| **CSP** | `default-src 'none'; script-src 'self'; connect-src 'none';` | Physically blocks all outbound network requests, exfiltration, and XSS. |
| **Framing Defense** | `X-Frame-Options: DENY` & `frame-ancestors 'none'` | Complete protection against clickjacking / UI redressing. |
| **HSTS** | `Strict-Transport-Security: max-age=31536000; preload` | Strictly enforces HTTPS on all modern browsers. |
| **Clipboard Privacy** | 30s auto-scrub timer | Automatically wipes copied credentials from the system clipboard. |
| **Zero Dependencies** | Pure Vanilla HTML5, CSS3, & JS (ES6+) | Eliminates supply-chain attacks and npm vulnerabilities. |

---

## 🚀 Getting Started Locally

No build tools or package managers needed!

```bash
# Clone the repository
git clone https://github.com/cyberlog69/pass-gen.git
cd pass-gen

# Open index.html in your browser
start index.html      # Windows
open index.html       # macOS
xdg-open index.html   # Linux
```

---

## 🧪 Automated Testing

PassGen includes an automated Node.js test runner covering CSPRNG bounds, PIN formatting, Diceware separators, QR generation, AES-256-GCM vault encryption, and breached password detection:

```bash
# Run unit tests
node tests/run-tests.js

# Or open in-browser test suite
start tests/test.html
```

---

## 📁 Project Structure

```
pass-gen/
├── index.html               # Web PWA markup, CSP meta tags, & UI layout
├── manifest.json            # Web App Manifest for PWA installation
├── sw.js                    # Service Worker for offline caching & background updates
├── icon.svg                 # PWA App Icon & Favicon
├── firebase.json            # Firebase Hosting configuration & HTTP security headers
├── .firebaserc              # Firebase project link (passgen-pwa-3903)
├── css/
│   ├── styles.css           # Modern glassmorphism, responsive styles & animations
│   └── fonts.css            # Local @font-face rules
├── fonts/                   # Bundled offline fonts (Inter, JetBrains Mono)
├── js/
│   ├── vault.js             # WebCrypto AES-256-GCM & SHA-256 vault engine (Export/Import)
│   ├── leaks.js             # Offline breached password & common pattern detector
│   ├── qrcode.js            # Zero-dependency offline SVG QR code generator
│   ├── words.js             # Offline EFF wordlist for passphrase mode
│   └── app.js               # Web CSPRNG, UI controller, & clipboard management
├── tests/
│   ├── run-tests.js         # Automated Node.js test runner for CI/CD
│   └── test.html            # In-browser unit test suite
├── .github/
│   ├── workflows/
│   │   ├── test.yml             # Automated CI test suite on pull requests
│   │   └── deploy-firebase.yml  # Automated deployment to Firebase Hosting on push to main
│   └── dependabot.yml       # Dependabot configuration for GitHub Actions
├── SECURITY.md              # Security vulnerability reporting policy
└── ROADMAP.md               # Future architectural roadmap & milestone checklist
```

---

## 📜 License

[MIT](LICENSE)
