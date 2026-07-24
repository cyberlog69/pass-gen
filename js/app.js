(function () {
  'use strict';

  // ── Character Sets ──────────────────────────────────────────
  const CHARS = {
    upper:   'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    lower:   'abcdefghijklmnopqrstuvwxyz',
    nums:    '0123456789',
    symbols: '!@#$%^&*()-_=+[]{}|;:,.<>?'
  };

  const AMBIGUOUS = /[l1IO0oQS5Z2]/g;

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

  // ── Password Logic ──────────────────────────────────────────
  function getActiveCharsets(toggleEls, excludeAmbiguous) {
    const keys = ['upper','lower','nums','symbols'];
    return toggleEls.map((el, i) => {
      if (!el.classList.contains('active')) return null;
      let set = CHARS[keys[i]];
      if (excludeAmbiguous) {
        set = set.replace(AMBIGUOUS, '');
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

  // ── Entropy & Crack Time Calculations ──────────────────────
  function calcEntropyBits(mode, options) {
    if (mode === 'password') {
      const { len, activeSets } = options;
      const poolSize = buildCharset(activeSets).length;
      if (poolSize === 0 || len <= 0) return 0;
      return Math.floor(len * Math.log2(poolSize));
    } else {
      const { wordCount, includeNumber } = options;
      const poolSize = (window.EFF_WORDS || []).length || 1000;
      let bits = wordCount * Math.log2(poolSize);
      if (includeNumber) bits += Math.log2(100);
      return Math.floor(bits);
    }
  }

  function estimateCrackTime(bits) {
    // Assume 100 Billion (10^11) guesses per second (modern GPU cluster)
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

  const STRENGTH_LEVELS = [
    { label: 'Weak',        color: 'var(--weak)',   segs: 1 },
    { label: 'Fair',        color: 'var(--fair)',   segs: 2 },
    { label: 'Strong',      color: 'var(--strong)', segs: 3 },
    { label: 'Very Strong', color: 'var(--great)',  segs: 4 },
  ];

  // ── DOM References ──────────────────────────────────────────
  const tabPassword    = document.getElementById('tabPassword');
  const tabPassphrase  = document.getElementById('tabPassphrase');
  const passwordOpts   = document.getElementById('passwordOptions');
  const passphraseOpts = document.getElementById('passphraseOptions');
  const sliderLabel    = document.getElementById('sliderLabel');

  const slider         = document.getElementById('lengthSlider');
  const lengthBox      = document.getElementById('lengthBox');
  const passText       = document.getElementById('passwordText');
  const copyBtn        = document.getElementById('copyBtn');
  const copyIcon       = document.getElementById('copyIcon');
  const genBtn         = document.getElementById('generateBtn');
  const bulkBtn        = document.getElementById('bulkBtn');
  const strengthEl     = document.getElementById('strengthText');
  const segs           = [1,2,3,4].map(n => document.getElementById('seg'+n));
  const entropyEl      = document.getElementById('entropyInfo');
  const crackTimeEl    = document.getElementById('crackTimeInfo');
  const histWrap       = document.getElementById('historyWrap');
  const histList       = document.getElementById('historyList');
  const clearHistBtn   = document.getElementById('clearHistBtn');
  const toastEl        = document.getElementById('toast');
  const trackEl        = document.querySelector('.strength-track');

  // Toggles
  const toggleIds   = ['tog-upper','tog-lower','tog-nums','tog-sym'];
  const toggleEls   = toggleIds.map(id => document.getElementById(id));
  const togAmbiguous= document.getElementById('tog-ambiguous');

  // Passphrase selects
  const selectSeparator = document.getElementById('selectSeparator');
  const togCapitalize   = document.getElementById('tog-capitalize');
  const togIncludeNum   = document.getElementById('tog-passnum');

  // Bulk Modal
  const bulkModal       = document.getElementById('bulkModal');
  const closeBulkBtn    = document.getElementById('closeBulkModal');
  const bulkCountSelect = document.getElementById('bulkCount');
  const bulkTextarea    = document.getElementById('bulkTextarea');
  const copyBulkBtn     = document.getElementById('copyBulkBtn');
  const downloadBulkBtn = document.getElementById('downloadBulkBtn');

  // State
  let currentMode = 'password'; // 'password' or 'passphrase'
  const history = [];
  let toastTimer = null;
  let clipboardWipeTimer = null;
  let currentCopiedText = null;

  // ── Mode Switcher ───────────────────────────────────────────
  function setMode(mode) {
    currentMode = mode;
    if (mode === 'password') {
      tabPassword.classList.add('active');
      tabPassphrase.classList.remove('active');
      passwordOpts.hidden = false;
      passphraseOpts.hidden = true;
      sliderLabel.textContent = 'Password Length';
      slider.min = 4; slider.max = 128; slider.value = 16;
      lengthBox.min = 4; lengthBox.max = 128; lengthBox.value = 16;
    } else {
      tabPassphrase.classList.add('active');
      tabPassword.classList.remove('active');
      passwordOpts.hidden = true;
      passphraseOpts.hidden = false;
      sliderLabel.textContent = 'Word Count';
      slider.min = 3; slider.max = 10; slider.value = 4;
      lengthBox.min = 3; lengthBox.max = 10; lengthBox.value = 4;
    }
    generate();
  }

  // ── Allowed values for validated inputs ────────────────────
  const ALLOWED_THEMES = Object.freeze(['violet', 'emerald', 'sapphire', 'cyberpunk']);
  const ALLOWED_SEPARATORS = Object.freeze(['hyphen', 'space', 'period', 'underscore']);

  // ── Generator Dispatch ──────────────────────────────────────
  function generate() {
    triggerHaptic();
    let password = '';
    let bits = 0;

    if (currentMode === 'password') {
      // PATCH #6: Clamp length to strict bounds before use
      const rawLen = parseInt(lengthBox.value, 10);
      const len = (isNaN(rawLen) ? 16 : Math.min(128, Math.max(4, rawLen)));
      const excludeAmbiguous = togAmbiguous.classList.contains('active');
      const activeSets = getActiveCharsets(toggleEls, excludeAmbiguous);

      if (activeSets.length === 0) {
        showToast('Enable at least one character set', 'error');
        passText.textContent = '';
        return;
      }
      password = generateStandardPassword(len, activeSets);
      bits = calcEntropyBits('password', { len, activeSets });
    } else {
      // PATCH #6: Clamp word count to strict bounds
      const rawCount = parseInt(lengthBox.value, 10);
      const wordCount = (isNaN(rawCount) ? 4 : Math.min(10, Math.max(3, rawCount)));
      // PATCH #9: Allowlist separator value
      const rawSep = selectSeparator.value;
      const safeSep = ALLOWED_SEPARATORS.includes(rawSep) ? rawSep : 'hyphen';
      const sepMap = { hyphen: '-', space: ' ', period: '.', underscore: '_' };
      const separator = sepMap[safeSep];
      const capitalize = togCapitalize.classList.contains('active');
      const includeNumber = togIncludeNum.classList.contains('active');

      password = generatePassphrase(wordCount, separator, capitalize, includeNumber);
      bits = calcEntropyBits('passphrase', { wordCount, includeNumber });
    }

    passText.textContent = password;
    updateStrengthUI(bits);
    addToHistory(password);

    // Rotate icon animation
    const icon = genBtn.querySelector('svg');
    if (icon) {
      icon.style.transform = 'rotate(360deg)';
      setTimeout(() => { icon.style.transform = ''; }, 500);
    }
  }

  function updateStrengthUI(bits) {
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
  }

  // ── History Management ──────────────────────────────────────
  function addToHistory(pass) {
    if (!pass || (history[0] && history[0].text === pass)) return;
    history.unshift({ text: pass, masked: true });
    if (history.length > 5) history.pop();
    renderHistory();
  }

  function renderHistory() {
    histList.innerHTML = '';
    history.forEach((item, i) => {
      const li = document.createElement('li');
      li.className = 'hist-item';

      const span = document.createElement('span');
      span.className = `hist-pass ${item.masked ? 'masked' : ''}`;
      span.textContent = item.masked ? '••••••••••••' : item.text;

      const actions = document.createElement('div');
      actions.className = 'hist-actions';

      // PATCH #1: Build SVG nodes with createElementNS instead of innerHTML to prevent XSS
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
      li.append(span, actions);
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
      // PATCH #7: Secure fallback — hide from screen readers and guarantee cleanup
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
        // Guaranteed cleanup even if an exception occurs
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
      } catch (_) {
        // Ignored if permissions fail
      }
    }, 30000);
  }

  // PATCH #2: Replace innerHTML icon updates with safe DOM methods
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
    if (!text) { showToast('Generate a password first', 'error'); return; }

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

  // ── Bulk Modal ──────────────────────────────────────────────
  function openBulkModal() {
    renderBulkPasswords();
    bulkModal.classList.add('open');
  }
  function closeBulkModal() {
    bulkModal.classList.remove('open');
  }

  function renderBulkPasswords() {
    const rawCount = parseInt(bulkCountSelect.value, 10);
    // PATCH #6: Clamp bulk count to prevent performance DoS
    const count = (isNaN(rawCount) ? 10 : Math.min(50, Math.max(1, rawCount)));
    const items = [];

    for (let i = 0; i < count; i++) {
      if (currentMode === 'password') {
        const rawLen = parseInt(lengthBox.value, 10);
        const len = (isNaN(rawLen) ? 16 : Math.min(128, Math.max(4, rawLen)));
        const excludeAmbiguous = togAmbiguous.classList.contains('active');
        const activeSets = getActiveCharsets(toggleEls, excludeAmbiguous);
        items.push(generateStandardPassword(len, activeSets));
      } else {
        const rawCount2 = parseInt(lengthBox.value, 10);
        const wordCount = (isNaN(rawCount2) ? 4 : Math.min(10, Math.max(3, rawCount2)));
        // PATCH #9: Allowlist separator value
        const rawSep = selectSeparator.value;
        const safeSep = ALLOWED_SEPARATORS.includes(rawSep) ? rawSep : 'hyphen';
        const sepMap = { hyphen: '-', space: ' ', period: '.', underscore: '_' };
        const separator = sepMap[safeSep];
        const capitalize = togCapitalize.classList.contains('active');
        const includeNumber = togIncludeNum.classList.contains('active');
        items.push(generatePassphrase(wordCount, separator, capitalize, includeNumber));
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
    // PATCH #11: Use crypto random suffix instead of predictable timestamp
    const randHex = Array.from(crypto.getRandomValues(new Uint8Array(4)))
      .map(b => b.toString(16).padStart(2, '0')).join('');
    a.download = `passwords_${randHex}.txt`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Downloaded passwords TXT', 'success');
  }

  // ── Theme Switcher ──────────────────────────────────────────
  function initTheme() {
    const saved = localStorage.getItem('passgen_theme') || 'violet';
    // PATCH #5: Validate stored theme against allowlist before use
    const safeTheme = ALLOWED_THEMES.includes(saved) ? saved : 'violet';
    setTheme(safeTheme);
  }

  function setTheme(themeName) {
    // PATCH #5: Strict allowlist — reject any value not in the approved list
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

  slider.addEventListener('input', () => {
    lengthBox.value = slider.value;
    generate();
  });
  lengthBox.addEventListener('input', () => {
    const v = parseInt(lengthBox.value, 10);
    const max = currentMode === 'password' ? 128 : 10;
    const min = currentMode === 'password' ? 4 : 3;
    if (!isNaN(v) && v >= min && v <= max) {
      slider.value = v;
      generate();
    }
  });

  // Toggles
  [...toggleEls, togAmbiguous, togCapitalize, togIncludeNum].forEach(el => {
    if (!el) return;
    function toggle() {
      const active = el.classList.toggle('active');
      el.setAttribute('aria-checked', active.toString());
      generate();
    }
    el.addEventListener('click', toggle);
    el.addEventListener('keydown', e => { if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); toggle(); } });
  });

  selectSeparator.addEventListener('change', generate);

  genBtn.addEventListener('click', generate);
  copyBtn.addEventListener('click', copyCurrentPassword);
  clearHistBtn.addEventListener('click', clearHistory);

  bulkBtn.addEventListener('click', openBulkModal);
  closeBulkBtn.addEventListener('click', closeBulkModal);
  bulkCountSelect.addEventListener('change', renderBulkPasswords);
  copyBulkBtn.addEventListener('click', () => copyText(bulkTextarea.value));
  downloadBulkBtn.addEventListener('click', downloadBulkTxt);

  // PATCH #12: Close modal on Escape key and outside-click for keyboard accessibility
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && bulkModal.classList.contains('open')) closeBulkModal();
  });
  bulkModal.addEventListener('click', e => {
    if (e.target === bulkModal) closeBulkModal();
  });

  // Themes — PATCH #5: Allowlist validated inside setTheme()
  document.querySelectorAll('.theme-dot').forEach(dot => {
    dot.addEventListener('click', () => {
      const theme = dot.getAttribute('data-set-theme');
      // Only call setTheme with values from the DOM that are in the allowlist
      if (ALLOWED_THEMES.includes(theme)) setTheme(theme);
    });
  });

  // Keyboard shortcut Ctrl/Cmd + Enter
  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') generate();
  });

  // Service Worker Registration
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').catch(() => {});
    });
  }

  // Initialize
  initTheme();
  generate();

  // PATCH #4: Freeze the PassGen export to prevent external mutation of core functions.
  // Only expose in non-production contexts (when test runner flag is set).
  if (typeof window.__PASSGEN_TEST__ !== 'undefined' && window.__PASSGEN_TEST__ === true) {
    window.PassGen = Object.freeze({
      secureRandom,
      shuffle,
      getActiveCharsets,
      buildCharset,
      generateStandardPassword,
      generatePassphrase,
      calcEntropyBits,
      estimateCrackTime,
      CHARS,
      AMBIGUOUS,
      STRENGTH_LEVELS
    });
  }
})();
