# 🔐 Offline Secure Password, Passphrase & PIN Generator

A **100% offline**, cryptographically secure Password, Passphrase & PIN Generator available as both a **Progressive Web App (PWA)** and a **Native Android App (Kotlin + Jetpack Compose)**. Engineered with zero network permissions, client-side AES-256-GCM vault encryption, encrypted backup export/import, offline breached password detection, zero-repeat deduplication intelligence, Android 13+ Material You themed icons, and modern aesthetics.

![Latest Release](https://img.shields.io/badge/Release-v1.2.0-blue?logo=android&logoColor=white)
![License](https://img.shields.io/github/license/cyberlog69/pass-gen)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?logo=android&logoColor=white)
![Material You](https://img.shields.io/badge/Material%20You-Themed%20Icons-7c3aed)
![PWA Ready](https://img.shields.io/badge/PWA-Ready-7c3aed)
![Offline](https://img.shields.io/badge/Offline-100%25-22c55e)
![Zero Dependencies](https://img.shields.io/badge/Dependencies-0-blue)

---

## 📲 Download Latest Android Release (v1.2.0)

You can directly install the pre-compiled, optimized release APK on your Android device (Android 8.0 / API 26 through Android 15+):

- **GitHub Release**: [PassGen v1.2.0 Release](https://github.com/cyberlog69/pass-gen/releases/tag/v1.2.0)
- **Direct Download (Local)**: [`release/PassGen-v1.2.0.apk`](release/PassGen-v1.2.0.apk)
- **Direct Download (GitHub)**: [Download PassGen-v1.2.0.apk](https://github.com/cyberlog69/pass-gen/releases/download/v1.2.0/PassGen-v1.2.0.apk)
- **File Size**: ~1.19 MB (Shrunk with R8 optimization and resource shrinking)
- **Version**: `1.2.0` (versionCode `3`)

### 🔒 Cryptographic Checksum Verification
Verify the authenticity and integrity of your downloaded APK using SHA-256:
```bash
# PowerShell
Get-FileHash release/PassGen-v1.2.0.apk -Algorithm SHA256

# Linux / macOS
sha256sum release/PassGen-v1.2.0.apk
```
```
SHA256: 76CFE5994914D9639C6C2CA33467FD1D5362417EFABFD6A7A490562BBF9D0180
```

---

## ✨ Key Features Across Platforms

### 🔑 3 Generator Modes
- **Password Mode**: Configurable length (4–128 characters) with toggles for Uppercase, Lowercase, Numbers, Symbols, and Ambiguous character exclusion (`l, 1, I, O, 0, o, Q, S, 5, Z, 2`).
- **Passphrase Mode (Diceware)**: Generate memorable multi-word passphrases using an offline EFF wordlist. Custom separators (`-`, ` `, `.`, `_`), capitalization, and optional number injection.
- **PIN Mode**: Pure numeric PIN generation (4–16 digits) using CSPRNG for banking cards, SIM cards, and device passcodes.

### 🛡️ Offline Leaked & Compromised Password Detection
- **100% Offline Intelligence**: Instantly detects known breached passwords, keyboard walks (`qwerty`, `asdfgh`), trivial numeric sequences (`12345678`), and single-character repetitions without making network requests.
- **Dynamic Guidance**: Immediately downgrades entropy rating and warns the user if a password candidate or manual entry matches breach digests.

### 🗄️ Encrypted Offline Password Vault & Cross-Platform Backup
- **AES-256-GCM Authenticated Encryption**: Passwords saved to the vault are protected with military-grade 256-bit AES-GCM and a 128-bit authentication tag.
- **PBKDF2 Key Derivation**: Uses 100,000 PBKDF2-HMAC-SHA256 iterations with a 128-bit random salt derived from your Master Passcode.
- **Encrypted Backup Export & Restore**: Export and import your encrypted vault (`passgen-vault-backup`) between the Web PWA and Android app with non-destructive merge support.
- **Zero Disk Exposure**: The Master Passcode is never persisted to disk or device preferences.
- **Inactivity Auto-Lock**: Vault items are automatically scrubbed from memory after 60 seconds of inactivity.


### 🧠 Intelligent Zero-Repeat Deduplication (Never Generates Same Password Twice)
- **Irreversible SHA-256 Salting**: Passwords are **never stored in plaintext** for repeat checking. Each device generates a 256-bit CSPRNG pepper ($H = \text{SHA-256}(\text{pepper} \parallel P)$).
- **Collision Prevention**: Automatically re-rolls up to 10 attempts if a candidate matches a previously generated hash.
- **Hacker-Proof**: Even if local storage or memory is extracted, SHA-256 is an irreversible cryptographic one-way function that cannot be cracked backwards.

### 🗄️ Encrypted Offline Password Vault
- **AES-256-GCM Authenticated Encryption**: Passwords saved to the vault are protected with military-grade 256-bit AES-GCM and a 128-bit authentication tag.
- **PBKDF2 Key Derivation**: Uses 100,000 PBKDF2-HMAC-SHA256 iterations with a 128-bit random salt derived from your Master Passcode.
- **Zero Disk Exposure**: The Master Passcode is never persisted to disk or device preferences.
- **Inactivity Auto-Lock**: Vault items are automatically scrubbed from memory after 60 seconds of inactivity.

### 🎨 Material You & Theme Presets
- **Android 13+ Material You Themed Icons**: Includes dedicated `<monochrome>` adaptive launcher vector drawables that dynamically adapt to your Android system wallpaper colors (Monet palette).
- **5 Aesthetic Themes**: *Neon Violet*, *Emerald Matrix*, *Sapphire Glass*, *Cyberpunk Amber*, and *Light Mode*.
- **One-Click Regenerate**: Tap the generated password display card to instantly roll a new password.
- **Bulk Generator**: Batch generate 5 to 50 passwords at once with one-click copy or download.

### 🛡️ Privacy-First Architecture
- **100% Offline**: Neither the Web app nor the Android app makes any external HTTP/socket calls. The Android app completely omits `INTERNET` permission in its manifest.
- **Window Screen Protection (`FLAG_SECURE`)**: Automatically blocks screenshots, video captures, and task-switcher previews on Android.
- **Sensitive Clipboard Scrubbing**: Marks clipboard copies as sensitive on Android 13+ (`ClipDescription.EXTRA_IS_SENSITIVE`) and auto-wipes clipboard data after 30 seconds.
- **Offline QR Code Sharing**: Transfer credentials device-to-device via high-contrast QR codes without network transfer.

---

## 🤖 Native Android App (Kotlin + Jetpack Compose)

The native Android application is located in `android/`.

### 🛠️ Building the Android App
Prerequisites: JDK 17+ (or Android Studio JBR) and Android SDK (API 36).

```bash
cd android

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Assemble Release APK (R8 minified & shrink-wrapped)
./gradlew assembleRelease
```

---

## 🌐 Progressive Web App (PWA)

No build tools or installation needed!

```bash
# Clone the repository
git clone https://github.com/cyberlog69/pass-gen.git

# Open index.html in your browser
start index.html      # Windows
open index.html       # macOS
xdg-open index.html   # Linux
```

- **Installable**: Full `manifest.json` for "Add to Home Screen" across desktop and mobile.
- **Cache-First Service Worker (`sw.js`)**: Offline caching with zero network dependencies.
- **Preloaded Local Fonts**: `Inter.woff2` and `JetBrainsMono.woff2` bundled locally.

---

## 📁 Project Structure

```
pass-gen/
├── release/                 # Pre-built release APKs and SHA-256 checksums
│   ├── PassGen-v1.2.0.apk
│   └── PassGen-v1.2.0-checksums.txt
├── android/                 # Native Android App (Kotlin + Jetpack Compose)
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── AndroidManifest.xml   # Zero-network permission manifest
│   │   │   ├── java/com/cyberlog69/passgen/
│   │   │   │   ├── domain/           # CSPRNG, VaultCrypto, LeakedChecker, Models, QR Engine
│   │   │   │   ├── ui/               # Compose Screens, ViewModels, Dialogs, Theme
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                  # Adaptive icons, Material You monochrome, themes
│   │   ├── src/test/                 # JVM unit tests (SecureRandom, VaultCrypto, Leaks)
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── index.html               # Web PWA markup, CSP, & vault modal
├── manifest.json            # Web App Manifest for PWA installation
├── sw.js                    # Service Worker for offline caching
├── icon.svg                 # PWA App Icon & Favicon
├── css/
│   ├── styles.css           # Glassmorphism, vault cards, responsive layout
│   └── fonts.css            # Local @font-face rules
├── fonts/                   # Bundled fonts (Inter, JetBrains Mono)
├── js/
│   ├── vault.js             # WebCrypto AES-256-GCM & SHA-256 vault engine (Export/Import)
│   ├── leaks.js             # Offline breached password & common pattern detector
│   ├── qrcode.js            # Zero-dependency offline SVG QR code generator
│   ├── words.js             # Offline EFF wordlist for passphrase mode
│   └── app.js               # Web CSPRNG, deduplication, & UI controller
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
