package com.jnetai.thermal.util;

import android.graphics.Bitmap;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;

public class ImageEncoder {
    private static final String COMPONENT = "ImageEncoder";

    /**
     * Scale a bitmap down to a target width (in pixels) preserving aspect ratio.
     */
    public static Bitmap scaleToWidth(Bitmap src, int targetWidth) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= targetWidth) return src;
        float ratio = (float) targetWidth / w;
        int newH = Math.max(1, Math.round(h * ratio));
        try {
            return Bitmap.createScaledBitmap(src, targetWidth, newH, true);
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_006, COMPONENT, "scaleToWidth", e,
                    "src=" + w + "x" + h + " target=" + targetWidth);
            return src;
        }
    }

    public static Bitmap rotate(Bitmap src, float degrees) {
        if (src == null) return null;
        android.graphics.Matrix m = new android.graphics.Matrix();
        m.postRotate(degrees);
        try {
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_006, COMPONENT, "rotate", e, "degrees=" + degrees);
            return src;
        }
    }

    /**
     * Convert a bitmap to a 1bpp boolean array. Black (dark) pixels become true.
     * darkness 0-100: higher = threshold lower = more black.
     */
    public static boolean[] toBinary(Bitmap bmp, int darkness) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        boolean[] pix = new boolean[w * h];
        int[] raw = new int[w * h];
        bmp.getPixels(raw, 0, w, 0, 0, w, h);
        int threshold = 255 - (int) (255 * (darkness / 100.0f));
        for (int i = 0; i < raw.length; i++) {
            int c = raw[i];
            int r = (c >> 16) & 0xFF;
            int g = (c >> 8) & 0xFF;
            int b = c & 0xFF;
            int lum = (r * 299 + g * 587 + b * 114) / 1000;
            pix[i] = lum < threshold;
        }
        return pix;
    }
}