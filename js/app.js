(function () {
  'use strict';

  const CHARS = {
    upper:   'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
    lower:   'abcdefghijklmnopqrstuvwxyz',
    nums:    '0123456789',
    symbols: '!@#$%^&*()-_=+[]{}|;:,.<>?'
  };

  function secureRandom(max) {
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

  function getActiveCharsets(toggleEls) {
    const keys = ['upper','lower','nums','symbols'];
    return toggleEls.map((el, i) => el.classList.contains('active') ? keys[i] : null).filter(Boolean);
  }

  function buildCharset(activeKeys) {
    return activeKeys.map(k => CHARS[k]).join('');
  }

  function calcEntropy(len, activeKeys) {
    const poolSize = buildCharset(activeKeys).length;
    return Math.floor(len * Math.log2(poolSize));
  }

  const STRENGTH_LEVELS = [
    { label: 'Weak',       color: 'var(--weak)',   segs: 1 },
    { label: 'Fair',       color: 'var(--fair)',   segs: 2 },
    { label: 'Strong',     color: 'var(--strong)', segs: 3 },
    { label: 'Very Strong',color: 'var(--great)',  segs: 4 },
  ];

  const toggleIds   = ['tog-upper','tog-lower','tog-nums','tog-sym'];
  const toggleEls   = toggleIds.map(id => document.getElementById(id));

  function generatePassword() {
    const len = getLength();
    if (!len) return;

    const activeKeys = getActiveCharsets(toggleEls);
    if (activeKeys.length === 0) {
      showToast('Enable at least one character type', 'error');
      return;
    }

    const guaranteed = activeKeys.map(k => CHARS[k][secureRandom(CHARS[k].length)]);

    const all = buildCharset(activeKeys);
    const rest = Array.from({ length: Math.max(0, len - guaranteed.length) }, () => all[secureRandom(all.length)]);

    const password = shuffle([...guaranteed, ...rest]).join('');

    passText.textContent = password;
    updateStrength(len, activeKeys);
    addToHistory(password);
    showToast('Password generated', 'success');

    const icon = genBtn.querySelector('svg');
    icon.style.transform = 'rotate(360deg)';
    setTimeout(() => { icon.style.transform = ''; }, 500);
  }

  function updateStrength(len, activeKeys) {
    const bits = calcEntropy(len, activeKeys);
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
    entropyEl.textContent = `~${bits} bits of entropy`;
  }

  const history = [];
  let toastTimer = null;

  function addToHistory(pass) {
    if (history[0] === pass) return;
    history.unshift(pass);
    if (history.length > 5) history.pop();

    histList.innerHTML = '';
    history.forEach((p, i) => {
      const li  = document.createElement('li');
      li.className = 'hist-item';

      const span = document.createElement('span');
      span.className = 'hist-pass';
      span.textContent = p;

      const btn = document.createElement('button');
      btn.className = 'hist-copy';
      btn.textContent = 'Copy';
      btn.setAttribute('aria-label', `Copy password ${i + 1} from history`);
      btn.addEventListener('click', () => copyText(p));

      li.append(span, btn);
      histList.appendChild(li);
    });

    histWrap.hidden = history.length === 0;
  }

  function clearHistory() {
    if (history.length === 0) return;
    history.length = 0;
    histList.innerHTML = '';
    histWrap.style.opacity = '0';
    histWrap.style.transform = 'translateY(6px)';
    histWrap.style.transition = 'opacity .2s, transform .2s';
    setTimeout(() => {
      histWrap.hidden = true;
      histWrap.style.opacity = '';
      histWrap.style.transform = '';
      histWrap.style.transition = '';
    }, 220);
    showToast('History cleared', 'success');
  }

  async function copyText(text) {
    if (!text) { showToast('Nothing to copy', 'error'); return; }
    try {
      await navigator.clipboard.writeText(text);
      showToast('Copied to clipboard!', 'success');
    } catch {
      const ta = document.createElement('textarea');
      ta.value = text;
      ta.style.cssText = 'position:fixed;opacity:0;pointer-events:none';
      document.body.appendChild(ta);
      ta.focus(); ta.select();
      document.execCommand('copy');
      document.body.removeChild(ta);
      showToast('Copied!', 'success');
    }
  }

  function copyCurrentPassword() {
    const text = passText.textContent.trim();
    if (!text) { showToast('Generate a password first', 'error'); return; }

    copyBtn.classList.add('copied');
    copyIcon.innerHTML = `<polyline points="20 6 9 17 4 12"/>`;
    copyText(text);

    setTimeout(() => {
      copyBtn.classList.remove('copied');
      copyIcon.innerHTML = `<rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>`;
    }, 2000);
  }

  function showToast(msg, type = 'success') {
    clearTimeout(toastTimer);
    toastEl.textContent = msg;
    toastEl.className = `toast show ${type}`;
    toastTimer = setTimeout(() => { toastEl.className = 'toast'; }, 2800);
  }

  function getLength() {
    const val = parseInt(lengthBox.value, 10);
    if (isNaN(val) || val < 4 || val > 128) {
      showToast('Length must be 4 – 128', 'error');
      return null;
    }
    return val;
  }

  const slider      = document.getElementById('lengthSlider');
  const lengthBox   = document.getElementById('lengthBox');
  const passText    = document.getElementById('passwordText');
  const copyBtn     = document.getElementById('copyBtn');
  const copyIcon    = document.getElementById('copyIcon');
  const genBtn      = document.getElementById('generateBtn');
  const strengthEl  = document.getElementById('strengthText');
  const segs        = [1,2,3,4].map(n => document.getElementById('seg'+n));
  const entropyEl   = document.getElementById('entropyInfo');
  const histWrap    = document.getElementById('historyWrap');
  const histList    = document.getElementById('historyList');
  const clearHistBtn = document.getElementById('clearHistBtn');
  const toastEl     = document.getElementById('toast');
  const trackEl     = document.querySelector('.strength-track');

  slider.addEventListener('input', () => { lengthBox.value = slider.value; });

  lengthBox.addEventListener('input', () => {
    const v = parseInt(lengthBox.value, 10);
    if (!isNaN(v) && v >= 4 && v <= 128) slider.value = v;
  });

  toggleEls.forEach(el => {
    function toggle() {
      const active = el.classList.toggle('active');
      el.setAttribute('aria-checked', active.toString());
    }
    el.addEventListener('click', toggle);
    el.addEventListener('keydown', e => { if (e.key === ' ' || e.key === 'Enter') { e.preventDefault(); toggle(); } });
  });

  genBtn.addEventListener('click', generatePassword);
  copyBtn.addEventListener('click', copyCurrentPassword);
  clearHistBtn.addEventListener('click', clearHistory);

  document.addEventListener('keydown', e => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') generatePassword();
  });

  generatePassword();

  window.PassGen = { secureRandom, shuffle, calcEntropy, buildCharset, getActiveCharsets, CHARS, STRENGTH_LEVELS };
})();
