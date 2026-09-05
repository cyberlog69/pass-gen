(function () {
  'use strict';

  // Galois Field GF(256) tables for Reed-Solomon error correction
  const EXP_TABLE = new Uint8Array(512);
  const LOG_TABLE = new Uint8Array(256);

  (function initGF256() {
    let x = 1;
    for (let i = 0; i < 255; i++) {
      EXP_TABLE[i] = x;
      LOG_TABLE[x] = i;
      x <<= 1;
      if (x & 0x100) x ^= 0x11d;
    }
    for (let i = 255; i < 512; i++) {
      EXP_TABLE[i] = EXP_TABLE[i - 255];
    }
  })();

  function gfMul(x, y) {
    if (x === 0 || y === 0) return 0;
    return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]];
  }

  function polyMul(p1, p2) {
    const res = new Uint8Array(p1.length + p2.length - 1);
    for (let i = 0; i < p1.length; i++) {
      for (let j = 0; j < p2.length; j++) {
        res[i + j] ^= gfMul(p1[i], p2[j]);
      }
    }
    return res;
  }

  function getGeneratorPoly(numEC) {
    let poly = new Uint8Array([1]);
    for (let i = 0; i < numEC; i++) {
      poly = polyMul(poly, new Uint8Array([1, EXP_TABLE[i]]));
    }
    return poly;
  }

  function calculateECC(dataBytes, numEC) {
    const gen = getGeneratorPoly(numEC);
    const msg = new Uint8Array(dataBytes.length + numEC);
    msg.set(dataBytes, 0);

    for (let i = 0; i < dataBytes.length; i++) {
      const coef = msg[i];
      if (coef !== 0) {
        for (let j = 0; j < gen.length; j++) {
          msg[i + j] ^= gfMul(gen[j], coef);
        }
      }
    }
    return msg.slice(dataBytes.length);
  }

  // QR Code specs for byte mode, Error Correction Level L (supports up to 154 characters for V1-V8)
  // [version, totalBytes, dataBytes, numEC, ecBlocks]
  const VERSIONS = [
    [1, 26, 19, 7, 1],
    [2, 44, 34, 10, 1],
    [3, 70, 55, 15, 1],
    [4, 100, 80, 20, 1],
    [5, 134, 108, 26, 1],
    [6, 172, 136, 18, 2],
    [7, 196, 156, 20, 2],
    [8, 242, 194, 24, 2],
    [9, 292, 232, 30, 2],
    [10, 346, 274, 18, 4]
  ];

  // Alignment pattern row/col coordinates per version
  const ALIGNMENT_COORDS = [
    [],               // V1: none
    [6, 18],          // V2
    [6, 22],          // V3
    [6, 26],          // V4
    [6, 30],          // V5
    [6, 34],          // V6
    [6, 22, 38],      // V7
    [6, 24, 42],      // V8
    [6, 26, 46],      // V9
    [6, 28, 50]       // V10
  ];

  function getVersionInfo(dataLength) {
    for (let i = 0; i < VERSIONS.length; i++) {
      const [ver, , dataCapacity] = VERSIONS[i];
      // Byte mode overhead: 4 bits mode + 8 bits length (for V1-V9) or 16 bits (for V10+)
      const overhead = ver < 10 ? 2 : 3;
      if (dataLength + overhead <= dataCapacity) {
        return VERSIONS[i];
      }
    }
    return VERSIONS[VERSIONS.length - 1];
  }

  function encodeData(text, version, dataCapacity) {
    const encoder = new TextEncoder();
    const rawBytes = encoder.encode(text);
    const bits = [];

    function pushBits(val, length) {
      for (let i = length - 1; i >= 0; i--) {
        bits.push((val >> i) & 1);
      }
    }

    // Byte mode: 0100
    pushBits(0b0100, 4);

    // Character count indicator (8 bits for ver 1-9, 16 bits for ver 10)
    const countBits = version < 10 ? 8 : 16;
    pushBits(rawBytes.length, countBits);

    // Payload data bits
    for (let i = 0; i < rawBytes.length; i++) {
      pushBits(rawBytes[i], 8);
    }

    // Terminator (up to 4 zeroes)
    const totalDataBits = dataCapacity * 8;
    const termLen = Math.min(4, totalDataBits - bits.length);
    for (let i = 0; i < termLen; i++) bits.push(0);

    // Pad to byte boundary
    while (bits.length % 8 !== 0) bits.push(0);

    // Convert to bytes
    const dataBytes = [];
    for (let i = 0; i < bits.length; i += 8) {
      let b = 0;
      for (let j = 0; j < 8; j++) b = (b << 1) | bits[i + j];
      dataBytes.push(b);
    }

    // Pad bytes 0xEC and 0x11 alternately
    const padBytes = [0xec, 0x11];
    let padIdx = 0;
    while (dataBytes.length < dataCapacity) {
      dataBytes.push(padBytes[padIdx]);
      padIdx = 1 - padIdx;
    }

    return new Uint8Array(dataBytes);
  }

  function buildInterleavedCodewords(dataBytes, verInfo) {
    const [, , dataCapacity, numEC, numBlocks] = verInfo;
    const blockDataLen = Math.floor(dataCapacity / numBlocks);
    const ecCodewords = [];
    const dataBlocks = [];

    for (let b = 0; b < numBlocks; b++) {
      const slice = dataBytes.slice(b * blockDataLen, (b + 1) * blockDataLen);
      dataBlocks.push(slice);
      ecCodewords.push(calculateECC(slice, numEC));
    }

    const result = [];
    // Interleave data
    for (let i = 0; i < blockDataLen; i++) {
      for (let b = 0; b < numBlocks; b++) {
        result.push(dataBlocks[b][i]);
      }
    }
    // Interleave EC
    for (let i = 0; i < numEC; i++) {
      for (let b = 0; b < numBlocks; b++) {
        result.push(ecCodewords[b][i]);
      }
    }

    return new Uint8Array(result);
  }

  function createMatrix(version) {
    const size = version * 4 + 17;
    const matrix = [];
    const isReserved = [];
    for (let r = 0; r < size; r++) {
      matrix.push(new Uint8Array(size));
      isReserved.push(new Uint8Array(size));
    }

    function setModule(r, c, val, reserved = true) {
      if (r >= 0 && r < size && c >= 0 && c < size) {
        matrix[r][c] = val ? 1 : 0;
        if (reserved) isReserved[r][c] = 1;
      }
    }

    // Finder pattern (7x7) + separator (8x8)
    function addFinder(row, col) {
      for (let r = -1; r <= 7; r++) {
        for (let c = -1; c <= 7; c++) {
          const mr = row + r;
          const mc = col + c;
          if (mr >= 0 && mr < size && mc >= 0 && mc < size) {
            const inPattern = (r >= 0 && r <= 6 && c >= 0 && c <= 6);
            if (inPattern) {
              const isBorder = (r === 0 || r === 6 || c === 0 || c === 6);
              const isCenter = (r >= 2 && r <= 4 && c >= 2 && c <= 4);
              setModule(mr, mc, isBorder || isCenter);
            } else {
              setModule(mr, mc, 0); // separator
            }
          }
        }
      }
    }

    addFinder(0, 0);
    addFinder(0, size - 7);
    addFinder(size - 7, 0);

    // Timing patterns
    for (let i = 8; i < size - 8; i++) {
      if (!isReserved[6][i]) setModule(6, i, i % 2 === 0);
      if (!isReserved[i][6]) setModule(i, 6, i % 2 === 0);
    }

    // Alignment patterns
    const alignCoords = ALIGNMENT_COORDS[version - 1];
    for (let i = 0; i < alignCoords.length; i++) {
      for (let j = 0; j < alignCoords.length; j++) {
        const ar = alignCoords[i];
        const ac = alignCoords[j];
        if (isReserved[ar][ac]) continue;

        for (let r = -2; r <= 2; r++) {
          for (let c = -2; c <= 2; c++) {
            const isBorder = (Math.abs(r) === 2 || Math.abs(c) === 2);
            const isCenter = (r === 0 && c === 0);
            setModule(ar + r, ac + c, isBorder || isCenter);
          }
        }
      }
    }

    // Dark module
    setModule(size - 8, 8, 1);

    // Reserve format information areas
    for (let i = 0; i < 9; i++) {
      if (!isReserved[8][i]) setModule(8, i, 0);
      if (!isReserved[i][8]) setModule(i, 8, 0);
    }
    for (let i = size - 8; i < size; i++) {
      if (!isReserved[8][i]) setModule(8, i, 0);
      if (!isReserved[i][8]) setModule(i, 8, 0);
    }

    return { size, matrix, isReserved };
  }

  // Format info for Level L, mask 0: 0x77c4
  const FORMAT_INFO_L_MASK0 = 0b111011111000100;

  function applyFormatInfo(matrixObj) {
    const { size, matrix } = matrixObj;
    const format = FORMAT_INFO_L_MASK0;

    // Top-left and split corners
    for (let i = 0; i < 15; i++) {
      const bit = (format >> i) & 1;
      // Along top-left
      if (i <= 5) {
        matrix[8][i] = bit;
      } else if (i === 6) {
        matrix[8][7] = bit;
      } else if (i === 7) {
        matrix[8][8] = bit;
      } else if (i === 8) {
        matrix[7][8] = bit;
      } else {
        matrix[14 - i][8] = bit;
      }

      // Along edges
      if (i < 8) {
        matrix[size - 1 - i][8] = bit;
      } else {
        matrix[8][size - 15 + i] = bit;
      }
    }
  }

  function placeData(matrixObj, codewords) {
    const { size, matrix, isReserved } = matrixObj;
    let bitIdx = 0;
    const totalBits = codewords.length * 8;

    let right = size - 1;
    let upward = true;

    while (right > 0) {
      if (right === 6) right--; // Skip vertical timing column

      const cols = [right, right - 1];
      const rows = upward
        ? Array.from({ length: size }, (_, i) => size - 1 - i)
        : Array.from({ length: size }, (_, i) => i);

      for (let r of rows) {
        for (let c of cols) {
          if (!isReserved[r][c]) {
            let bit = 0;
            if (bitIdx < totalBits) {
              const byteVal = codewords[Math.floor(bitIdx / 8)];
              bit = (byteVal >> (7 - (bitIdx % 8))) & 1;
              bitIdx++;
            }

            // Mask 0: (row + col) % 2 === 0
            if ((r + c) % 2 === 0) {
              bit ^= 1;
            }

            matrix[r][c] = bit;
          }
        }
      }

      upward = !upward;
      right -= 2;
    }
  }

  function generateQRCodeMatrix(text) {
    const verInfo = getVersionInfo(text.length);
    const [version, , dataCapacity] = verInfo;
    const dataBytes = encodeData(text, version, dataCapacity);
    const codewords = buildInterleavedCodewords(dataBytes, verInfo);
    const matrixObj = createMatrix(version);
    placeData(matrixObj, codewords);
    applyFormatInfo(matrixObj);
    return matrixObj;
  }

  function generateSVG(text, options = {}) {
    if (!text) return null;
    const matrixObj = generateQRCodeMatrix(text);
    const { size, matrix } = matrixObj;
    const margin = options.margin !== undefined ? options.margin : 2;
    const viewBoxSize = size + margin * 2;

    const NS = 'http://www.w3.org/2000/svg';
    const svg = document.createElementNS(NS, 'svg');
    svg.setAttribute('viewBox', `0 0 ${viewBoxSize} ${viewBoxSize}`);
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('shape-rendering', 'crispEdges');
    svg.setAttribute('role', 'img');
    svg.setAttribute('aria-label', 'QR Code for password');

    // Background rect
    const bg = document.createElementNS(NS, 'rect');
    bg.setAttribute('width', viewBoxSize.toString());
    bg.setAttribute('height', viewBoxSize.toString());
    bg.setAttribute('fill', options.bgColor || '#ffffff');
    svg.appendChild(bg);

    // Path for dark modules
    let pathD = '';
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (matrix[r][c] === 1) {
          const x = c + margin;
          const y = r + margin;
          pathD += `M${x},${y}h1v1h-1z `;
        }
      }
    }

    const path = document.createElementNS(NS, 'path');
    path.setAttribute('d', pathD.trim());
    path.setAttribute('fill', options.fgColor || '#0a0a0f');
    svg.appendChild(path);

    return svg;
  }

  function renderToElement(targetEl, text, options = {}) {
    while (targetEl.firstChild) {
      targetEl.removeChild(targetEl.firstChild);
    }
    const svg = generateSVG(text, options);
    if (svg) {
      targetEl.appendChild(svg);
    }
  }

  window.QRCode = Object.freeze({
    generateSVG,
    renderToElement,
    generateQRCodeMatrix
  });
})();
