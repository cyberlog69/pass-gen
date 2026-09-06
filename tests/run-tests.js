// Node.js test runner for CI and local CLI testing
const assert = require('assert');
const { webcrypto } = require('crypto');

// Polyfill window, crypto, TextEncoder in node environment
global.window = global;
global.crypto = webcrypto;

// Load words.js
require('../js/words.js');

// Load qrcode.js
require('../js/qrcode.js');

// Load vault.js
require('../js/vault.js');

// Minimal DOM mocks to load app.js in Node
const dummyEl = {
  classList: { contains: () => true, add: () => {}, remove: () => {}, toggle: () => {} },
  setAttribute: () => {},
  removeAttribute: () => {},
  getAttribute: () => 'violet',
  addEventListener: () => {},
  removeEventListener: () => {},
  appendChild: () => {},
  removeChild: () => {},
  append: () => {},
  querySelector: () => dummyEl,
  querySelectorAll: () => [],
  closest: () => null,
  focus: () => {},
  value: '16',
  min: '4',
  max: '128',
  hidden: false,
  style: {},
  textContent: ''
};

global.document = {
  getElementById: () => dummyEl,
  querySelector: () => dummyEl,
  querySelectorAll: () => [dummyEl],
  createElement: () => ({ ...dummyEl }),
  createElementNS: () => ({ ...dummyEl }),
  addEventListener: () => {},
  removeEventListener: () => {},
  documentElement: dummyEl
};
global.localStorage = {
  getItem: () => null,
  setItem: () => {}
};
global.navigator = {
  clipboard: { writeText: async () => {}, readText: async () => '' }
};

// Enable test export flag
global.__PASSGEN_TEST__ = true;
require('../js/app.js');

const PassGen = global.PassGen;
const QRCode = global.QRCode;
const VaultCrypto = global.VaultCrypto;

let passed = 0;
let failed = 0;

async function runTest(name, fn) {
  try {
    const res = fn();
    if (res && typeof res.then === 'function') {
      await res;
    }
    console.log(`  ✓ ${name}`);
    passed++;
  } catch (err) {
    console.error(`  ✗ ${name}`);
    console.error(`    ${err.message}`);
    failed++;
  }
}

