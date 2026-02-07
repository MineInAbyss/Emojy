package com.mineinabyss.emojy.helpers

import team.unnamed.creative.base.Writable
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.BufferedInputStream
import java.io.InputStream

class GifDecoder {
    companion object {
        const val STATUS_FORMAT_ERROR = 1
        const val STATUS_OPEN_ERROR = 2
        private const val MAX_STACK_SIZE = 4096
    }

    private var input: BufferedInputStream? = null
    private var status: Int = 0
    private var width: Int = 0
    private var height: Int = 0
    private var gctFlag: Boolean = false
    private var gctSize: Int = 0
    private var loopCount: Int = 1
    private var gct: IntArray? = null
    private var lct: IntArray? = null
    private var act: IntArray? = null
    private var bgIndex: Int = 0
    private var bgColor: Int = 0
    private var lastBgColor: Int = 0
    private var pixelAspect: Int = 0
    private var lctFlag: Boolean = false
    private var interlace: Boolean = false
    private var lctSize: Int = 0
    private var ix: Int = 0
    private var iy: Int = 0
    private var iw: Int = 0
    private var ih: Int = 0
    private var lastRect: Rectangle? = null
    private var image: BufferedImage? = null
    private var lastImage: BufferedImage? = null
    private var block: ByteArray = ByteArray(256)
    private var blockSize: Int = 0
    private var dispose: Int = 0
    private var lastDispose: Int = 0
    private var transparency: Boolean = false
    private var delay: Int = 0
    private var transIndex: Int = 0
    private var prefix: ShortArray? = null
    private var suffix: ByteArray? = null
    private var pixelStack: ByteArray? = null
    private var pixels: ByteArray? = null
    private var frames: MutableList<GifFrame> = ArrayList()
    private var frameCount: Int = 0

    fun getDelay(n: Int): Int {
        delay = -1
        if (n in 0 until frameCount) {
            delay = frames[n].delay
        }
        return delay
    }

    fun getFrameCount(): Int = frameCount

    fun setFrameCount(frameCount: Int) {
        this.frameCount = frameCount
    }

    private fun setPixels() {
        val dest = (image?.raster?.dataBuffer as DataBufferInt).data
        if (lastDispose > 0) {
            if (lastDispose == 3) {
                val n = frameCount - 2
                lastImage = if (n > 0) getFrame(n - 1) else null
            }
            lastImage?.let { last ->
                val prev = (last.raster.dataBuffer as DataBufferInt).data
                System.arraycopy(prev, 0, dest, 0, width * height)
                if (lastDispose == 2) {
                    val g = image!!.createGraphics()
                    val c = if (transparency) Color(0, 0, 0, 0) else Color(lastBgColor)
                    g.color = c
                    g.composite = AlphaComposite.Src
                    g.fill(lastRect)
                    g.dispose()
                }
            }
        }
        var pass = 1
        var inc = 8
        var iline = 0
        for (i in 0 until ih) {
            var line = i
            if (interlace) {
                if (iline >= ih) {
                    pass++
                    when (pass) {
                        2 -> iline = 4
                        3 -> {
                            iline = 2
                            inc = 4
                        }
                        4 -> {
                            iline = 1
                            inc = 2
                        }
                    }
                }
                line = iline
                iline += inc
            }
            line += iy
            if (line < height) {
                val k = line * width
                var dx = k + ix
                var dlim = dx + iw
                if (k + width < dlim) dlim = k + width
                var sx = i * iw
                while (dx < dlim) {
                    val index = pixels!![sx++].toInt() and 0xFF
                    val c = act!![index]
                    if (c != 0) dest[dx] = c
                    dx++
                }
            }
        }
    }

    fun getFrame(n: Int): BufferedImage? {
        return if (n in 0 until frameCount) frames[n].image else null
    }

    fun read(writable: Writable): Int {
        return runCatching {
            writable.toByteArray().inputStream().use(::read)
        }.onFailure { it.printStackTrace() }.getOrDefault(STATUS_OPEN_ERROR)
    }

