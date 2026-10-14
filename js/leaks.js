(function () {
  'use strict';

  // ── Curated Offline Database of Most Common Leaked & Dangerous Passwords ──
  // Compiled from public breach digests (SecLists, RockYou top frequencies, NordPass top breached).
  const COMMON_LEAKED_PASSWORDS = new Set([
    'password', '123456', '123456789', 'guest', 'qwerty', '12345678', '111111',
    '12345', '1234567', 'dragon', 'p@ssword', 'welcome', '123123', 'football',
    'baseball', 'monkey', 'letmein', 'princess', 'sunshine', 'iloveyou', 'trustno1',
    'admin', 'admin123', 'pass123', 'welcome1', 'superman', 'master', 'shadow',
    'charlie', 'starwars', 'killer', '666666', '777777', '888888', '999999',
    '000000', 'hello', 'secret', 'default', 'root', 'toor', 'test', 'login',
    'hunter2', 'myspace1', 'matrix', 'super123', 'qwertyuiop', 'asdfghjkl',
    'zxcvbnm', 'password1', 'passphrase', 'abc123', '1q2w3e4r', '0123456789',
    'trustme', 'welcome@123', 'system', 'pass@123', 'server', 'oracle', 'database',
    'access', 'changeme', 'manager', 'security', 'secure123', 'cisco', 'cisco123',
    'spring2024', 'summer2024', 'winter2024', 'fall2024', 'coffee', 'computer',
    'internet', 'freedom', 'forever', 'football1', 'arsenal', 'chelsea', 'liverpool',
    'barcelona', 'realmadrid', 'pokemon', 'naruto', 'dragonball', 'batman', 'spiderman',
    'ironman', 'avengers', 'disney', 'starwars1', 'jordan', 'kobe24', 'michael',
    'jessica', 'daniel', 'ashley', 'matthew', 'amanda', 'anthony', 'jennifer',
    'joshua', 'brittany', 'david', 'samantha', 'andrew', 'taylor', 'james',
    'elizabeth', 'justin', 'stephanie', 'joseph', 'nicole', 'william', 'megan',
    'robert', 'alexandra', 'christopher', 'hannah', 'ryan', 'rachel', 'nicholas',
    'sarah', 'brandon', 'kayla', 'anthony', 'victoria', 'tyler', 'emily',
    'alexander', 'rebecca', 'samuel', 'lauren', 'john', 'morgan', 'dylan',
    'melissa', 'christian', 'michelle', 'benjamin', 'amber', 'zachary', 'courtney'
  ]);

  /**
   * Checks if a password string is in the offline common leak dictionary
   * or matches trivial sequential/repeated patterns.
   * @param {string} password
   * @returns {{ isLeaked: boolean, reason?: string }}
   */
  function checkLeakedPassword(password) {
    if (!password || typeof password !== 'string') {
      return { isLeaked: false };
    }

    const clean = password.trim().toLowerCase();

    // 1. Direct match in common leak database
    if (COMMON_LEAKED_PASSWORDS.has(clean)) {
      return {
        isLeaked: true,
        reason: 'Known breached password found in public leak databases.'
      };
    }

    // 2. Pure single-character repetition (e.g., 'aaaaaa', '1111111')
    if (password.length >= 4 && /^([\s\S])\1+$/.test(password)) {
      return {
        isLeaked: true,
        reason: 'Trivial repeated character pattern.'
      };
    }

    // 3. Trivial sequential digits (e.g., '12345678', '987654321')
    if (/^(0123456789|123456789|9876543210|987654321)+$/.test(clean)) {
      return {
        isLeaked: true,
        reason: 'Trivial numeric keyboard sequence.'
      };
    }

    // 4. Keyboard walk patterns
    const walks = ['qwerty', 'asdfgh', 'zxcvbn', '1qaz2wsx'];
    for (const walk of walks) {
      if (clean.includes(walk)) {
        return {
          isLeaked: true,
          reason: 'Common keyboard walk pattern detected.'
        };
      }
    }

    return { isLeaked: false };
  }

  const LeakedPasswordChecker = Object.freeze({
    checkLeakedPassword,
    COMMON_LEAK_COUNT: COMMON_LEAKED_PASSWORDS.size
  });

  if (typeof window !== 'undefined') {
    window.LeakedPasswordChecker = LeakedPasswordChecker;
  }
  if (typeof global !== 'undefined') {
    global.LeakedPasswordChecker = LeakedPasswordChecker;
  }
})();
