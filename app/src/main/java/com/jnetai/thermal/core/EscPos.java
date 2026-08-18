package com.jnetai.thermal.core;

import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * ESC/POS command builder for thermal printers such as the PT210.
 * All commands are built as raw bytes and written to the printer output stream.
 */
public class EscPos {
    public static final String ENC_UTF8 = "UTF-8";
    public static final String ENC_CP437 = "Cp437";
    public static final String ENC_CP850 = "Cp850";
    public static final String ENC_WIN1252 = "windows-1252";
    public static final String ENC_GBK = "GBK";

    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;

    private final ByteArrayOutputStream out;

    public EscPos() {
        out = new ByteArrayOutputStream();
    }

    public EscPos init() {
        out.write(ESC); out.write(0x40);
        return this;
    }

    public EscPos feedLines(int n) {
        out.write(ESC); out.write(0x64); out.write(n & 0xFF);
        return this;
    }

    public EscPos feed(int n) {
        out.write(ESC); out.write(0x4A); out.write(n & 0xFF);
        return this;
    }

    public EscPos setLineSpacing(int n) {
        out.write(ESC); out.write(0x33); out.write(n & 0xFF);
        return this;
    }

    public EscPos cut(int mode) {
        out.write(GS); out.write(0x56); out.write(mode & 0xFF);
        return this;
    }

    public EscPos setAlignment(int align) {
        out.write(ESC); out.write(0x61); out.write(align & 0xFF);
        return this;
    }

    /**
     * Set character size via GS ! n. low nibble = height multiplier, high nibble = width multiplier.
     */
    public EscPos setSize2x2() {
        out.write(GS); out.write(0x21); out.write(0x22);
        return this;
    }

    public EscPos setSize3x3() {
        out.write(GS); out.write(0x21); out.write(0x33);
        return this;
    }

    public EscPos setSize1x1() {
        out.write(GS); out.write(0x21); out.write(0x11);
        return this;
    }

    public EscPos setSizeOnlyWidth2() {
        out.write(GS); out.write(0x21); out.write(0x12);
        return this;
    }

    public EscPos setSizeOnlyHeight2() {
        out.write(GS); out.write(0x21); out.write(0x21);
        return this;
    }

    public EscPos setSize(int w, int h) {
        int val = ((w & 0x0F) << 4) | (h & 0x0F);
        out.write(GS); out.write(0x21); out.write(val);
        return this;
    }

    public EscPos setBold(boolean on) {
        out.write(ESC); out.write(0x45); out.write(on ? 1 : 0);
        return this;
    }

    public EscPos setUnderline(int mode) {
        out.write(ESC); out.write(0x2D); out.write(mode & 0xFF);
        return this;
    }

    public EscPos setInvert(boolean on) {
        out.write(GS); out.write(0x42); out.write(on ? 1 : 0);
        return this;
    }

    public EscPos setDoubleStrike(boolean on) {
        out.write(ESC); out.write(0x47); out.write(on ? 1 : 0);
        return this;
    }

    public EscPos setCodePage(int page) {
        out.write(ESC); out.write(0x74); out.write(page & 0xFF);
        return this;
    }

    public EscPos setLeftMargin(int n) {
        out.write(GS); out.write(0x4C); out.write(n & 0xFF); out.write((n >> 8) & 0xFF);
        return this;
    }

    public EscPos setPrintAreaWidth(int n) {
        out.write(GS); out.write(0x57); out.write(n & 0xFF); out.write((n >> 8) & 0xFF);
        return this;
    }

    public EscPos beep(int n) {
        out.write(ESC); out.write(0x42); out.write(0x18); out.write(0xFF); out.write(n);
        return this;
    }

    /** Write a string using the given character set. */
    public EscPos write(String text, String charset) {
        try {
            out.write(encode(text, charset));
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_003, "EscPos", "write", e, "Encoding failed for charset " + charset);
            try {
                out.write(text.getBytes("UTF-8"));
            } catch (Exception ex) {
                out.write(0);
            }
        }
        return this;
    }

    public EscPos newline() {
        out.write(0x0A);
        return this;
    }

    public EscPos raw(byte[] data) {
        out.write(data, 0, data.length);
        return this;
    }

    public EscPos raw(int b) {
        out.write(b);
        return this;
    }

    /**
     * Print a raster image using GS v 0.
     * bitmap is a 1-bit-per-pixel array: width*height booleans (true = black dot).
     */
    public EscPos rasterImage(int width, int height, boolean[] pix, int mult) {
        int xBytes = (width + 7) / 8;
        int xL = xBytes & 0xFF;
        int xH = (xBytes >> 8) & 0xFF;
        int yL = height & 0xFF;
        int yH = (height >> 8) & 0xFF;
        out.write(GS); out.write(0x76); out.write(0x30);
        out.write(mult & 0xFF);
        out.write(xL); out.write(xH); out.write(yL); out.write(yH);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x += 8) {
                int b = 0;
                for (int i = 0; i < 8; i++) {
                    if (x + i < width && pix[y * width + x + i]) {
                        b |= (0x80 >> i);
                    }
                }
                out.write(b);
            }
        }
        return this;
    }

    /** Print a QR code natively via the printer's ESC/POS QR command (GS ( k). */
    public EscPos qrEscPos(String text, int moduleSize, int errorLevel) {
        out.write(GS); out.write(0x28); out.write(0x6B);
        out.write(0x04); out.write(0x00); out.write(0x31); out.write(0x41); out.write(0x32); out.write(0x00);
        out.write(GS); out.write(0x28); out.write(0x6B);
        out.write(0x03); out.write(0x00); out.write(0x31); out.write(0x43); out.write(moduleSize & 0xFF);
        out.write(GS); out.write(0x28); out.write(0x6B);
        out.write(0x03); out.write(0x00); out.write(0x31); out.write(0x45); out.write(errorLevel & 0xFF);
        byte[] data = text.getBytes(Charset.forName("ISO-8859-1"));
        int len = data.length + 3;
        out.write(GS); out.write(0x28); out.write(0x6B);
        out.write(len & 0xFF); out.write((len >> 8) & 0xFF); out.write(0x31); out.write(0x50); out.write(0x30);
        out.write(data, 0, data.length);
        out.write(GS); out.write(0x28); out.write(0x6B);
        out.write(0x03); out.write(0x00); out.write(0x31); out.write(0x51); out.write(0x30);
        return this;
    }

    /** CODE128 barcode via GS k 73. */
    public EscPos barcodeCode128(String data) {
        return barcode(0x49, data);
    }

    /** CODE39 barcode via GS k 69. */
    public EscPos barcodeCode39(String data) {
        return barcode(0x45, data);
    }

    private EscPos barcode(int type, String data) {
        byte[] b = data.getBytes(Charset.forName("ISO-8859-1"));
        if (b.length > 255) b = java.util.Arrays.copyOf(b, 255);
        out.write(GS); out.write(0x6B);
        out.write(type); out.write(b.length); out.write(b, 0, b.length);
        return this;
    }

    public byte[] bytes() {
        return out.toByteArray();
    }

    private byte[] encode(String text, String charset) throws UnsupportedEncodingException {
        if (charset == null || charset.isEmpty()) charset = ENC_UTF8;
        return text.getBytes(charset);
    }
}