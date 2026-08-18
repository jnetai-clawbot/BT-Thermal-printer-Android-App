package com.jnetai.thermal.core;

import android.content.Context;
import android.graphics.Bitmap;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.ReceiptData;
import com.jnetai.thermal.model.Template;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.ImageEncoder;
import com.jnetai.thermal.util.QRGenerator;

public class PrintManager {
    private static final String COMPONENT = "PrintManager";

    public enum PrintResult {
        SUCCESS,
        NOT_CONNECTED,
        CONNECTION_FAILED,
        NO_PRINTER,
        FAILED
    }

    private final Context context;
    private final BluetoothHelper bt;

    public PrintManager(Context context, BluetoothHelper bt) {
        this.context = context.getApplicationContext();
        this.bt = bt;
    }

    public PrintResult printTemplate(Template t, ReceiptData data, Bitmap logo) {
        try {
            TemplateRenderer.ContextCtx ctx = new TemplateRenderer.ContextCtx(context,
                    () -> logo != null ? logo : null);
            byte[] bytes = TemplateRenderer.render(ctx, t, data);
            return sendBytes(bytes);
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_003, COMPONENT, "printTemplate", e, "Template=" + t.name);
            return PrintResult.FAILED;
        }
    }

    public PrintResult printText(String text) {
        try {
            SettingsStore s = SettingsStore.getInstance(context);
            EscPos e = new EscPos();
            e.init();
            e.setCodePage(s.getCharset().equals(EscPos.ENC_UTF8) ? 0 : 2);
            e.setAlignment(s.getAlignment());
            if (s.isBold()) e.setBold(true);
            if (s.isUnderline()) e.setUnderline(1);
            switch (s.getFontSize()) {
                case 2: e.setSize(2, 2); break;
                case 3: e.setSize(3, 3); break;
                default: e.setSize(1, 1);
            }
            String[] lines = text.split("\n", -1);
            for (String line : lines) {
                e.write(line, s.getCharset());
                e.newline();
            }
            if (s.getFeedAfter() > 0) e.feedLines(s.getFeedAfter());
            if (s.isCutPaper()) e.cut(1);
            e.feedLines(2);
            return sendBytes(e.bytes());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_003, COMPONENT, "printText", e, "textLen=" + (text == null ? 0 : text.length()));
            return PrintResult.FAILED;
        }
    }

    public PrintResult printImage(Bitmap bmp) {
        try {
            SettingsStore s = SettingsStore.getInstance(context);
            int widthMm = s.isLandscape() ? s.getPaperWidthMm() : s.getPaperWidthMm();
            int widthDots = widthMm * 8;
            Bitmap rotated = bmp;
            boolean landscape = s.isLandscape();
            boolean scaled = false;
            if (!landscape) {
                rotated = ImageEncoder.scaleToWidth(bmp, widthDots);
            } else {
                rotated = ImageEncoder.rotate(bmp, 90);
                rotated = ImageEncoder.scaleToWidth(rotated, widthDots);
            }
            boolean[] pix = ImageEncoder.toBinary(rotated, s.getDensity());
            int w = rotated.getWidth();
            int h = rotated.getHeight();
            EscPos e = new EscPos();
            e.init();
            e.setPrintAreaWidth(widthDots);
            e.setAlignment(1);
            e.rasterImage(w, h, pix, 0);
            if (s.getFeedAfter() > 0) e.feedLines(s.getFeedAfter());
            if (s.isCutPaper()) e.cut(1);
            e.feedLines(2);
            Diagnostics.info(COMPONENT, "printImage", "width=" + w + " height=" + h + " dots, density=" + s.getDensity());
            return sendBytes(e.bytes());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_006, COMPONENT, "printImage", e, "Image print failed");
            return PrintResult.FAILED;
        }
    }

    public PrintResult printQR(String content) {
        try {
            SettingsStore s = SettingsStore.getInstance(context);
            EscPos e = new EscPos();
            e.init();
            e.setAlignment(1);
            if (s.isQrRasterMode()) {
                int sizePx = Math.max(200, Math.min(600, content.length() * 6 + 200));
                Bitmap qr = QRGenerator.generateQr(content, sizePx);
                int widthDots = s.getPaperWidthMm() * 8;
                Bitmap scaled = ImageEncoder.scaleToWidth(qr, Math.min(widthDots, 384));
                boolean[] pix = ImageEncoder.toBinary(scaled, Math.max(s.getDensity(), 50));
                e.rasterImage(scaled.getWidth(), scaled.getHeight(), pix, 0);
            } else {
                e.qrEscPos(content, s.getQrModuleSize(), 0x31);
            }
            if (s.getFeedAfter() > 0) e.feedLines(s.getFeedAfter());
            if (s.isCutPaper()) e.cut(1);
            e.feedLines(2);
            e.setAlignment(0);
            Diagnostics.info(COMPONENT, "printQR", "mode=" + (s.isQrRasterMode() ? "raster" : "escpos"));
            return sendBytes(e.bytes());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.QR_002, COMPONENT, "printQR", e, "contentLen=" + (content == null ? 0 : content.length()));
            return PrintResult.FAILED;
        }
    }

    public PrintResult testPattern() {
        try {
            SettingsStore s = SettingsStore.getInstance(context);
            EscPos e = new EscPos();
            e.init();
            e.feedLines(1);
            e.setAlignment(1);
            e.setSize(2, 2);
            e.setBold(true);
            e.write("PRINTER TEST", s.getCharset());
            e.newline();
            e.setSize(1, 1);
            e.setBold(false);
            e.write("Bluetooth Thermal Printer", s.getCharset());
            e.newline();
            e.write("PT210 - " + s.getPrinterName(), s.getCharset());
            e.newline();
            e.setAlignment(0);
            e.write("--------------------------------", s.getCharset());
            e.newline();
            e.write("ASCII : !\"#$%&'()*+,-./ 0123456789", s.getCharset());
            e.newline();
            e.write("Letters: ABCDEFGHIJKLMNOPQRSTUVWXYZ", s.getCharset());
            e.newline();
            e.write("Lower  : abcdefghijklmnopqrstuvwxyz", s.getCharset());
            e.newline();
            e.write("Symbols: @^_`{|}~ [];:<>? /\\=+", s.getCharset());
            e.newline();
            e.setAlignment(0);
            e.write("--------------------------------", s.getCharset());
            e.newline();
            e.setAlignment(1);
            e.setSize(2, 2);
            e.write("OK", s.getCharset());
            e.newline();
            e.setSize(1, 1);
            e.setAlignment(0);
            e.write("Printed: " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.US)
                    .format(new java.util.Date()), s.getCharset());
            e.newline();
            Bitmap qr = QRGenerator.generateQr("J~Net Thermal Printer Test " + System.currentTimeMillis(), 240);
            boolean[] pix = ImageEncoder.toBinary(ImageEncoder.scaleToWidth(qr, 240), 50);
            e.setAlignment(1);
            e.rasterImage(240, 240, pix, 0);
            e.setAlignment(0);
            if (s.getFeedAfter() > 0) e.feedLines(s.getFeedAfter());
            if (s.isCutPaper()) e.cut(1);
            e.feedLines(2);
            return sendBytes(e.bytes());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.PR_004, COMPONENT, "testPattern", e, "Test print failed");
            return PrintResult.FAILED;
        }
    }

    private PrintResult sendBytes(byte[] bytes) {
        SettingsStore s = SettingsStore.getInstance(context);
        int copies = s.getCopies();
        if (copies < 1) copies = 1;
        for (int i = 0; i < copies; i++) {
            if (!bt.isConnected()) {
                String addr = s.getPrinterAddress();
                if (addr == null || addr.isEmpty()) {
                    Diagnostics.log(ErrorCodes.PR_005, COMPONENT, "sendBytes", "No printer selected");
                    return PrintResult.NO_PRINTER;
                }
                boolean ok = bt.connect(addr);
                if (!ok) {
                    Diagnostics.log(ErrorCodes.PR_002, COMPONENT, "sendBytes", "Reconnect failed for " + addr);
                    return PrintResult.CONNECTION_FAILED;
                }
            }
            boolean ok = bt.write(bytes);
            if (!ok) {
                return PrintResult.NOT_CONNECTED;
            }
        }
        Diagnostics.info(COMPONENT, "sendBytes", "Print sent (" + copies + "x) bytes=" + bytes.length);
        return PrintResult.SUCCESS;
    }

    public void disconnect() {
        bt.close();
    }
}