async function main() {
  console.log('Running PassGen Automated CI Test Suite...\n');

  await runTest('EFF Wordlist loaded with 500+ words', () => {
    assert(Array.isArray(global.EFF_WORDS), 'EFF_WORDS should be an array');
    assert(global.EFF_WORDS.length > 500, 'EFF_WORDS should have >500 words');
    assert(Object.isFrozen(global.EFF_WORDS), 'EFF_WORDS should be frozen');
  });

  await runTest('secureRandom produces numbers within bounds [0, max)', () => {
    for (let max = 1; max <= 50; max++) {
      for (let i = 0; i < 20; i++) {
        const val = PassGen.secureRandom(max);
        assert(val >= 0 && val < max, `val ${val} out of bounds for max ${max}`);
      }
    }
  });

  await runTest('PIN generator produces exact length and digits only', () => {
    for (const len of [4, 6, 8, 12, 16]) {
      const pin = PassGen.generatePIN(len);
      assert.strictEqual(pin.length, len, `PIN length should be ${len}`);
      assert(/^\d+$/.test(pin), `PIN "${pin}" should contain digits only`);
    }
  });

  await runTest('Standard password includes required character sets', () => {
    const activeSets = [PassGen.CHARS.upper, PassGen.CHARS.lower, PassGen.CHARS.nums, PassGen.CHARS.symbols];
    const pwd = PassGen.generateStandardPassword(20, activeSets);
    assert.strictEqual(pwd.length, 20);
    assert(/[A-Z]/.test(pwd), 'Should contain uppercase');
    assert(/[a-z]/.test(pwd), 'Should contain lowercase');
    assert(/[0-9]/.test(pwd), 'Should contain digits');
  });

  await runTest('Passphrase generation respects word count and separators', () => {
    const phrase = PassGen.generatePassphrase(5, '.', true, false);
    const words = phrase.split('.');
    assert.strictEqual(words.length, 5, 'Should have 5 words');
    for (const w of words) {
      assert(/^[A-Z][a-z]+$/.test(w), `Word "${w}" should be capitalized`);
    }
  });

  await runTest('Custom symbols parameter overrides default symbols', () => {
    const toggleStubs = [
      { classList: { contains: (c) => c === 'active' } }, // upper
      { classList: { contains: () => false } },            // lower
      { classList: { contains: () => false } },            // nums
      { classList: { contains: (c) => c === 'active' } }  // symbols
    ];
    const charsets = PassGen.getActiveCharsets(toggleStubs, false, '#@$');
    assert.strictEqual(charsets.length, 2);
    assert.strictEqual(charsets[1], '#@$', 'Symbols set should match custom symbols');
  });

  await runTest('Relative time formatter produces human readable strings', () => {
    const now = Date.now();
    assert.strictEqual(PassGen.getRelativeTime(now), 'Just now');
    assert.strictEqual(PassGen.getRelativeTime(now - 45000), '45s ago');
    assert.strictEqual(PassGen.getRelativeTime(now - 120000), '2m ago');
    assert.strictEqual(PassGen.getRelativeTime(now - 7200000), '2h ago');
  });

  await runTest('Strength guidance returns appropriate feedback for modes', () => {
    const gShort = PassGen.getStrengthGuidance('password', 25, { len: 6, hasSymbols: false });
    assert.strictEqual(gShort.status, 'alert');

    const gPin = PassGen.getStrengthGuidance('pin', 20, { len: 6 });
    assert.strictEqual(gPin.status, 'warn');

    const gPass = PassGen.getStrengthGuidance('passphrase', 90, { wordCount: 6 });
    assert.strictEqual(gPass.status, 'good');
  });

  await runTest('QR Code generator produces valid matrix dimensions', () => {
    const matrixObj = QRCode.generateQRCodeMatrix('TestPassword123');
    assert(matrixObj.size >= 21, 'QR matrix should have size >= 21');
    assert.strictEqual(matrixObj.matrix.length, matrixObj.size);
    assert.strictEqual(matrixObj.matrix[0].length, matrixObj.size);
  });

  await runTest('VaultCrypto produces deterministic SHA-256 hashes for deduplication', async () => {
    const pepper = VaultCrypto.getOrCreateDevicePepper();
    assert(pepper && pepper.length === 64, 'Pepper should be 64 hex characters (256-bit)');
    const h1 = await VaultCrypto.hashPassword(pepper, 'secret_pass_1');
    const h2 = await VaultCrypto.hashPassword(pepper, 'secret_pass_1');
    const h3 = await VaultCrypto.hashPassword(pepper, 'secret_pass_2');
    assert.strictEqual(h1, h2, 'Identical passwords should hash identically');
    assert.notStrictEqual(h1, h3, 'Different passwords should have different hashes');
  });

  await runTest('VaultCrypto encrypts and decrypts items via AES-256-GCM', async () => {
    const items = [{ id: '1', title: 'Work WiFi', password: 'wificode99', createdAt: 1000 }];
    const passcode = 'MySecureMasterPasscode123!';
    const bundle = await VaultCrypto.encryptVault(items, passcode);
    assert(bundle.salt && bundle.iv && bundle.ciphertext, 'Bundle should contain salt, iv, and ciphertext');

    const decrypted = await VaultCrypto.decryptVault(bundle, passcode);
    assert.strictEqual(decrypted.length, 1);
    assert.strictEqual(decrypted[0].password, 'wificode99');
  });

  await runTest('VaultCrypto rejects incorrect master passcode', async () => {
    const items = [{ id: '1', title: 'Bank', password: '1234', createdAt: 1000 }];
    const bundle = await VaultCrypto.encryptVault(items, 'correct_passcode');
    let threw = false;
    try {
      await VaultCrypto.decryptVault(bundle, 'wrong_passcode');
    } catch (err) {
      threw = true;
    }
    assert.strictEqual(threw, true, 'Decrypting with wrong passcode must throw');
  });

  console.log(`\nResults: ${passed} passed, ${failed} failed`);

  if (failed > 0) {
    process.exit(1);
  } else {
    console.log('All tests passed successfully!');
    process.exit(0);
  }
}

main();
