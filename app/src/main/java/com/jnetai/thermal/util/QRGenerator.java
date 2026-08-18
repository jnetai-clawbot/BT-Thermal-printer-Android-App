package com.jnetai.thermal.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import java.util.HashMap;
import java.util.Map;

public class QRGenerator {
    private static final String COMPONENT = "QRGenerator";

    public static Bitmap generateQr(String content, int sizePx) throws WriterException {
        if (content == null || content.isEmpty()) {
            throw new WriterException("Empty content");
        }
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
            int w = matrix.getWidth();
            int h = matrix.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            bmp.setPixels(pixels, 0, w, 0, 0, w, h);
            Diagnostics.info(COMPONENT, "generateQr", "Generated " + sizePx + "px QR for " + content.length() + " chars");
            return bmp;
        } catch (WriterException e) {
            Diagnostics.log(ErrorCodes.QR_001, COMPONENT, "generateQr", e, "contentLen=" + content.length());
            throw e;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.QR_001, COMPONENT, "generateQr", e, "contentLen=" + content.length());
            throw new WriterException(e.getMessage());
        }
    }

    /** Estimate the number of black pixels across one row to compute printed width. */
    public static int qrBitmapWidthForSize(String content, int moduleSize, int printerDotsPerMm) {
        int sizePx = Math.max(120, content.length() * 5);
        try {
            Bitmap bmp = generateQr(content, sizePx);
            int modules = (int) Math.round(Math.sqrt(sizePx * sizePx / (0.5 + content.length() * 0.5)));
            return modules * moduleSize;
        } catch (Exception e) {
            return moduleSize * 10;
        }
    }
}