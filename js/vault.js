(function () {
  'use strict';

  // ── Cryptographic Utility for Salted Hash Deduplication & AES-256-GCM Vault ──
  const PBKDF2_ITERATIONS = 100000;
  const SALT_LENGTH = 16;   // 128-bit salt for PBKDF2
  const IV_LENGTH = 12;     // 96-bit IV for AES-GCM

  function getCrypto() {
    if (typeof window !== 'undefined' && window.crypto) return window.crypto;
    if (typeof global !== 'undefined' && global.crypto) return global.crypto;
    throw new Error('Web Crypto API not available');
  }

  function bufferToHex(buf) {
    const bytes = new Uint8Array(buf);
    let hex = '';
    for (let i = 0; i < bytes.length; i++) {
      hex += bytes[i].toString(16).padStart(2, '0');
    }
    return hex;
  }

  function hexToBuffer(hex) {
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < bytes.length; i++) {
      bytes[i] = parseInt(hex.substr(i * 2, 2), 16);
    }
    return bytes.buffer;
  }

  // ── 1. Device Pepper & Irreversible SHA-256 Hashing ──────────────────────
  function getOrCreateDevicePepper() {
    const storageKey = 'passgen_device_pepper';
    try {
      let pepper = localStorage.getItem(storageKey);
      if (!pepper || pepper.length !== 64) {
        const crypto = getCrypto();
        const rand = new Uint8Array(32);
        crypto.getRandomValues(rand);
        pepper = bufferToHex(rand);
        localStorage.setItem(storageKey, pepper);
      }
      return pepper;
    } catch (_) {
      // In-memory fallback if localStorage is restricted
      return '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef';
    }
  }

  async function hashPassword(pepper, password) {
    const crypto = getCrypto();
    const enc = new TextEncoder();
    const data = enc.encode(`${pepper}:${password}`);
    const digest = await crypto.subtle.digest('SHA-256', data);
    return bufferToHex(digest);
  }

  // ── 2. PBKDF2 Key Derivation ─────────────────────────────────────────────
  async function deriveVaultKey(passcode, saltBuffer) {
    const crypto = getCrypto();
    const enc = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
      'raw',
      enc.encode(passcode),
      { name: 'PBKDF2' },
      false,
      ['deriveKey']
    );

    return await crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt: saltBuffer,
        iterations: PBKDF2_ITERATIONS,
        hash: 'SHA-256'
      },
      keyMaterial,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }

  // ── 3. AES-256-GCM Vault Encryption & Decryption ─────────────────────────
  async function encryptVault(items, passcode) {
    const crypto = getCrypto();
    const enc = new TextEncoder();
    const salt = new Uint8Array(SALT_LENGTH);
    const iv = new Uint8Array(IV_LENGTH);
    crypto.getRandomValues(salt);
    crypto.getRandomValues(iv);

    const key = await deriveVaultKey(passcode, salt);
    const plainBytes = enc.encode(JSON.stringify(items));

    const cipherBuffer = await crypto.subtle.encrypt(
      { name: 'AES-GCM', iv: iv },
      key,
      plainBytes
    );

    return {
      version: 1,
      salt: bufferToHex(salt),
      iv: bufferToHex(iv),
      ciphertext: bufferToHex(cipherBuffer)
    };
  }

  function toBuffer(str) {
    if (typeof str !== 'string') return new Uint8Array(0).buffer;
    const clean = str.trim();
    if (/^[0-9a-fA-F]+$/.test(clean) && clean.length % 2 === 0) {
      return hexToBuffer(clean);
    }
    if (typeof Buffer !== 'undefined') {
      const b = Buffer.from(clean, 'base64');
      return b.buffer.slice(b.byteOffset, b.byteOffset + b.byteLength);
    }
    const bin = atob(clean);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) {
      bytes[i] = bin.charCodeAt(i);
    }
    return bytes.buffer;
  }

  async function decryptVault(bundle, passcode) {
    if (!bundle) throw new Error('Invalid vault bundle');
    const saltStr = bundle.salt || (bundle.kdf && bundle.kdf.salt);
    const ivStr = bundle.iv || (bundle.cipher && bundle.cipher.iv);
    const cipherStr = bundle.ciphertext;

    if (!saltStr || !ivStr || !cipherStr) {
      throw new Error('Invalid vault bundle: Missing cryptographic parameters');
    }

    const crypto = getCrypto();
    const salt = new Uint8Array(toBuffer(saltStr));
    const iv = new Uint8Array(toBuffer(ivStr));
    const ciphertext = toBuffer(cipherStr);

    const key = await deriveVaultKey(passcode, salt);

    try {
      const decryptedBuffer = await crypto.subtle.decrypt(
        { name: 'AES-GCM', iv: iv },
        key,
        ciphertext
      );
      const dec = new TextDecoder();
      return JSON.parse(dec.decode(decryptedBuffer));
    } catch (err) {
      throw new Error('Decryption failed: Incorrect passcode or corrupted vault');
    }
  }

  async function exportVaultBackup(items, passcode) {
    const bundle = await encryptVault(items, passcode);
    const payload = {
      format: 'passgen-vault-backup',
      version: 1,
      timestamp: Date.now(),
      kdf: {
        algorithm: 'PBKDF2-HMAC-SHA256',
        iterations: PBKDF2_ITERATIONS,
        salt: bundle.salt
      },
      cipher: {
        algorithm: 'AES-256-GCM',
        iv: bundle.iv,
        tagLength: 128
      },
      ciphertext: bundle.ciphertext,
      itemCount: Array.isArray(items) ? items.length : 0
    };
    return JSON.stringify(payload, null, 2);
  }

  async function importVaultBackup(jsonOrObj, passcode) {
    let parsed;
    if (typeof jsonOrObj === 'string') {
      try {
        parsed = JSON.parse(jsonOrObj);
      } catch (_) {
        throw new Error('Invalid backup file: Not valid JSON');
      }
    } else {
      parsed = jsonOrObj;
    }

    if (!parsed) {
      throw new Error('Backup content is empty');
    }

    const items = await decryptVault(parsed, passcode);
    if (!Array.isArray(items)) {
      throw new Error('Decrypted backup content is invalid');
    }
    return items;
  }

  const VaultCrypto = Object.freeze({
    getOrCreateDevicePepper,
    hashPassword,
    encryptVault,
    decryptVault,
    exportVaultBackup,
    importVaultBackup
  });

  if (typeof window !== 'undefined') {
    window.VaultCrypto = VaultCrypto;
  }
  if (typeof global !== 'undefined') {
    global.VaultCrypto = VaultCrypto;
  }
})();
