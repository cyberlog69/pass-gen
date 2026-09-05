# 🔐 Offline Secure Password, Passphrase & PIN Generator

A **100% offline**, cryptographically secure Password, Passphrase & PIN Generator available as both a **Progressive Web App (PWA)** and a **Native Android App (Kotlin + Jetpack Compose)**. Designed with privacy-first architecture, zero network permissions, offline QR code sharing, and modern aesthetics.

![License](https://img.shields.io/github/license/cyberlog69/pass-gen)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![PWA Ready](https://img.shields.io/badge/PWA-Ready-7c3aed)
![Offline](https://img.shields.io/badge/Offline-100%25-22c55e)
![Zero Dependencies](https://img.shields.io/badge/Dependencies-0-blue)

---

## ✨ Key Features Across Platforms

### 🔑 3 Generator Modes
- **Password Mode**: Configurable length (4–128 characters) with toggles for Uppercase, Lowercase, Numbers, Symbols, and Ambiguous character exclusion (`l, 1, I, O, 0, o, Q, S, 5, Z, 2`).
- **Passphrase Mode (Diceware)**: Generate memorable multi-word passphrases using an offline EFF wordlist. Custom separators (`-`, ` `, `.`, `_`), capitalization, and optional number injection.
- **PIN Mode**: Pure numeric PIN generation (4–16 digits) using CSPRNG for banking cards, locks, and device passcodes.

### 🛡️ Advanced Privacy & Security
- **Cryptographically Secure**: All randomness comes strictly from `java.security.SecureRandom` (Android) and `crypto.getRandomValues()` (Web Crypto API CSPRNG) with modulo-bias elimination.
- **Custom Symbol Sets**: Customize allowed symbols or reset to default `!@#$%^&*()-_=+[]{}|;:,.<>?` to match strict website password rules.
- **Offline QR Code Transfer**: Generate an offline QR code to transfer passwords directly to mobile devices without copy-pasting across networks.
- **Auto-Wipe Clipboard**: Automatically clears the clipboard after 30 seconds to prevent clipboard surveillance or history leaks.
- **Masked History with Timestamps**: Recent outputs are masked (`••••••••••••`) by default with relative creation timestamps (*"Just now"*, *"2m ago"*) and eye toggle buttons.
- **Smart Strength Guidance**: Dynamic contextual recommendations to optimize password length and complexity.
- **Settings Persistence**: Remembers your preferred mode, length, toggles, separator, and theme across sessions.

### 🎨 Themes & UI Polish
- **5 Theme Presets**: *Neon Violet*, *Emerald Matrix*, *Sapphire Glass*, *Cyberpunk Amber*, and *Light Mode*.
- **One-Click Regenerate**: Click or tap anywhere on the generated output display to immediately roll a new password.
- **Bulk Generator**: Batch generate 5 to 50 passwords/passphrases/PINs at once, copy all, or download as `.txt`.

---

## 🤖 Native Android App (Kotlin + Jetpack Compose)

The repository contains a first-class native Android application located in `android/`.

### 🚀 Android Architectural Highlights & Optimizations
- **100% Offline Manifest Guarantee**: The `AndroidManifest.xml` deliberately **omits `android.permission.INTERNET`**. The Android OS physically prevents the app from opening network sockets or making external requests.
- **Window Screen Protection (`FLAG_SECURE`)**: Automatically enables `FLAG_SECURE` to block screenshots, video recordings, and task-switcher previews of sensitive credentials.
- **Sensitive Clipboard Masking**: Utilizes Android 13+ (API 33+) `ClipDescription.EXTRA_IS_SENSITIVE = true` to hide passwords from on-screen clipboard preview toasts, backed by a coroutine-based 30-second auto-wipe.
- **Lightweight Pure Kotlin QR Engine**: Replaced heavy external imaging/QR libraries with an embedded pure Kotlin QR Code matrix generator (`QrCodeEngine.kt`), keeping APK footprint tiny.
- **CSPRNG Rejection Sampling**: Implements uniform rejection sampling over `SecureRandom` to eliminate modulo bias across arbitrary character sets.
- **Modern Jetpack Compose UI**: Material 3 theming, edge-to-edge system bars, fluid haptic feedback on rolls and copies, and MVI/MVVM `StateFlow` state management.

### 🛠️ Building the Android App
Prerequisites: JDK 17+ and Android SDK (API 36).

```bash
cd android

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Output APK is generated at:
# android/app/build/outputs/apk/debug/app-debug.apk
```

---

## 🌐 Progressive Web App (PWA)

No build tools or installation needed for the web app!

```bash
# Clone the repository
git clone https://github.com/cyberlog69/pass-gen.git

# Open index.html in your browser
start index.html      # Windows
open index.html       # macOS
xdg-open index.html   # Linux
```

- **Installable**: Full `manifest.json` for "Add to Home Screen" on iOS, Android, macOS, Windows, and Linux.
- **Cache-Only Service Worker (`sw.js`)**: Fast, reliable offline caching with zero external network calls.
- **Bundled Fonts**: `Inter.woff2` and `JetBrainsMono.woff2` preloaded locally.

---

## 📁 Project Structure

```
pass-gen/
├── .github/
│   └── workflows/
│       └── test.yml         # Automated CI testing
├── android/                 # Native Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml   # Zero-network permission manifest
│   │   │   ├── java/com/cyberlog69/passgen/
│   │   │   │   ├── domain/           # CSPRNG, Diceware, Models, QR Engine
│   │   │   │   ├── ui/               # Compose Screens, ViewModels, Theme
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                  # Adaptive icons, themes, strings
│   │   ├── src/test/                 # Android unit test suite
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── index.html               # Web PWA markup, CSP, & metadata
├── manifest.json            # Web App Manifest for PWA installation
├── sw.js                    # Service Worker for offline asset caching
├── icon.svg                 # PWA App Icon & Favicon
├── css/
│   ├── styles.css           # Themes, glassmorphism, responsive styles
│   └── fonts.css            # Local @font-face rules
├── fonts/                   # Bundled fonts (Inter, JetBrains Mono)
├── js/
│   ├── qrcode.js            # Zero-dependency offline SVG QR code generator
│   ├── words.js             # Offline EFF wordlist for passphrase mode
│   └── app.js               # Web CSPRNG algorithms & UI controller
└── tests/
    ├── run-tests.js         # Node.js automated test runner for CI
    └── test.html            # In-browser unit test suite
```

---

## 🧪 Automated Testing

### Android Unit Tests
```bash
cd android
./gradlew test
```

### Web App Tests (Node.js & In-Browser)
```bash
# Node.js CLI
node tests/run-tests.js

# Browser
open tests/test.html
```

---

## 📜 License

[MIT](LICENSE)

