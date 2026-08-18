package com.jnetai.thermal.store;

import android.content.Context;
import android.content.SharedPreferences;
import com.jnetai.thermal.core.EscPos;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;

public class SettingsStore {
    private static final String PREF_NAME = "jnet_thermal_settings";
    private static final String COMPONENT = "SettingsStore";

    private static SettingsStore instance;
    private final SharedPreferences prefs;

    public static synchronized SettingsStore getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsStore(context.getApplicationContext());
        }
        return instance;
    }

    private SettingsStore(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private void logSaveError(String key, Exception e) {
        if (e != null) {
            Diagnostics.log(ErrorCodes.ST_002, COMPONENT, "save", e, "Key=" + key);
        } else {
            Diagnostics.info(COMPONENT, "save", "Saved key=" + key);
        }
    }

    public String getPrinterAddress() { return prefs.getString("printer_address", ""); }
    public void setPrinterAddress(String v) {
        try { prefs.edit().putString("printer_address", v).apply(); logSaveError("printer_address", null); }
        catch (Exception e) { logSaveError("printer_address", e); }
    }

    public String getPrinterName() { return prefs.getString("printer_name", "PT210 Thermal Printer"); }
    public void setPrinterName(String v) {
        try { prefs.edit().putString("printer_name", v).apply(); logSaveError("printer_name", null); }
        catch (Exception e) { logSaveError("printer_name", e); }
    }

    public int getPaperWidthMm() { return prefs.getInt("paper_width_mm", 58); }
    public void setPaperWidthMm(int v) {
        try { prefs.edit().putInt("paper_width_mm", v).apply(); logSaveError("paper_width_mm", null); }
        catch (Exception e) { logSaveError("paper_width_mm", e); }
    }

    public int getDensity() { return prefs.getInt("density", 60); }
    public void setDensity(int v) {
        try { prefs.edit().putInt("density", Math.max(0, Math.min(100, v))).apply(); logSaveError("density", null); }
        catch (Exception e) { logSaveError("density", e); }
    }

    public int getFontSize() { return prefs.getInt("font_size", 1); }
    public void setFontSize(int v) {
        try { prefs.edit().putInt("font_size", v).apply(); logSaveError("font_size", null); }
        catch (Exception e) { logSaveError("font_size", e); }
    }

    public boolean isBold() { return prefs.getBoolean("bold", true); }
    public void setBold(boolean v) {
        try { prefs.edit().putBoolean("bold", v).apply(); logSaveError("bold", null); }
        catch (Exception e) { logSaveError("bold", e); }
    }

    public boolean isUnderline() { return prefs.getBoolean("underline", false); }
    public void setUnderline(boolean v) {
        try { prefs.edit().putBoolean("underline", v).apply(); logSaveError("underline", null); }
        catch (Exception e) { logSaveError("underline", e); }
    }

    public int getAlignment() { return prefs.getInt("alignment", 0); }
    public void setAlignment(int v) {
        try { prefs.edit().putInt("alignment", v).apply(); logSaveError("alignment", null); }
        catch (Exception e) { logSaveError("alignment", e); }
    }

    public boolean isLandscape() { return prefs.getBoolean("landscape", false); }
    public void setLandscape(boolean v) {
        try { prefs.edit().putBoolean("landscape", v).apply(); logSaveError("landscape", null); }
        catch (Exception e) { logSaveError("landscape", e); }
    }

    public int getCopies() { return Math.max(1, prefs.getInt("copies", 1)); }
    public void setCopies(int v) {
        try { prefs.edit().putInt("copies", Math.max(1, Math.min(9, v))).apply(); logSaveError("copies", null); }
        catch (Exception e) { logSaveError("copies", e); }
    }

    public int getFeedBefore() { return prefs.getInt("feed_before", 2); }
    public void setFeedBefore(int v) {
        try { prefs.edit().putInt("feed_before", Math.max(0, Math.min(20, v))).apply(); logSaveError("feed_before", null); }
        catch (Exception e) { logSaveError("feed_before", e); }
    }

    public int getFeedAfter() { return prefs.getInt("feed_after", 4); }
    public void setFeedAfter(int v) {
        try { prefs.edit().putInt("feed_after", Math.max(0, Math.min(30, v))).apply(); logSaveError("feed_after", null); }
        catch (Exception e) { logSaveError("feed_after", e); }
    }

    public boolean isCutPaper() { return prefs.getBoolean("cut_paper", true); }
    public void setCutPaper(boolean v) {
        try { prefs.edit().putBoolean("cut_paper", v).apply(); logSaveError("cut_paper", null); }
        catch (Exception e) { logSaveError("cut_paper", e); }
    }

    public String getCharset() { return prefs.getString("charset", EscPos.ENC_UTF8); }
    public void setCharset(String v) {
        try { prefs.edit().putString("charset", v).apply(); logSaveError("charset", null); }
        catch (Exception e) { logSaveError("charset", e); }
    }

    public boolean isQrRasterMode() { return prefs.getBoolean("qr_raster", true); }
    public void setQrRasterMode(boolean v) {
        try { prefs.edit().putBoolean("qr_raster", v).apply(); logSaveError("qr_raster", null); }
        catch (Exception e) { logSaveError("qr_raster", e); }
    }

    public int getQrModuleSize() { return prefs.getInt("qr_module", 3); }
    public void setQrModuleSize(int v) {
        try { prefs.edit().putInt("qr_module", Math.max(2, Math.min(8, v))).apply(); logSaveError("qr_module", null); }
        catch (Exception e) { logSaveError("qr_module", e); }
    }

    public int getLabelWidthMm() { return prefs.getInt("label_width_mm", 58); }
    public void setLabelWidthMm(int v) {
        try { prefs.edit().putInt("label_width_mm", Math.max(20, Math.min(112, v))).apply(); logSaveError("label_width_mm", null); }
        catch (Exception e) { logSaveError("label_width_mm", e); }
    }

    public int getLabelHeightMm() { return prefs.getInt("label_height_mm", 40); }
    public void setLabelHeightMm(int v) {
        try { prefs.edit().putInt("label_height_mm", Math.max(10, Math.min(200, v))).apply(); logSaveError("label_height_mm", null); }
        catch (Exception e) { logSaveError("label_height_mm", e); }
    }

    public boolean isShowPrintDialog() { return prefs.getBoolean("show_print_dialog", true); }
    public void setShowPrintDialog(boolean v) {
        try { prefs.edit().putBoolean("show_print_dialog", v).apply(); logSaveError("show_print_dialog", null); }
        catch (Exception e) { logSaveError("show_print_dialog", e); }
    }

    public int getRepeatIntervalSec() { return prefs.getInt("repeat_interval", 0); }
    public void setRepeatIntervalSec(int v) {
        try { prefs.edit().putInt("repeat_interval", Math.max(0, Math.min(600, v))).apply(); logSaveError("repeat_interval", null); }
        catch (Exception e) { logSaveError("repeat_interval", e); }
    }
}