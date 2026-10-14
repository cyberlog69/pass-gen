(function () {
  'use strict';

  // ── Character Sets & Standards ──────────────────────────────
  const DEFAULT_SYMBOLS = '!@#$%^&*()-_=+[]{}|;:,.<>?';
  const CHARS = {
    upper:   'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    lower:   'abcdefghijklmnopqrstuvwxyz',
    nums:    '0123456789',
    symbols: DEFAULT_SYMBOLS
  };

  const AMBIGUOUS_CHARS = new Set(['l','1','I','O','0','o','Q','S','5','Z','2']);
  const ALLOWED_THEMES = Object.freeze(['violet', 'emerald', 'sapphire', 'cyberpunk', 'light']);
  const ALLOWED_SEPARATORS = Object.freeze(['hyphen', 'space', 'period', 'underscore']);
  const SEP_MAP = Object.freeze({
    hyphen: '-',
    space: ' ',
    period: '.',
    underscore: '_'
  });

  // ── CSPRNG Helpers ──────────────────────────────────────────
  function secureRandom(max) {
    if (max <= 0) return 0;
    const limit = Math.floor(0xFFFFFFFF / max) * max;
    let val;
    do {
      const buf = new Uint32Array(1);
      crypto.getRandomValues(buf);
      val = buf[0];
    } while (val >= limit);
    return val % max;
  }

  function shuffle(arr) {
    for (let i = arr.length - 1; i > 0; i--) {
      const j = secureRandom(i + 1);
      [arr[i], arr[j]] = [arr[j], arr[i]];
    }
    return arr;
  }

  function triggerHaptic() {
    if ('vibrate' in navigator) {
      try { navigator.vibrate(30); } catch (_) {}
    }
  }

  function debounce(fn, delay) {
    let timer = null;
    return function (...args) {
      clearTimeout(timer);
      timer = setTimeout(() => fn.apply(this, args), delay);
    };
  }

  // ── Password Logic ──────────────────────────────────────────
  function filterAmbiguous(str) {
    let res = '';
    for (let i = 0; i < str.length; i++) {
      if (!AMBIGUOUS_CHARS.has(str[i])) res += str[i];
    }
    return res;
  }

  function getActiveCharsets(toggleEls, excludeAmbiguous, customSymbols) {
    const keys = ['upper', 'lower', 'nums', 'symbols'];
    return toggleEls.map((el, i) => {
      if (!el.classList.contains('active')) return null;
      let set = (keys[i] === 'symbols')
        ? (typeof customSymbols === 'string' && customSymbols.trim() ? customSymbols : CHARS.symbols)
        : CHARS[keys[i]];

      if (excludeAmbiguous) {
        set = filterAmbiguous(set);
      }
      return set;
    }).filter(Boolean);
  }

  function buildCharset(activeSets) {
    return activeSets.join('');
  }

  function generateStandardPassword(len, activeSets) {
    if (activeSets.length === 0 || len <= 0) return '';
    // Guarantee at least 1 character per active set
    const guaranteed = activeSets.map(set => set[secureRandom(set.length)]);
    const all = buildCharset(activeSets);
    const rest = Array.from({ length: Math.max(0, len - guaranteed.length) }, () => all[secureRandom(all.length)]);
    return shuffle([...guaranteed, ...rest]).join('');
  }

  // ── Passphrase Logic ────────────────────────────────────────
  function generatePassphrase(wordCount, separator, capitalize, includeNumber) {
    const wordsList = window.EFF_WORDS || ['correct', 'horse', 'battery', 'staple', 'secure', 'random'];
    let words = Array.from({ length: wordCount }, () => {
      const w = wordsList[secureRandom(wordsList.length)];
      return capitalize ? w.charAt(0).toUpperCase() + w.slice(1) : w;
    });

    if (includeNumber) {
      const idx = secureRandom(words.length);
      words[idx] += secureRandom(100);
    }

    return words.join(separator);
  }

  // ── PIN Logic ───────────────────────────────────────────────
  function generatePIN(len) {
    if (len <= 0) return '';
    const digits = CHARS.nums;
    return Array.from({ length: len }, () => digits[secureRandom(digits.length)]).join('');
  }

  // ── Entropy & Crack Time Calculations ──────────────────────
  function calcEntropyBits(mode, options) {
    if (mode === 'password') {
      const { len, activeSets } = options;
      const poolSize = buildCharset(activeSets).length;
      if (poolSize === 0 || len <= 0) return 0;
      return Math.floor(len * Math.log2(poolSize));
    } else if (mode === 'passphrase') {
      const { wordCount, includeNumber } = options;
      const poolSize = (window.EFF_WORDS || []).length || 1000;
      let bits = wordCount * Math.log2(poolSize);
      if (includeNumber) bits += Math.log2(100);
      return Math.floor(bits);
    } else {
      // PIN mode: pool of 10 digits
      const { len } = options;
      if (len <= 0) return 0;
      return Math.floor(len * Math.log2(10));
    }
  }

  function estimateCrackTime(bits) {
    if (bits <= 0) return 'Instant';
    const combinations = Math.pow(2, bits);
    const seconds = combinations / 1e11;

    if (seconds < 1) return 'Instant';
    if (seconds < 60) return `${Math.round(seconds)} seconds`;
    if (seconds < 3600) return `${Math.round(seconds / 60)} minutes`;
    if (seconds < 86400) return `${Math.round(seconds / 3600)} hours`;
    if (seconds < 31536000) return `${Math.round(seconds / 86400)} days`;
    if (seconds < 31536000 * 100) return `${Math.round(seconds / 31536000)} years`;
    if (seconds < 31536000 * 1e6) return `${(seconds / (31536000 * 1e3)).toFixed(1)}k years`;
    return `${(seconds / (31536000 * 1e6)).toFixed(1)}M years`;
  }

  function getStrengthGuidance(mode, bits, options = {}) {
    if (mode === 'pin') {
      const len = options.len || 6;
      if (len < 6) return { status: 'alert', icon: '⚠️', text: 'Short PIN (10,000 combinations). Enable rate-limiting on devices.' };
      if (len >= 8) return { status: 'good', icon: '🛡️', text: 'High security PIN with over 100 million combinations.' };
      return { status: 'warn', icon: '💡', text: 'Standard 6-digit PIN. Good balance for screen locks & cards.' };
    }

    if (mode === 'passphrase') {
      const wc = options.wordCount || 4;
      if (wc < 4) return { status: 'warn', icon: '💡', text: 'Tip: 4 or more words provide high resistance to dictionary attacks.' };
      if (wc >= 6) return { status: 'good', icon: '🔒', text: 'Massive entropy passphrase! Highly memorable and virtually uncrackable.' };
      return { status: 'good', icon: '🛡️', text: 'Strong passphrase. Easy to type, hard to crack.' };
    }

    // Password mode
    const len = options.len || 16;
    if (len < 8) return { status: 'alert', icon: '⚠️', text: 'Extremely short! Use 12–16+ characters for secure accounts.' };
    if (!options.hasSymbols) return { status: 'warn', icon: '💡', text: 'Tip: Enabling symbols substantially increases brute-force complexity.' };
    if (bits < 40) return { status: 'alert', icon: '⚠️', text: 'Weak password. Increase length and enable more character sets.' };
    if (bits < 60) return { status: 'warn', icon: '💡', text: 'Fair password. Suitable for basic accounts, but 16+ chars recommended.' };
    if (bits >= 80) return { status: 'good', icon: '🔒', text: 'Excellent! Highly resistant to offline GPU cluster attacks.' };
    return { status: 'good', icon: '🛡️', text: 'Strong password with solid entropy.' };
  }

  const STRENGTH_LEVELS = [
    { label: 'Weak',        color: 'var(--weak)',   segs: 1 },
    { label: 'Fair',        color: 'var(--fair)',   segs: 2 },
    { label: 'Strong',      color: 'var(--strong)', segs: 3 },
    { label: 'Very Strong', color: 'var(--great)',  segs: 4 },
  ];

  // ── DOM References ──────────────────────────────────────────
  const tabPassword       = document.getElementById('tabPassword');
  const tabPassphrase     = document.getElementById('tabPassphrase');
  const tabPin            = document.getElementById('tabPin');
  const passwordOpts      = document.getElementById('passwordOptions');
  const passphraseOpts    = document.getElementById('passphraseOptions');
  const pinOpts           = document.getElementById('pinOptions');
  const sliderLabel       = document.getElementById('sliderLabel');

  const slider            = document.getElementById('lengthSlider');
  const lengthBox         = document.getElementById('lengthBox');
  const passBox           = document.getElementById('passBox');
  const passText          = document.getElementById('passwordText');
  const copyBtn           = document.getElementById('copyBtn');
  const copyIcon          = document.getElementById('copyIcon');
  const qrBtn             = document.getElementById('qrBtn');
  const genBtn            = document.getElementById('generateBtn');
  const bulkBtn           = document.getElementById('bulkBtn');
  const strengthEl        = document.getElementById('strengthText');
  const segs              = [1,2,3,4].map(n => document.getElementById('seg'+n));
  const entropyEl         = document.getElementById('entropyInfo');
  const crackTimeEl       = document.getElementById('crackTimeInfo');
  const strengthGuidanceEl= document.getElementById('strengthGuidance');
  const histWrap          = document.getElementById('historyWrap');
  const histList          = document.getElementById('historyList');
  const clearHistBtn      = document.getElementById('clearHistBtn');
  const toastEl           = document.getElementById('toast');
  const trackEl           = document.querySelector('.strength-track');

  // Password Options
  const toggleIds         = ['tog-upper','tog-lower','tog-nums','tog-sym'];
  const toggleEls         = toggleIds.map(id => document.getElementById(id));
  const togAmbiguous      = document.getElementById('tog-ambiguous');
  const customSymbolsInput= document.getElementById('customSymbolsInput');
  const resetSymbolsBtn   = document.getElementById('resetSymbolsBtn');

  // Passphrase Options
  const selectSeparator   = document.getElementById('selectSeparator');
  const togCapitalize     = document.getElementById('tog-capitalize');
  const togIncludeNum     = document.getElementById('tog-passnum');

  // Bulk Modal
  const bulkModal         = document.getElementById('bulkModal');
  const bulkTitle         = document.getElementById('bulkTitle');
  const closeBulkBtn      = document.getElementById('closeBulkModal');
  const bulkCountSelect   = document.getElementById('bulkCount');
  const bulkTextarea      = document.getElementById('bulkTextarea');
  const copyBulkBtn       = document.getElementById('copyBulkBtn');
  const downloadBulkBtn   = document.getElementById('downloadBulkBtn');

  // QR Modal
  const qrModal           = document.getElementById('qrModal');
  const closeQrBtn        = document.getElementById('closeQrModal');
  const doneQrBtn         = document.getElementById('doneQrBtn');
  const qrContainer       = document.getElementById('qrContainer');

  // Vault Elements
  const openVaultBtn        = document.getElementById('openVaultBtn');
  const saveVaultBtn        = document.getElementById('saveVaultBtn');
  const vaultModal          = document.getElementById('vaultModal');
  const closeVaultModalBtn  = document.getElementById('closeVaultModal');
  const vaultSetupView      = document.getElementById('vaultSetupView');
  const vaultUnlockView     = document.getElementById('vaultUnlockView');
  const vaultContentView    = document.getElementById('vaultContentView');
  const setupPasscode       = document.getElementById('setupPasscode');
  const confirmPasscode     = document.getElementById('confirmPasscode');
  const btnCreateVault      = document.getElementById('btnCreateVault');
  const unlockPasscode      = document.getElementById('unlockPasscode');
  const btnUnlockVault      = document.getElementById('btnUnlockVault');
  const btnLockVault        = document.getElementById('btnLockVault');
  const vaultItemTitle      = document.getElementById('vaultItemTitle');
  const btnSaveCurrentToVault = document.getElementById('btnSaveCurrentToVault');
  const vaultItemCount      = document.getElementById('vaultItemCount');
  const vaultList           = document.getElementById('vaultList');
  const btnRestoreBackupSetup = document.getElementById('btnRestoreBackupSetup');
  const btnRestoreBackupUnlock = document.getElementById('btnRestoreBackupUnlock');
  const btnExportVault      = document.getElementById('btnExportVault');
  const btnImportVault      = document.getElementById('btnImportVault');
  const vaultFileInput      = document.getElementById('vaultFileInput');

  // State
  let currentMode = 'password'; // 'password', 'passphrase', or 'pin'
  const modeLengths = {
    password: 16,
    passphrase: 4,
    pin: 6
  };
  const modeOutputs = {
    password: null,
    passphrase: null,
    pin: null
  };
  const history = [];
  let toastTimer = null;
  let clipboardWipeTimer = null;
  let currentCopiedText = null;
  let lastActiveModalTrigger = null;

  // Deduplication & Vault State
  let devicePepper = (window.VaultCrypto && typeof window.VaultCrypto.getOrCreateDevicePepper === 'function')
    ? window.VaultCrypto.getOrCreateDevicePepper()
    : 'device_pepper_fallback_0123456789';
  let pastHashes = new Set();
  try {
    const rawHashes = localStorage.getItem('passgen_hashes');
    if (rawHashes) {
      const arr = JSON.parse(rawHashes);
      if (Array.isArray(arr)) pastHashes = new Set(arr);
    }
  } catch (_) {}

  let vaultUnlockedItems = null; // Array of items when unlocked, null when locked
  let vaultPasscode = null;      // Kept in volatile memory only while unlocked
  let vaultAutoLockTimer = null;
  let cleanupVaultFocus = null;

  // ── Mode Switcher ───────────────────────────────────────────
  function setMode(mode, skipSave = false) {
    currentMode = mode;

    tabPassword.classList.toggle('active', mode === 'password');
    tabPassword.setAttribute('aria-selected', (mode === 'password').toString());

    tabPassphrase.classList.toggle('active', mode === 'passphrase');
    tabPassphrase.setAttribute('aria-selected', (mode === 'passphrase').toString());

    tabPin.classList.toggle('active', mode === 'pin');
    tabPin.setAttribute('aria-selected', (mode === 'pin').toString());

    passwordOpts.hidden   = (mode !== 'password');
    passphraseOpts.hidden = (mode !== 'passphrase');
    pinOpts.hidden        = (mode !== 'pin');

    if (mode === 'password') {
      sliderLabel.textContent = 'Password Length';
      slider.min = 4; slider.max = 128;
      const val = Math.min(128, Math.max(4, modeLengths.password || 16));
      slider.value = val; lengthBox.value = val;
    } else if (mode === 'passphrase') {
      sliderLabel.textContent = 'Word Count';
      slider.min = 3; slider.max = 10;
      const val = Math.min(10, Math.max(3, modeLengths.passphrase || 4));
      slider.value = val; lengthBox.value = val;
    } else {
      sliderLabel.textContent = 'PIN Digits';
      slider.min = 4; slider.max = 16;
      const val = Math.min(16, Math.max(4, modeLengths.pin || 6));
      slider.value = val; lengthBox.value = val;
    }

    if (!skipSave) saveSettings();

    // Preserve generated output per mode without re-rolling or polluting history
    if (modeOutputs[mode]) {
      passText.textContent = modeOutputs[mode].password;
      updateStrengthUI(modeOutputs[mode].bits, modeOutputs[mode].guidance);
    } else {
      // First visit to this mode: generate initial password
      generate();
    }
  }

  // ── Settings Persistence ────────────────────────────────────
  function saveSettings() {
    try {
      const prefs = {
        mode: currentMode,
        length: parseInt(lengthBox.value, 10) || 16,
        lengths: modeLengths,
        toggles: {
          upper: toggleEls[0]?.classList.contains('active'),
          lower: toggleEls[1]?.classList.contains('active'),
          nums: toggleEls[2]?.classList.contains('active'),
          sym: toggleEls[3]?.classList.contains('active'),
          ambiguous: togAmbiguous?.classList.contains('active'),
          capitalize: togCapitalize?.classList.contains('active'),
          passnum: togIncludeNum?.classList.contains('active')
        },
        separator: selectSeparator?.value || 'hyphen',
        customSymbols: customSymbolsInput?.value || DEFAULT_SYMBOLS
      };
      localStorage.setItem('passgen_prefs', JSON.stringify(prefs));
    } catch (_) {}
  }

  function loadSettings() {
    try {
      const raw = localStorage.getItem('passgen_prefs');
      if (!raw) return;
      const prefs = JSON.parse(raw);

      if (prefs.toggles) {
        if (typeof prefs.toggles.upper === 'boolean') toggleEls[0]?.classList.toggle('active', prefs.toggles.upper);
        if (typeof prefs.toggles.lower === 'boolean') toggleEls[1]?.classList.toggle('active', prefs.toggles.lower);
        if (typeof prefs.toggles.nums === 'boolean') toggleEls[2]?.classList.toggle('active', prefs.toggles.nums);
        if (typeof prefs.toggles.sym === 'boolean') toggleEls[3]?.classList.toggle('active', prefs.toggles.sym);
        if (typeof prefs.toggles.ambiguous === 'boolean') togAmbiguous?.classList.toggle('active', prefs.toggles.ambiguous);
        if (typeof prefs.toggles.capitalize === 'boolean') togCapitalize?.classList.toggle('active', prefs.toggles.capitalize);
        if (typeof prefs.toggles.passnum === 'boolean') togIncludeNum?.classList.toggle('active', prefs.toggles.passnum);
      }

      if (prefs.separator && ALLOWED_SEPARATORS.includes(prefs.separator)) {
        selectSeparator.value = prefs.separator;
      }

      if (prefs.customSymbols && typeof prefs.customSymbols === 'string') {
        customSymbolsInput.value = prefs.customSymbols;
      }

      if (prefs.lengths && typeof prefs.lengths === 'object') {
        if (Number.isInteger(prefs.lengths.password)) modeLengths.password = Math.min(128, Math.max(4, prefs.lengths.password));
        if (Number.isInteger(prefs.lengths.passphrase)) modeLengths.passphrase = Math.min(10, Math.max(3, prefs.lengths.passphrase));
        if (Number.isInteger(prefs.lengths.pin)) modeLengths.pin = Math.min(16, Math.max(4, prefs.lengths.pin));
      } else if (prefs.length) {
        modeLengths.password = Math.min(128, Math.max(4, prefs.length));
      }

      if (prefs.mode && ['password', 'passphrase', 'pin'].includes(prefs.mode)) {
        currentMode = prefs.mode;
      }
    } catch (_) {}
  }

  // ── Generator Dispatch ──────────────────────────────────────
  async function generate() {
    triggerHaptic();
    let password = '';
    let bits = 0;
    let guidanceInfo = {};
    let candidateHash = '';

    // Rejection sampling against past hashes (up to 10 attempts to prevent collisions)
    for (let attempt = 0; attempt < 10; attempt++) {
      if (currentMode === 'password') {
        const rawLen = parseInt(lengthBox.value, 10);
        const len = (isNaN(rawLen) ? 16 : Math.min(128, Math.max(4, rawLen)));
        modeLengths.password = len;
        const excludeAmbiguous = togAmbiguous.classList.contains('active');
        const customSym = customSymbolsInput.value || DEFAULT_SYMBOLS;
        const activeSets = getActiveCharsets(toggleEls, excludeAmbiguous, customSym);

        if (activeSets.length === 0) {
          showToast('Enable at least one character set', 'error');
          passText.textContent = '';
          return;
        }
        password = generateStandardPassword(len, activeSets);
        bits = calcEntropyBits('password', { len, activeSets });
        guidanceInfo = getStrengthGuidance('password', bits, {
          len,
          hasSymbols: toggleEls[3]?.classList.contains('active')
        });
      } else if (currentMode === 'passphrase') {
        const rawCount = parseInt(lengthBox.value, 10);
        const wordCount = (isNaN(rawCount) ? 4 : Math.min(10, Math.max(3, rawCount)));
        modeLengths.passphrase = wordCount;
        const rawSep = selectSeparator.value;
        const safeSep = ALLOWED_SEPARATORS.includes(rawSep) ? rawSep : 'hyphen';
        const separator = SEP_MAP[safeSep];
        const capitalize = togCapitalize.classList.contains('active');
        const includeNumber = togIncludeNum.classList.contains('active');

        password = generatePassphrase(wordCount, separator, capitalize, includeNumber);
        bits = calcEntropyBits('passphrase', { wordCount, includeNumber });
        guidanceInfo = getStrengthGuidance('passphrase', bits, { wordCount });
      } else {
        // PIN Mode
        const rawLen = parseInt(lengthBox.value, 10);
        const len = (isNaN(rawLen) ? 6 : Math.min(16, Math.max(4, rawLen)));
        modeLengths.pin = len;

        password = generatePIN(len);
        bits = calcEntropyBits('pin', { len });
        guidanceInfo = getStrengthGuidance('pin', bits, { len });
      }

      // Check SHA-256 collision with past hashes
      if (window.VaultCrypto && typeof window.VaultCrypto.hashPassword === 'function') {
        try {
          candidateHash = await window.VaultCrypto.hashPassword(devicePepper, password);
          if (!pastHashes.has(candidateHash)) {
            pastHashes.add(candidateHash);
            try {
              const arr = Array.from(pastHashes).slice(-2000);
              localStorage.setItem('passgen_hashes', JSON.stringify(arr));
            } catch (_) {}
            break;
          }
          // Collision detected: continue loop to re-roll
        } catch (_) {
          break;
        }
      } else {
        break;
      }
    }

    // Check offline leaked / compromised password database
    if (window.LeakedPasswordChecker && password) {
      const leakCheck = window.LeakedPasswordChecker.checkLeakedPassword(password);
      if (leakCheck && leakCheck.isLeaked) {
        guidanceInfo = {
          status: 'alert',
          icon: '⚠️',
          text: 'Compromised! ' + (leakCheck.reason || 'Found in public breach digests. Regenerate recommended.')
        };
        bits = Math.min(bits, 20);
      }
    }

    modeOutputs[currentMode] = {
      password,
      bits,
      guidance: guidanceInfo
    };

    passText.textContent = password;
    updateStrengthUI(bits, guidanceInfo);
    addToHistory(password);

    // Rotate icon animation
    const icon = genBtn.querySelector('svg');
    if (icon) {
      icon.style.transform = 'rotate(360deg)';
      setTimeout(() => { icon.style.transform = ''; }, 500);
    }
  }

  function updateStrengthUI(bits, guidance) {
    let level;
    if      (bits < 40) level = 0;
    else if (bits < 60) level = 1;
    else if (bits < 80) level = 2;
    else                level = 3;

    const { label, color, segs: fill } = STRENGTH_LEVELS[level];

    segs.forEach((seg, i) => {
      seg.style.background = i < fill ? color : 'rgba(255,255,255,0.07)';
    });

    strengthEl.textContent = label;
    strengthEl.style.color = color;
    trackEl.setAttribute('aria-valuenow', fill);
    entropyEl.textContent = `~${bits} bits entropy`;
    crackTimeEl.textContent = `Time to crack: ${estimateCrackTime(bits)}`;

    if (guidance) {
      strengthGuidanceEl.className = `strength-guidance ${guidance.status}`;
      strengthGuidanceEl.innerHTML = '';
      const iconSpan = document.createElement('span');
      iconSpan.className = 'guidance-icon';
      iconSpan.textContent = guidance.icon;
      const textSpan = document.createElement('span');
      textSpan.className = 'guidance-text';
      textSpan.textContent = guidance.text;
      strengthGuidanceEl.append(iconSpan, textSpan);
    }
  }

  // ── Relative Time Formatter ─────────────────────────────────
  function getRelativeTime(timestamp) {
    const diff = Math.floor((Date.now() - timestamp) / 1000);
    if (diff < 30) return 'Just now';
    if (diff < 60) return `${diff}s ago`;
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    return `${Math.floor(diff / 3600)}h ago`;
  }

  // ── History Management ──────────────────────────────────────
  function addToHistory(pass) {
    if (!pass || (history[0] && history[0].text === pass)) return;
    history.unshift({ text: pass, masked: true, timestamp: Date.now() });
    if (history.length > 5) history.pop();
    renderHistory();
  }

  function renderHistory() {
    histList.innerHTML = '';
    history.forEach((item) => {
      const li = document.createElement('li');
      li.className = 'hist-item';

      const span = document.createElement('span');
      span.className = `hist-pass ${item.masked ? 'masked' : ''}`;
      span.textContent = item.masked ? '••••••••••••' : item.text;

      const metaDiv = document.createElement('div');
      metaDiv.className = 'hist-meta';

      const timeSpan = document.createElement('span');
      timeSpan.className = 'hist-time';
      timeSpan.textContent = getRelativeTime(item.timestamp);
      metaDiv.appendChild(timeSpan);

      const actions = document.createElement('div');
      actions.className = 'hist-actions';

      const toggleEyeBtn = document.createElement('button');
      toggleEyeBtn.className = 'hist-btn';
      toggleEyeBtn.title = item.masked ? 'Reveal' : 'Mask';
      (() => {
        const NS = 'http://www.w3.org/2000/svg';
        const svg = document.createElementNS(NS, 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('width', '12');
        svg.setAttribute('height', '12');
        svg.setAttribute('fill', 'none');
        svg.setAttribute('stroke', 'currentColor');
        svg.setAttribute('stroke-width', '2');
        if (item.masked) {
          const path = document.createElementNS(NS, 'path');
          path.setAttribute('d', 'M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z');
          const circle = document.createElementNS(NS, 'circle');
          circle.setAttribute('cx', '12'); circle.setAttribute('cy', '12'); circle.setAttribute('r', '3');
          svg.append(path, circle);
        } else {
          const path = document.createElementNS(NS, 'path');
          path.setAttribute('d', 'M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24');
          const line = document.createElementNS(NS, 'line');
          line.setAttribute('x1', '1'); line.setAttribute('y1', '1');
          line.setAttribute('x2', '23'); line.setAttribute('y2', '23');
          svg.append(path, line);
        }
        toggleEyeBtn.appendChild(svg);
      })();
      toggleEyeBtn.addEventListener('click', () => {
        item.masked = !item.masked;
        renderHistory();
      });

      const copyHistBtn = document.createElement('button');
      copyHistBtn.className = 'hist-btn';
      copyHistBtn.textContent = 'Copy';
      copyHistBtn.addEventListener('click', () => copyText(item.text));

      actions.append(toggleEyeBtn, copyHistBtn);
      metaDiv.appendChild(actions);
      li.append(span, metaDiv);
      histList.appendChild(li);
    });

    histWrap.hidden = history.length === 0;
  }

  function clearHistory() {
    if (history.length === 0) return;
    history.length = 0;
    renderHistory();
    showToast('History cleared', 'info');
  }

  // ── Clipboard & Auto-wipe Timer ─────────────────────────────
  async function copyText(text) {
    if (!text) { showToast('Nothing to copy', 'error'); return; }
    triggerHaptic();
    try {
      await navigator.clipboard.writeText(text);
      startAutoWipe(text);
      showToast('Copied! Will auto-clear in 30s', 'success');
    } catch {
      // Secure fallback
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.setAttribute('aria-hidden', 'true');
      ta.setAttribute('tabindex', '-1');
      ta.style.cssText = 'position:fixed;top:-9999px;left:-9999px;width:1px;height:1px;opacity:0;pointer-events:none';
      try {
        document.body.appendChild(ta);
        ta.focus();
        ta.select();
        document.execCommand('copy');
        startAutoWipe(text);
        showToast('Copied! Will auto-clear in 30s', 'success');
      } finally {
        if (ta.parentNode) document.body.removeChild(ta);
      }
    }
  }

  function startAutoWipe(copiedText) {
    currentCopiedText = copiedText;
    clearTimeout(clipboardWipeTimer);
    clipboardWipeTimer = setTimeout(async () => {
      try {
        const readText = await navigator.clipboard.readText();
        if (readText === currentCopiedText) {
          await navigator.clipboard.writeText('');
          showToast('Clipboard auto-cleared for security', 'info');
        }
      } catch (_) {}
    }, 30000);
  }

  function setSvgIcon(svgEl, nodes) {
    while (svgEl.firstChild) svgEl.removeChild(svgEl.firstChild);
    const NS = 'http://www.w3.org/2000/svg';
    nodes.forEach(({ tag, attrs }) => {
      const el = document.createElementNS(NS, tag);
      Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
      svgEl.appendChild(el);
    });
  }

  function copyCurrentPassword() {
    const text = passText.textContent.trim();
    if (!text) { showToast('Generate an output first', 'error'); return; }

    copyBtn.classList.add('copied');
    setSvgIcon(copyIcon, [{ tag: 'polyline', attrs: { points: '20 6 9 17 4 12' } }]);
    copyText(text);

    setTimeout(() => {
      copyBtn.classList.remove('copied');
      setSvgIcon(copyIcon, [
        { tag: 'rect', attrs: { x: '9', y: '9', width: '13', height: '13', rx: '2' } },
        { tag: 'path', attrs: { d: 'M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1' } }
      ]);
    }, 2000);
  }

  function showToast(msg, type = 'success') {
    clearTimeout(toastTimer);
    toastEl.textContent = msg;
    toastEl.className = `toast show ${type}`;
    toastTimer = setTimeout(() => { toastEl.className = 'toast'; }, 3200);
  }

  // ── Modal Focus Trap ────────────────────────────────────────
  function trapFocus(modalEl, closeFn) {
    const focusable = modalEl.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    const first = focusable[0];
    const last = focusable[focusable.length - 1];

    if (first) first.focus();

    function handleKey(e) {
      if (e.key === 'Escape') {
        closeFn();
        return;
      }
      if (e.key === 'Tab') {
        if (e.shiftKey) {
          if (document.activeElement === first) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    }

    modalEl.addEventListener('keydown', handleKey);
    return () => modalEl.removeEventListener('keydown', handleKey);
  }

  // ── Bulk Modal ──────────────────────────────────────────────
  let cleanupBulkFocus = null;

  function openBulkModal() {
    lastActiveModalTrigger = bulkBtn;
    const titles = {
      password: 'Bulk Password Generator',
      passphrase: 'Bulk Passphrase Generator',
      pin: 'Bulk PIN Generator'
    };
    bulkTitle.textContent = titles[currentMode] || 'Bulk Generator';
    renderBulkPasswords();
    bulkModal.classList.add('open');
    cleanupBulkFocus = trapFocus(bulkModal, closeBulkModal);
  }

  function closeBulkModal() {
    bulkModal.classList.remove('open');
    if (cleanupBulkFocus) cleanupBulkFocus();
    if (lastActiveModalTrigger) lastActiveModalTrigger.focus();
  }

  function renderBulkPasswords() {
    const rawCount = parseInt(bulkCountSelect.value, 10);
    const count = (isNaN(rawCount) ? 10 : Math.min(50, Math.max(1, rawCount)));
    const items = [];

    for (let i = 0; i < count; i++) {
      if (currentMode === 'password') {
        const rawLen = parseInt(lengthBox.value, 10);
        const len = (isNaN(rawLen) ? 16 : Math.min(128, Math.max(4, rawLen)));
        const excludeAmbiguous = togAmbiguous.classList.contains('active');
        const customSym = customSymbolsInput.value || DEFAULT_SYMBOLS;
        const activeSets = getActiveCharsets(toggleEls, excludeAmbiguous, customSym);
        items.push(generateStandardPassword(len, activeSets));
      } else if (currentMode === 'passphrase') {
        const rawCount2 = parseInt(lengthBox.value, 10);
        const wordCount = (isNaN(rawCount2) ? 4 : Math.min(10, Math.max(3, rawCount2)));
        const rawSep = selectSeparator.value;
        const safeSep = ALLOWED_SEPARATORS.includes(rawSep) ? rawSep : 'hyphen';
        const separator = SEP_MAP[safeSep];
        const capitalize = togCapitalize.classList.contains('active');
        const includeNumber = togIncludeNum.classList.contains('active');
        items.push(generatePassphrase(wordCount, separator, capitalize, includeNumber));
      } else {
        const rawLen = parseInt(lengthBox.value, 10);
        const len = (isNaN(rawLen) ? 6 : Math.min(16, Math.max(4, rawLen)));
        items.push(generatePIN(len));
      }
    }

    bulkTextarea.value = items.join('\n');
  }

  function downloadBulkTxt() {
    const content = bulkTextarea.value;
    if (!content) return;
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const randHex = Array.from(crypto.getRandomValues(new Uint8Array(4)))
      .map(b => b.toString(16).padStart(2, '0')).join('');
    a.download = `${currentMode}_${randHex}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Downloaded TXT file', 'success');
  }

  // ── QR Code Modal ───────────────────────────────────────────
  let cleanupQrFocus = null;

  function openQrModal() {
    const text = passText.textContent.trim();
    if (!text) {
      showToast('Generate an output first', 'error');
      return;
    }
    lastActiveModalTrigger = qrBtn;
    if (window.QRCode && typeof window.QRCode.renderToElement === 'function') {
      window.QRCode.renderToElement(qrContainer, text, {
        fgColor: '#0a0a0f',
        bgColor: '#ffffff',
        margin: 2
      });
    }
    qrModal.classList.add('open');
    cleanupQrFocus = trapFocus(qrModal, closeQrModal);
  }

  function closeQrModal() {
    qrModal.classList.remove('open');
    if (cleanupQrFocus) cleanupQrFocus();
    if (lastActiveModalTrigger) lastActiveModalTrigger.focus();
  }

  // ── Encrypted Vault Management ──────────────────────────────
  function resetVaultAutoLock() {
    clearTimeout(vaultAutoLockTimer);
    vaultAutoLockTimer = setTimeout(() => {
      if (vaultUnlockedItems !== null) {
        lockVault();
        showToast('Vault auto-locked after inactivity', 'info');
      }
    }, 60000);
  }

  function openVaultModal() {
    lastActiveModalTrigger = openVaultBtn;
    resetVaultAutoLock();

    let storedBundle = null;
    try {
      const raw = localStorage.getItem('passgen_vault');
      if (raw) storedBundle = JSON.parse(raw);
    } catch (_) {}

    if (!storedBundle) {
      // First time: setup master passcode
      if (vaultSetupView) vaultSetupView.hidden = false;
      if (vaultUnlockView) vaultUnlockView.hidden = true;
      if (vaultContentView) vaultContentView.hidden = true;
      if (setupPasscode) setupPasscode.value = '';
      if (confirmPasscode) confirmPasscode.value = '';
    } else if (vaultUnlockedItems !== null) {
      // Already unlocked
      if (vaultSetupView) vaultSetupView.hidden = true;
      if (vaultUnlockView) vaultUnlockView.hidden = true;
      if (vaultContentView) vaultContentView.hidden = false;
      renderVaultList();
    } else {
      // Locked: prompt for unlock
      if (vaultSetupView) vaultSetupView.hidden = true;
      if (vaultUnlockView) vaultUnlockView.hidden = false;
      if (vaultContentView) vaultContentView.hidden = true;
      if (unlockPasscode) unlockPasscode.value = '';
    }

    if (vaultModal) {
      vaultModal.classList.add('open');
      cleanupVaultFocus = trapFocus(vaultModal, closeVaultModal);
    }
  }

  function closeVaultModal() {
    if (vaultModal) vaultModal.classList.remove('open');
    if (cleanupVaultFocus) cleanupVaultFocus();
    if (lastActiveModalTrigger) lastActiveModalTrigger.focus();
  }

  async function handleCreateVault() {
    const p1 = setupPasscode ? setupPasscode.value : '';
    const p2 = confirmPasscode ? confirmPasscode.value : '';
    if (!p1 || p1.length < 6) {
      showToast('Passcode must be at least 6 characters', 'error');
      return;
    }
    if (p1 !== p2) {
      showToast('Passcodes do not match', 'error');
      return;
    }

    try {
      const bundle = await window.VaultCrypto.encryptVault([], p1);
      localStorage.setItem('passgen_vault', JSON.stringify(bundle));
      vaultPasscode = p1;
      vaultUnlockedItems = [];
      if (setupPasscode) setupPasscode.value = '';
      if (confirmPasscode) confirmPasscode.value = '';
      showToast('Vault created & encrypted with AES-256-GCM!', 'success');

      if (vaultSetupView) vaultSetupView.hidden = true;
      if (vaultUnlockView) vaultUnlockView.hidden = true;
      if (vaultContentView) vaultContentView.hidden = false;
      renderVaultList();
      resetVaultAutoLock();
    } catch (err) {
      showToast('Error creating vault: ' + err.message, 'error');
    }
  }

  async function handleUnlockVault() {
    const p = unlockPasscode ? unlockPasscode.value : '';
    if (!p) {
      showToast('Enter your master passcode', 'error');
      return;
    }

    const raw = localStorage.getItem('passgen_vault');
    if (!raw) {
      showToast('No vault found', 'error');
      return;
    }

    try {
      const bundle = JSON.parse(raw);
      const items = await window.VaultCrypto.decryptVault(bundle, p);
      vaultUnlockedItems = items;
      vaultPasscode = p;
      if (unlockPasscode) unlockPasscode.value = '';
      showToast('Vault unlocked successfully!', 'success');

      if (vaultSetupView) vaultSetupView.hidden = true;
      if (vaultUnlockView) vaultUnlockView.hidden = true;
      if (vaultContentView) vaultContentView.hidden = false;
      renderVaultList();
      resetVaultAutoLock();
    } catch (_) {
      showToast('Incorrect passcode or corrupted vault', 'error');
    }
  }

  function lockVault() {
    vaultPasscode = null;
    vaultUnlockedItems = null;
    clearTimeout(vaultAutoLockTimer);
    if (vaultSetupView) vaultSetupView.hidden = true;
    if (vaultUnlockView) vaultUnlockView.hidden = false;
    if (vaultContentView) vaultContentView.hidden = true;
    if (unlockPasscode) unlockPasscode.value = '';
  }

  async function saveItemToVault(title, pass) {
    if (!pass) {
      showToast('Generate a password first', 'error');
      return;
    }

    const raw = localStorage.getItem('passgen_vault');
    if (!raw || vaultUnlockedItems === null) {
      openVaultModal();
      showToast('Unlock or create vault to save', 'info');
      return;
    }

    const item = {
      id: Date.now().toString(36) + Math.random().toString(36).substring(2, 6),
      title: title && title.trim() ? title.trim() : `Password (${new Date().toLocaleDateString()})`,
      password: pass,
      createdAt: Date.now()
    };

    vaultUnlockedItems.unshift(item);
    try {
      const bundle = await window.VaultCrypto.encryptVault(vaultUnlockedItems, vaultPasscode);
      localStorage.setItem('passgen_vault', JSON.stringify(bundle));
      renderVaultList();
      resetVaultAutoLock();
      showToast(`Saved "${item.title}" to encrypted vault!`, 'success');
    } catch (err) {
      showToast('Failed to save to vault: ' + err.message, 'error');
    }
  }

  async function deleteVaultItem(id) {
    if (!vaultUnlockedItems || !vaultPasscode) return;
    vaultUnlockedItems = vaultUnlockedItems.filter(i => i.id !== id);
    try {
      const bundle = await window.VaultCrypto.encryptVault(vaultUnlockedItems, vaultPasscode);
      localStorage.setItem('passgen_vault', JSON.stringify(bundle));
      renderVaultList();
      resetVaultAutoLock();
      showToast('Deleted item from vault', 'info');
    } catch (err) {
      showToast('Failed to update vault', 'error');
    }
  }

  function renderVaultList() {
    if (!vaultUnlockedItems || !vaultList) return;
    if (vaultItemCount) {
      vaultItemCount.textContent = `${vaultUnlockedItems.length} item${vaultUnlockedItems.length === 1 ? '' : 's'}`;
    }
    vaultList.innerHTML = '';

    if (vaultUnlockedItems.length === 0) {
      const emptyLi = document.createElement('li');
      emptyLi.style.cssText = 'text-align: center; color: var(--muted); padding: 20px; font-size: 13px;';
      emptyLi.textContent = 'No saved passwords yet.';
      vaultList.appendChild(emptyLi);
      return;
    }

    vaultUnlockedItems.forEach(item => {
      const li = document.createElement('li');
      li.className = 'vault-item';

      const infoDiv = document.createElement('div');
      infoDiv.className = 'vault-item-info';

      const titleSpan = document.createElement('span');
      titleSpan.className = 'vault-item-title';
      titleSpan.textContent = item.title;

      const passSpan = document.createElement('span');
      passSpan.className = 'vault-item-pass masked';
      passSpan.textContent = '••••••••••••';

      infoDiv.append(titleSpan, passSpan);

      const actionsDiv = document.createElement('div');
      actionsDiv.className = 'vault-item-actions';

      // Eye button
      const eyeBtn = document.createElement('button');
      eyeBtn.className = 'vault-btn';
      eyeBtn.title = 'Reveal / Mask';
      eyeBtn.innerHTML = '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>';
      eyeBtn.addEventListener('click', () => {
        const isMasked = passSpan.classList.toggle('masked');
        passSpan.textContent = isMasked ? '••••••••••••' : item.password;
        resetVaultAutoLock();
      });

      // Copy button
      const copyBtnItem = document.createElement('button');
      copyBtnItem.className = 'vault-btn';
      copyBtnItem.title = 'Copy';
      copyBtnItem.innerHTML = '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>';
      copyBtnItem.addEventListener('click', () => {
        copyText(item.password);
        resetVaultAutoLock();
      });

      // Delete button
      const delBtn = document.createElement('button');
      delBtn.className = 'vault-btn del-btn';
      delBtn.title = 'Delete';
      delBtn.innerHTML = '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>';
      delBtn.addEventListener('click', () => {
        deleteVaultItem(item.id);
      });

      actionsDiv.append(eyeBtn, copyBtnItem, delBtn);
      li.append(infoDiv, actionsDiv);
      vaultList.appendChild(li);
    });
  }

  // ── Vault Backup Export & Import ─────────────────────────────
  async function handleExportVault() {
    if (!vaultUnlockedItems || !vaultPasscode) {
      showToast('Unlock vault to export', 'error');
      return;
    }
    try {
      const backupJson = await window.VaultCrypto.exportVaultBackup(vaultUnlockedItems, vaultPasscode);
      const blob = new Blob([backupJson], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      const dateStr = new Date().toISOString().slice(0, 10);
      a.href = url;
      a.download = `passgen-vault-${dateStr}.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast('Vault exported securely (AES-256-GCM)', 'success');
      resetVaultAutoLock();
    } catch (err) {
      showToast('Export failed: ' + err.message, 'error');
    }
  }

  function handleImportVaultClick() {
    if (vaultFileInput) {
      vaultFileInput.value = '';
      vaultFileInput.click();
    }
  }

  async function handleImportVaultFile(file) {
    if (!file) return;
    const reader = new FileReader();
    reader.onload = async (e) => {
      const text = e.target.result;
      let passcode = vaultPasscode;
      if (!passcode) {
        passcode = prompt('Enter the Master Passcode used to encrypt this backup:');
        if (!passcode) return;
      }
      try {
        const importedItems = await window.VaultCrypto.importVaultBackup(text, passcode);
        if (!Array.isArray(importedItems)) {
          showToast('Invalid backup content', 'error');
          return;
        }

        let finalItems;
        if (vaultUnlockedItems && vaultUnlockedItems.length > 0) {
          const shouldMerge = confirm(`Imported ${importedItems.length} items.\nClick OK to MERGE with current ${vaultUnlockedItems.length} items, or CANCEL to REPLACE.`);
          if (shouldMerge) {
            const existingIds = new Set(vaultUnlockedItems.map(i => i.id));
            const newItems = importedItems.filter(i => !existingIds.has(i.id));
            finalItems = [...newItems, ...vaultUnlockedItems];
          } else {
            finalItems = importedItems;
          }
        } else {
          finalItems = importedItems;
        }

        vaultPasscode = passcode;
        vaultUnlockedItems = finalItems;
        const bundle = await window.VaultCrypto.encryptVault(finalItems, passcode);
        localStorage.setItem('passgen_vault', JSON.stringify(bundle));

        if (vaultSetupView) vaultSetupView.hidden = true;
        if (vaultUnlockView) vaultUnlockView.hidden = true;
        if (vaultContentView) vaultContentView.hidden = false;
        renderVaultList();
        resetVaultAutoLock();
        showToast(`Successfully restored ${importedItems.length} items!`, 'success');
      } catch (err) {
        showToast(err.message || 'Decryption failed: Incorrect passcode', 'error');
      }
    };
    reader.readAsText(file);
  }

  // ── Theme Switcher ──────────────────────────────────────────
  function initTheme() {
    let saved = localStorage.getItem('passgen_theme');
    if (!saved) {
      // Auto detect prefers-color-scheme
      if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
        saved = 'light';
      } else {
        saved = 'violet';
      }
    }
    const safeTheme = ALLOWED_THEMES.includes(saved) ? saved : 'violet';
    setTheme(safeTheme);
  }

  function setTheme(themeName) {
    if (!ALLOWED_THEMES.includes(themeName)) return;
    if (themeName === 'violet') {
      document.documentElement.removeAttribute('data-theme');
    } else {
      document.documentElement.setAttribute('data-theme', themeName);
    }
    localStorage.setItem('passgen_theme', themeName);

    document.querySelectorAll('.theme-dot').forEach(dot => {
      dot.classList.toggle('active', dot.getAttribute('data-set-theme') === themeName);
    });
  }

  // ── Event Listeners Setup ───────────────────────────────────
  tabPassword.addEventListener('click', () => setMode('password'));
  tabPassphrase.addEventListener('click', () => setMode('passphrase'));
  tabPin.addEventListener('click', () => setMode('pin'));

  function updateLength(v) {
    if (currentMode === 'password') modeLengths.password = v;
    else if (currentMode === 'passphrase') modeLengths.passphrase = v;
    else modeLengths.pin = v;
    saveSettings();
  }

  slider.addEventListener('input', () => {
    lengthBox.value = slider.value;
    updateLength(parseInt(slider.value, 10));
  });

  lengthBox.addEventListener('input', () => {
    const v = parseInt(lengthBox.value, 10);
    const max = currentMode === 'password' ? 128 : (currentMode === 'pin' ? 16 : 10);
    const min = currentMode === 'password' ? 4 : (currentMode === 'pin' ? 4 : 3);
    if (!isNaN(v) && v >= min && v <= max) {
      slider.value = v;
      updateLength(v);
    }
  });

  // Custom Symbols Input & Reset
  customSymbolsInput.addEventListener('input', () => {
    saveSettings();
  });
  resetSymbolsBtn.addEventListener('click', () => {
    customSymbolsInput.value = DEFAULT_SYMBOLS;
    saveSettings();
  });

  // Toggles
  [...toggleEls, togAmbiguous, togCapitalize, togIncludeNum].forEach(el => {
    if (!el) return;
    function toggle() {
      const active = el.classList.toggle('active');
      el.setAttribute('aria-checked', active.toString());
      saveSettings();
    }
    el.addEventListener('click', toggle);
    el.addEventListener('keydown', e => {
      if (e.key === ' ' || e.key === 'Enter') {
        e.preventDefault();
        toggle();
      }
    });
  });

  selectSeparator.addEventListener('change', () => {
    saveSettings();
  });

  // One-click regeneration on clicking the password output box
  passBox.addEventListener('click', (e) => {
    if (e.target.closest('.pass-actions')) return; // ignore clicks on copy or QR button
    generate();
  });
  passBox.addEventListener('keydown', (e) => {
    if (e.key === ' ' || e.key === 'Enter') {
      if (!e.target.closest('.pass-actions')) {
        e.preventDefault();
        generate();
      }
    }
  });

  genBtn.addEventListener('click', generate);
  copyBtn.addEventListener('click', copyCurrentPassword);
  qrBtn.addEventListener('click', openQrModal);
  clearHistBtn.addEventListener('click', clearHistory);

  // Bulk modal events
  bulkBtn.addEventListener('click', openBulkModal);
  closeBulkBtn.addEventListener('click', closeBulkModal);
  bulkModal.addEventListener('click', e => { if (e.target === bulkModal) closeBulkModal(); });
  bulkCountSelect.addEventListener('change', renderBulkPasswords);
  copyBulkBtn.addEventListener('click', () => copyText(bulkTextarea.value));
  downloadBulkBtn.addEventListener('click', downloadBulkTxt);

  // QR modal events
  closeQrBtn.addEventListener('click', closeQrModal);
  doneQrBtn.addEventListener('click', closeQrModal);
  qrModal.addEventListener('click', e => { if (e.target === qrModal) closeQrModal(); });

  // Vault modal events
  if (openVaultBtn) openVaultBtn.addEventListener('click', openVaultModal);
  if (saveVaultBtn) saveVaultBtn.addEventListener('click', () => saveItemToVault('', passText.textContent.trim()));
  if (closeVaultModalBtn) closeVaultModalBtn.addEventListener('click', closeVaultModal);
  if (vaultModal) {
    vaultModal.addEventListener('click', e => { if (e.target === vaultModal) closeVaultModal(); });
    vaultModal.addEventListener('mousemove', resetVaultAutoLock);
    vaultModal.addEventListener('keydown', resetVaultAutoLock);
  }
  if (btnCreateVault) btnCreateVault.addEventListener('click', handleCreateVault);
  if (btnUnlockVault) btnUnlockVault.addEventListener('click', handleUnlockVault);
  if (btnLockVault) btnLockVault.addEventListener('click', () => { lockVault(); showToast('Vault locked', 'info'); });
  if (btnSaveCurrentToVault) {
    btnSaveCurrentToVault.addEventListener('click', () => {
      saveItemToVault(vaultItemTitle.value, passText.textContent.trim());
      if (vaultItemTitle) vaultItemTitle.value = '';
    });
  }
  if (setupPasscode) setupPasscode.addEventListener('keydown', e => { if (e.key === 'Enter') handleCreateVault(); });
  if (confirmPasscode) confirmPasscode.addEventListener('keydown', e => { if (e.key === 'Enter') handleCreateVault(); });
  if (unlockPasscode) unlockPasscode.addEventListener('keydown', e => { if (e.key === 'Enter') handleUnlockVault(); });

  // Backup & Restore
  if (btnExportVault) btnExportVault.addEventListener('click', handleExportVault);
  if (btnImportVault) btnImportVault.addEventListener('click', handleImportVaultClick);
  if (btnRestoreBackupSetup) btnRestoreBackupSetup.addEventListener('click', handleImportVaultClick);
  if (btnRestoreBackupUnlock) btnRestoreBackupUnlock.addEventListener('click', handleImportVaultClick);
  if (vaultFileInput) {
    vaultFileInput.addEventListener('change', (e) => {
      const file = e.target.files && e.target.files[0];
      if (file) handleImportVaultFile(file);
    });
  }

  // Themes
  document.querySelectorAll('.theme-dot').forEach(dot => {
    dot.addEventListener('click', () => {
      const theme = dot.getAttribute('data-set-theme');
      if (ALLOWED_THEMES.includes(theme)) setTheme(theme);
    });
  });

  // Global Keyboard shortcuts
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      generate();
    }
  });

  // Service Worker Registration
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').catch(() => {});
    });
  }

  // Periodic Relative Time refresh for history
  setInterval(renderHistory, 30000);

  // Initialization
  initTheme();
  loadSettings();
  setMode(currentMode, true);

  // Test Export
  if (typeof window.__PASSGEN_TEST__ !== 'undefined' && window.__PASSGEN_TEST__ === true) {
    window.PassGen = Object.freeze({
      secureRandom,
      shuffle,
      getActiveCharsets,
      buildCharset,
      generateStandardPassword,
      generatePassphrase,
      generatePIN,
      calcEntropyBits,
      estimateCrackTime,
      getStrengthGuidance,
      getRelativeTime,
      CHARS,
      DEFAULT_SYMBOLS,
      ALLOWED_THEMES,
      ALLOWED_SEPARATORS,
      SEP_MAP,
      STRENGTH_LEVELS
    });
  }
})();
