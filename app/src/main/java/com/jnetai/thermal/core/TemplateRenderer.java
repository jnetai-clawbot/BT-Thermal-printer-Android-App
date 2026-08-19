package com.jnetai.thermal.core;

import android.graphics.Bitmap;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.ReceiptData;
import com.jnetai.thermal.model.Template;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.ImageEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TemplateRenderer {
    private static final String COMPONENT = "TemplateRenderer";
    private static final int DOTS_PER_MM = 8;

    public static byte[] render(ContextCtx ctx, Template t, ReceiptData data) {
        SettingsStore settings = SettingsStore.getInstance(ctx.context);
        String charset = settings.getCharset();
        boolean landscape = settings.isLandscape();

        int widthMm = t.isLabel ? t.labelWidthMm : t.widthMm;
        if (widthMm <= 0) widthMm = 58;
        int widthDots = widthMm * DOTS_PER_MM;

        try {
            EscPos e = new EscPos();
            e.init();
            e.setCodePage(charset.equals(EscPos.ENC_UTF8) ? 0 : 2);
            e.setPrintAreaWidth(widthDots);

            if (t.feedBefore > 0) e.feedLines(t.feedBefore);

            if (t.logoEnabled) {
                Bitmap logo = ctx.logoProvider != null ? ctx.logoProvider.provide() : null;
                if (logo != null) {
                    Bitmap scaled = ImageEncoder.scaleToWidth(logo, widthDots);
                    if (landscape) scaled = ImageEncoder.rotate(scaled, 90);
                    int lw = scaled.getWidth();
                    int lh = scaled.getHeight();
                    boolean[] pix = ImageEncoder.toBinary(scaled, settings.getDensity());
                    e.setAlignment(1);
                    e.rasterImage(lw, lh, pix, 0);
                    e.feedLines(1);
                    e.setAlignment(0);
                } else {
                    Diagnostics.info(COMPONENT, "render", "Logo bitmap unavailable for " + t.name);
                }
            }

            if (t.isLabel) {
                renderLabel(e, t, data, charset);
            } else {
                renderLines(e, t, data, charset, widthDots);
            }

            if (t.feedAfter > 0) e.feedLines(t.feedAfter);
            if (settings.isCutPaper()) e.cut(1);
            e.feedLines(2);
            return e.bytes();
        } catch (Exception ex) {
            Diagnostics.log(ErrorCodes.PR_003, COMPONENT, "render", ex, "Template=" + t.name);
            throw new RuntimeException(ex);
        }
    }

    private static void renderLabel(EscPos e, Template t, ReceiptData data, String charset) {
        for (Template.TemplateLine line : t.lines) {
            if ("spacer".equals(line.kind)) {
                e.feedLines(line.spacerCount);
            } else if ("items".equals(line.kind)) {
                if (data.header != null && !data.header.isEmpty()) {
                    e.setAlignment(line.align);
                    e.write(data.header, charset);
                    e.newline();
                }
                for (ReceiptData.ReceiptItem item : data.items) {
                    e.setAlignment(line.align);
                    e.write(item.name, charset);
                    e.newline();
                    if (!"1".equals(item.qty) && !"1.0".equals(item.qty)) {
                        e.setSize(1, 1);
                        e.write("Qty: " + item.qty, charset);
                        e.newline();
                    }
                }
            } else {
                String text = substitute(line.text, t, data);
                if (text == null || text.isEmpty()) continue;
                applyLineStyle(e, line);
                e.setAlignment(line.align);
                e.write(text, charset);
                e.newline();
                resetStyle(e);
            }
        }
    }

    private static void renderLines(EscPos e, Template t, ReceiptData data, String charset, int widthDots) {
        for (Template.TemplateLine line : t.lines) {
            if ("spacer".equals(line.kind)) {
                e.feedLines(line.spacerCount);
                continue;
            }
            if ("items".equals(line.kind)) {
                renderItemBlock(e, data, charset, widthDots);
                continue;
            }
            String text = substitute(line.text, t, data);
            if (text == null || text.isEmpty()) continue;

            if (line.dash) {
                e.setAlignment(line.align);
                e.setSize(1, 1);
                e.write("--------------------------------", charset);
                e.newline();
                continue;
            }
            applyLineStyle(e, line);
            e.setAlignment(line.align);
            e.write(text, charset);
            e.newline();
            resetStyle(e);
        }
    }

    private static void applyLineStyle(EscPos e, Template.TemplateLine line) {
        if (line.size <= 1) e.setSize(1, 1);
        else if (line.size == 2) e.setSize(2, 2);
        else if (line.size == 3) e.setSize(3, 3);
        else e.setSize(4, 4);
        e.setBold(line.bold);
        if (line.underline) e.setUnderline(1);
    }

    private static void resetStyle(EscPos e) {
        e.setBold(false);
        e.setUnderline(0);
        e.setSize(1, 1);
    }

    private static void renderItemBlock(EscPos e, ReceiptData data, String charset, int widthDots) {
        int cols = Math.max(20, widthDots / 12);
        e.setSize(1, 1);
        e.setBold(true);
        e.setUnderline(0);
        e.setAlignment(0);
        e.write(padRight("Item", cols - 16) + padLeft("Qty", 4) + " " + padLeft("Price", 11), charset);
        e.newline();
        e.write("--------------------------------", charset);
        e.newline();
        e.setBold(false);
        for (ReceiptData.ReceiptItem item : data.items) {
            String name = item.name == null ? "" : item.name;
            String qty = item.qty == null ? "1" : item.qty;
            String price = String.format(Locale.US, "%.2f", item.price);
            String left = name.length() > cols - 16 ? name.substring(0, Math.max(0, cols - 16)) : name;
            e.write(padRight(left, cols - 16) + padLeft(qty, 4) + " " + padLeft(price, 11), charset);
            e.newline();
        }
    }

    private static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    private static String padLeft(String s, int n) {
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n - s.length(); i++) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    private static String substitute(String text, Template t, ReceiptData data) {
        if (text == null) return null;
        String now = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());
        String nowTime = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
        String res = text;
        if (data != null) {
            res = res.replace("{store}", safe(data.storeName));
            res = res.replace("{header}", safe(data.header));
            res = res.replace("{footer}", safe(data.footer));
            res = res.replace("{number}", safe(data.number));
            res = res.replace("{cashier}", safe(data.cashier));
            res = res.replace("{title}", safe(t.title));
            res = res.replace("{subtotal}", money(data.subtotal));
            res = res.replace("{tax}", money(data.tax));
            res = res.replace("{total}", money(data.total));
            res = res.replace("{cash}", money(data.tendered));
            res = res.replace("{change}", money(data.change));
            res = res.replace("{qty}", String.valueOf(data.items.size()));
            if (res.contains("{items}")) {
                res = res.replace("{items}", itemsBlockText(data, t));
            }
        }
        res = res.replace("{date}", now);
        res = res.replace("{time}", nowTime);
        return res;
    }

    public static String previewText(Template t, ReceiptData data) {
        StringBuilder sb = new StringBuilder();
        int col = t.isLabel ? Math.max(20, t.labelWidthMm * 8 / 12) : Math.max(20, t.widthMm * 8 / 12);
        for (Template.TemplateLine line : t.lines) {
            if ("spacer".equals(line.kind)) {
                for (int i = 0; i < line.spacerCount; i++) sb.append("\n");
                continue;
            }
            if ("items".equals(line.kind)) {
                sb.append(padRight("Item", col - 16)).append(padLeft("Qty", 4)).append(" ").append(padLeft("Price", 11)).append("\n");
                sb.append("--------------------------------\n");
                for (ReceiptData.ReceiptItem item : data.items) {
                    if (t.isLabel) {
                        sb.append(item.name).append("\n");
                        if (!"1".equals(item.qty) && !"1.0".equals(item.qty)) sb.append("Qty: ").append(item.qty).append("\n");
                    } else {
                        String name = item.name == null ? "" : item.name;
                        String left = name.length() > col - 16 ? name.substring(0, Math.max(0, col - 16)) : name;
                        sb.append(padRight(left, col - 16)).append(padLeft(item.qty == null ? "1" : item.qty, 4))
                                .append(" ").append(padLeft(money(item.price), 11)).append("\n");
                    }
                }
                continue;
            }
            String text = substitute(line.text, t, data);
            if (text == null || text.isEmpty()) continue;
            if (line.dash) {
                sb.append(centerOrAlign("--------------------------------", col, line.align)).append("\n");
                continue;
            }
            sb.append(centerOrAlign(text, col, line.align)).append("\n");
        }
        if (t.logoEnabled) {
            sb.insert(0, "[LOGO]\n");
        }
        return sb.toString();
    }

    private static String centerOrAlign(String s, int col, int align) {
        if (s.length() >= col) return s;
        int pad = col - s.length();
        if (align == 1) {
            int left = pad / 2;
            return padStrs(" ", left) + s + padStrs(" ", pad - left);
        } else if (align == 2) {
            return padStrs(" ", pad) + s;
        }
        return s + padStrs(" ", pad);
    }

    private static String padStrs(String c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String money(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String itemsBlockText(ReceiptData data, Template t) {
        StringBuilder sb = new StringBuilder();
        int col = t.isLabel ? Math.max(20, t.labelWidthMm * 8 / 12) : Math.max(20, t.widthMm * 8 / 12);
        if (data.items.isEmpty()) {
            return t.isLabel ? "" : "";
        }
        if (!t.isLabel) {
            sb.append(padRight("Item", col - 16)).append(padLeft("Qty", 4)).append(" ").append(padLeft("Price", 11)).append("\n");
            sb.append("--------------------------------\n");
        }
        for (ReceiptData.ReceiptItem item : data.items) {
            if (t.isLabel) {
                sb.append(item.name == null ? "" : item.name).append("\n");
                if (!"1".equals(item.qty) && !"1.0".equals(item.qty)) sb.append("Qty: ").append(item.qty).append("\n");
            } else {
                String name = item.name == null ? "" : item.name;
                String left = name.length() > col - 16 ? name.substring(0, Math.max(0, col - 16)) : name;
                sb.append(padRight(left, col - 16)).append(padLeft(item.qty == null ? "1" : item.qty, 4))
                        .append(" ").append(padLeft(money(item.price), 11)).append("\n");
            }
        }
        return sb.toString();
    }

    public interface LogoProvider {
        Bitmap provide();
    }

    public static class ContextCtx {
        public final android.content.Context context;
        public final LogoProvider logoProvider;

        public ContextCtx(android.content.Context context, LogoProvider logoProvider) {
            this.context = context;
            this.logoProvider = logoProvider;
        }
    }
}