    fun read(inputStream: InputStream?): Int {
        init()
        if (inputStream != null) {
            val bufferedInput = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
            input = bufferedInput
            readHeader()
            if (!err()) {
                readContents()
                if (frameCount < 0) status = STATUS_FORMAT_ERROR
            }
        } else status = STATUS_OPEN_ERROR
        runCatching { inputStream?.close() }
        return status
    }

    private fun decodeImageData() {
        val nullCode = -1
        val npix = iw * ih
        if (pixels == null || pixels!!.size < npix) pixels = ByteArray(npix)
        if (prefix == null) prefix = ShortArray(MAX_STACK_SIZE)
        if (suffix == null) suffix = ByteArray(MAX_STACK_SIZE)
        if (pixelStack == null) pixelStack = ByteArray(MAX_STACK_SIZE + 1)
        val dataSize = read()
        val clear = 1 shl dataSize
        val endOfInformation = clear + 1
        var available = clear + 2
        var oldCode = nullCode
        var codeSize = dataSize + 1
        var codeMask = (1 shl codeSize) - 1
        for (code in 0 until clear) {
            prefix!![code] = 0
            suffix!![code] = code.toByte()
        }
        var bi = 0
        var pi = 0
        var top = 0
        var first = 0
        var count = 0
        var bits = 0
        var datum = 0
        var i = 0
        while (i < npix) {
            if (top == 0) {
                if (bits < codeSize) {
                    if (count == 0) {
                        count = readBlock()
                        if (count <= 0) break
                        bi = 0
                    }
                    datum += (block[bi].toInt() and 0xFF) shl bits
                    bits += 8
                    bi++
                    count--
                    continue
                }
                var code = datum and codeMask
                datum = datum shr codeSize
                bits -= codeSize
                if (code > available || code == endOfInformation) break
                if (code == clear) {
                    codeSize = dataSize + 1
                    codeMask = (1 shl codeSize) - 1
                    available = clear + 2
                    oldCode = nullCode
                    continue
                }
                if (oldCode == nullCode) {
                    pixelStack!![top++] = suffix!![code]
                    oldCode = code
                    first = code
                    continue
                }
                val inCode = code
                if (code == available) {
                    pixelStack!![top++] = first.toByte()
                    code = oldCode
                }
                while (code > clear) {
                    pixelStack!![top++] = suffix!![code]
                    code = prefix!![code].toInt()
                }
                first = suffix!![code].toInt() and 0xFF
                if (available >= MAX_STACK_SIZE) {
                    pixelStack!![top++] = first.toByte()
                    continue
                }
                pixelStack!![top++] = first.toByte()
                prefix!![available] = oldCode.toShort()
                suffix!![available] = first.toByte()
                available++
                if (available and codeMask == 0 && available < MAX_STACK_SIZE) {
                    codeSize++
                    codeMask += available
                }
                oldCode = inCode
            }
            top--
            pixels!![pi++] = pixelStack!![top]
            i++
        }
        for (j in pi until npix) pixels!![j] = 0
    }

    private fun err(): Boolean = status != 0

    private fun init() {
        status = 0
        frameCount = 0
        frames = ArrayList()
        gct = null
        lct = null
    }

    private fun read(): Int {
        return runCatching {
            input?.read() ?: 0
        }.onFailure {
            status = STATUS_FORMAT_ERROR
        }.getOrDefault(0)
    }

    private fun readBlock(): Int {
        blockSize = read()
        var n = 0
        if (blockSize > 0) {
            runCatching {
                var count: Int
                while (n < blockSize) {
                    count = input?.read(block, n, blockSize - n) ?: -1
                    if (count == -1) break
                    n += count
                }
            }
            if (n < blockSize) status = STATUS_FORMAT_ERROR
        }
        return n
    }

