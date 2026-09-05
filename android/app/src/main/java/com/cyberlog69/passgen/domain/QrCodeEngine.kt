package com.cyberlog69.passgen.domain

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.min

object QrCodeEngine {

    private val EXP_TABLE = IntArray(512)
    private val LOG_TABLE = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            EXP_TABLE[i] = x
            LOG_TABLE[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) x = x xor 0x11d
        }
        for (i in 255 until 512) {
            EXP_TABLE[i] = EXP_TABLE[i - 255]
        }
    }

    private fun gfMul(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return EXP_TABLE[LOG_TABLE[x] + LOG_TABLE[y]]
    }

    private fun polyMul(p1: IntArray, p2: IntArray): IntArray {
        val res = IntArray(p1.size + p2.size - 1)
        for (i in p1.indices) {
            for (j in p2.indices) {
                res[i + j] = res[i + j] xor gfMul(p1[i], p2[j])
            }
        }
        return res
    }

    private fun getGeneratorPoly(numEC: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until numEC) {
            poly = polyMul(poly, intArrayOf(1, EXP_TABLE[i]))
        }
        return poly
    }

    private fun calculateECC(dataBytes: IntArray, numEC: Int): IntArray {
        val gen = getGeneratorPoly(numEC)
        val msg = IntArray(dataBytes.size + numEC)
        System.arraycopy(dataBytes, 0, msg, 0, dataBytes.size)

        for (i in dataBytes.indices) {
            val coef = msg[i]
            if (coef != 0) {
                for (j in gen.indices) {
                    msg[i + j] = msg[i + j] xor gfMul(gen[j], coef)
                }
            }
        }
        return msg.copyOfRange(dataBytes.size, msg.size)
    }

    private data class VersionInfo(
        val version: Int,
        val totalBytes: Int,
        val dataCapacity: Int,
        val numEC: Int,
        val numBlocks: Int
    )

    private val VERSIONS = arrayOf(
        VersionInfo(1, 26, 19, 7, 1),
        VersionInfo(2, 44, 34, 10, 1),
        VersionInfo(3, 70, 55, 15, 1),
        VersionInfo(4, 100, 80, 20, 1),
        VersionInfo(5, 134, 108, 26, 1),
        VersionInfo(6, 172, 136, 18, 2),
        VersionInfo(7, 196, 156, 20, 2),
        VersionInfo(8, 242, 194, 24, 2),
        VersionInfo(9, 292, 232, 30, 2),
        VersionInfo(10, 346, 274, 18, 4)
    )

    private val ALIGNMENT_COORDS = arrayOf(
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50)
    )

    private fun getVersionInfo(dataLength: Int): VersionInfo {
        for (ver in VERSIONS) {
            val overhead = if (ver.version < 10) 2 else 3
            if (dataLength + overhead <= ver.dataCapacity) {
                return ver
            }
        }
        return VERSIONS.last()
    }

    private fun encodeData(text: String, version: Int, dataCapacity: Int): IntArray {
        val rawBytes = text.toByteArray(Charsets.UTF_8)
        val bits = mutableListOf<Int>()

        fun pushBits(value: Int, length: Int) {
            for (i in length - 1 downTo 0) {
                bits.add((value shr i) and 1)
            }
        }

        // Mode byte: 0100
        pushBits(0b0100, 4)

        // Character count indicator
        val countBits = if (version < 10) 8 else 16
        pushBits(rawBytes.size, countBits)

        // Data bytes
        for (b in rawBytes) {
            pushBits(b.toInt() and 0xFF, 8)
        }

        // Terminator (up to 4 zeroes)
        val totalDataBits = dataCapacity * 8
        val termLen = min(4, totalDataBits - bits.size)
        repeat(termLen) { bits.add(0) }

        // Pad to byte boundary
        while (bits.size % 8 != 0) bits.add(0)

        val dataBytes = mutableListOf<Int>()
        for (i in bits.indices step 8) {
            var b = 0
            for (j in 0 until 8) {
                b = (b shl 1) or bits[i + j]
            }
            dataBytes.add(b)
        }

        // Pad bytes 0xEC, 0x11
        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (dataBytes.size < dataCapacity) {
            dataBytes.add(padBytes[padIdx])
            padIdx = 1 - padIdx
        }

        return dataBytes.toIntArray()
    }

    private fun buildInterleavedCodewords(dataBytes: IntArray, verInfo: VersionInfo): IntArray {
        val numBlocks = verInfo.numBlocks
        val blockDataLen = verInfo.dataCapacity / numBlocks
        val ecCodewords = mutableListOf<IntArray>()
        val dataBlocks = mutableListOf<IntArray>()

        for (b in 0 until numBlocks) {
            val slice = dataBytes.copyOfRange(b * blockDataLen, (b + 1) * blockDataLen)
            dataBlocks.add(slice)
            ecCodewords.add(calculateECC(slice, verInfo.numEC))
        }

        val result = mutableListOf<Int>()
        for (i in 0 until blockDataLen) {
            for (b in 0 until numBlocks) {
                result.add(dataBlocks[b][i])
            }
        }
        for (i in 0 until verInfo.numEC) {
            for (b in 0 until numBlocks) {
                result.add(ecCodewords[b][i])
            }
        }
        return result.toIntArray()
    }

    private class Matrix(val size: Int) {
        val matrix = Array(size) { IntArray(size) }
        val isReserved = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, value: Boolean, reserved: Boolean = true) {
            if (r in 0 until size && c in 0 until size) {
                matrix[r][c] = if (value) 1 else 0
                if (reserved) isReserved[r][c] = true
            }
        }
    }

    private fun createMatrix(version: Int): Matrix {
        val size = version * 4 + 17
        val m = Matrix(size)

        fun addFinder(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val mr = row + r
                    val mc = col + c
                    if (mr in 0 until size && mc in 0 until size) {
                        val inPattern = r in 0..6 && c in 0..6
                        if (inPattern) {
                            val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                            val isCenter = r in 2..4 && c in 2..4
                            m.setModule(mr, mc, isBorder || isCenter)
                        } else {
                            m.setModule(mr, mc, false)
                        }
                    }
                }
            }
        }

        addFinder(0, 0)
        addFinder(0, size - 7)
        addFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            if (!m.isReserved[6][i]) m.setModule(6, i, i % 2 == 0)
            if (!m.isReserved[i][6]) m.setModule(i, 6, i % 2 == 0)
        }

        // Alignment patterns
        val alignCoords = ALIGNMENT_COORDS[version - 1]
        for (i in alignCoords.indices) {
            for (j in alignCoords.indices) {
                val ar = alignCoords[i]
                val ac = alignCoords[j]
                if (m.isReserved[ar][ac]) continue

                for (r in -2..2) {
                    for (c in -2..2) {
                        val isBorder = kotlin.math.abs(r) == 2 || kotlin.math.abs(c) == 2
                        val isCenter = r == 0 && c == 0
                        m.setModule(ar + r, ac + c, isBorder || isCenter)
                    }
                }
            }
        }

        // Dark module
        m.setModule(size - 8, 8, true)

        // Reserve format info
        for (i in 0 until 9) {
            if (!m.isReserved[8][i]) m.setModule(8, i, false)
            if (!m.isReserved[i][8]) m.setModule(i, 8, false)
        }
        for (i in size - 8 until size) {
            if (!m.isReserved[8][i]) m.setModule(8, i, false)
            if (!m.isReserved[i][8]) m.setModule(i, 8, false)
        }

        return m
    }

    private const val FORMAT_INFO_L_MASK0 = 0b111011111000100

    private fun applyFormatInfo(m: Matrix) {
        val format = FORMAT_INFO_L_MASK0
        val size = m.size
        for (i in 0 until 15) {
            val bit = (format shr i) and 1
            when {
                i <= 5 -> m.matrix[8][i] = bit
                i == 6 -> m.matrix[8][7] = bit
                i == 7 -> m.matrix[8][8] = bit
                i == 8 -> m.matrix[7][8] = bit
                else -> m.matrix[14 - i][8] = bit
            }
            if (i < 8) {
                m.matrix[size - 1 - i][8] = bit
            } else {
                m.matrix[8][size - 15 + i] = bit
            }
        }
    }

    private fun placeData(m: Matrix, codewords: IntArray) {
        var bitIdx = 0
        val totalBits = codewords.size * 8
        val size = m.size

        var right = size - 1
        var upward = true

        while (right > 0) {
            if (right == 6) right--

            val cols = intArrayOf(right, right - 1)
            val rows = if (upward) (size - 1 downTo 0).toList() else (0 until size).toList()

            for (r in rows) {
                for (c in cols) {
                    if (!m.isReserved[r][c]) {
                        var bit = 0
                        if (bitIdx < totalBits) {
                            val byteVal = codewords[bitIdx / 8]
                            bit = (byteVal shr (7 - (bitIdx % 8))) and 1
                            bitIdx++
                        }
                        // Mask 0: (row + col) % 2 == 0
                        if ((r + c) % 2 == 0) {
                            bit = bit xor 1
                        }
                        m.matrix[r][c] = bit
                    }
                }
            }
            upward = !upward
            right -= 2
        }
    }

    fun generateMatrix(text: String): Array<IntArray> {
        val verInfo = getVersionInfo(text.length)
        val dataBytes = encodeData(text, verInfo.version, verInfo.dataCapacity)
        val codewords = buildInterleavedCodewords(dataBytes, verInfo)
        val matrix = createMatrix(verInfo.version)
        placeData(matrix, codewords)
        applyFormatInfo(matrix)
        return matrix.matrix
    }

    fun generateBitmap(
        text: String,
        sizePx: Int = 512,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE,
        margin: Int = 2
    ): Bitmap {
        val matrix = generateMatrix(text)
        val qrSize = matrix.size
        val totalSize = qrSize + margin * 2
        val scale = sizePx / totalSize
        val finalSize = totalSize * scale

        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(finalSize * finalSize)

        for (y in 0 until finalSize) {
            val matrixY = (y / scale) - margin
            for (x in 0 until finalSize) {
                val matrixX = (x / scale) - margin
                val isDark = if (matrixY in 0 until qrSize && matrixX in 0 until qrSize) {
                    matrix[matrixY][matrixX] == 1
                } else {
                    false
                }
                pixels[y * finalSize + x] = if (isDark) fgColor else bgColor
            }
        }

        bitmap.setPixels(pixels, 0, finalSize, 0, 0, finalSize, finalSize)
        return bitmap
    }
}