    private fun readColorTable(ncolors: Int): IntArray? {
        val nbytes = 3 * ncolors
        var tab: IntArray? = null
        val c = ByteArray(nbytes)
        val n = runCatching {
            input?.read(c) ?: 0
        }.getOrDefault(0)

        if (n < nbytes) status = STATUS_FORMAT_ERROR
        else {
            tab = IntArray(256)
            var i = 0
            var j = 0
            while (i < ncolors) {
                val r = c[j++].toInt() and 0xFF
                val g = c[j++].toInt() and 0xFF
                val b = c[j++].toInt() and 0xFF
                tab[i++] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            }
        }
        return tab
    }

    private fun readContents() {
        var done = false
        while (!done && !err()) {
            val code = read()
            when (code) {
                0 -> continue
                0x21 -> {
                    when (read()) {
                        0xF9 -> readGraphicControlExt()
                        0xFF -> {
                            readBlock()
                            val app = StringBuilder()
                            repeat(11) { app.append(block[it].toInt().toChar()) }
                            if (app.toString() == "NETSCAPE2.0") readNetscapeExt() else skip()
                        }
                        else -> skip()
                    }
                }
                0x2C -> readImage()
                0x3B -> done = true
                else -> status = STATUS_FORMAT_ERROR
            }
        }
    }

    private fun readGraphicControlExt() {
        read()
        val packed = read()
        dispose = (packed and 0x1C) shr 2
        if (dispose == 0) dispose = 1
        transparency = (packed and 0x1) != 0
        delay = readShort() * 10
        transIndex = read()
        read()
    }

    private fun readHeader() {
        val id = StringBuilder()
        for (i in 0 until 6) id.append(read().toChar())
        if (!id.toString().startsWith("GIF")) {
            status = STATUS_FORMAT_ERROR
        } else {
            readLSD()
            if (gctFlag && !err()) {
                gct = readColorTable(gctSize)
                gct?.let { bgColor = it[bgIndex] }
            }
        }
    }

    private fun readImage() {
        ix = readShort()
        iy = readShort()
        iw = readShort()
        ih = readShort()
        val packed = read()
        lctFlag = (packed and 0x80) != 0
        interlace = (packed and 0x40) != 0
        lctSize = 2 shl (packed and 0x7)
        if (lctFlag) {
            lct = readColorTable(lctSize)
            act = lct
        } else {
            act = gct
            if (bgIndex == transIndex) bgColor = 0
        }
        var save = 0
        if (transparency) {
            act?.let { save = it[transIndex]; it[transIndex] = 0 }
        }
        if (act == null) status = STATUS_FORMAT_ERROR
        if (!err()) {
            decodeImageData()
            skip()
            if (!err()) {
                frameCount++
                image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                setPixels()
                frames.add(GifFrame(image!!, delay))
                if (transparency) act?.let { it[transIndex] = save }
                resetFrame()
            }
        }
    }

    private fun readLSD() {
        width = readShort()
        height = readShort()
        val packed = read()
        gctFlag = (packed and 0x80) != 0
        gctSize = 2 shl (packed and 0x7)
        bgIndex = read()
        pixelAspect = read()
    }

    private fun readNetscapeExt() {
        do {
            readBlock()
            if (block[0] != 1.toByte()) continue
            val b1 = block[1].toInt() and 0xFF
            val b2 = block[2].toInt() and 0xFF
            loopCount = (b2 shl 8) or b1
        } while (blockSize > 0 && !err())
    }

    private fun readShort(): Int = read() or (read() shl 8)

    private fun resetFrame() {
        lastDispose = dispose
        lastRect = Rectangle(ix, iy, iw, ih)
        lastImage = image
        lastBgColor = bgColor
        dispose = 0
        transparency = false
        delay = 0
        lct = null
    }

    private fun skip() {
        do {
            readBlock()
        } while (blockSize > 0 && !err())
    }

    data class GifFrame(val image: BufferedImage, val delay: Int)
}