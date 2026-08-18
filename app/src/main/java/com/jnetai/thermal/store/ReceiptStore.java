package com.jnetai.thermal.store;

import android.content.ContentValues;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.ReceiptData;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReceiptStore {
    private static final String COMPONENT = "ReceiptStore";
    private static ReceiptStore instance;
    private final Context context;
    private final Gson gson;
    private final File receiptsDir;

    public static synchronized ReceiptStore getInstance(Context context) {
        if (instance == null) {
            instance = new ReceiptStore(context.getApplicationContext());
        }
        return instance;
    }

    private ReceiptStore(Context context) {
        this.context = context;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        receiptsDir = new File(context.getFilesDir(), "receipts");
        if (!receiptsDir.exists()) receiptsDir.mkdirs();
    }

    public static class SavedReceipt {
        public String fileName;
        public String saveDate;
        public String title;
        public ReceiptData data;
        public String renderedText;
        public boolean labeled;
    }

    public synchronized List<SavedReceipt> loadAll() {
        List<SavedReceipt> out = new ArrayList<>();
        File[] files = receiptsDir.listFiles((d, f) -> f.endsWith(".json"));
        if (files != null) {
            for (File f : files) {
                try (FileReader fr = new FileReader(f)) {
                    SavedReceipt r = gson.fromJson(fr, SavedReceipt.class);
                    if (r != null) {
                        r.fileName = f.getName();
                        out.add(r);
                    }
                } catch (Exception e) {
                    Diagnostics.log(ErrorCodes.RC_002, COMPONENT, "loadAll", e, "File=" + f.getName());
                }
            }
        }
        Collections.sort(out, Comparator.comparing((SavedReceipt r) -> r.saveDate == null ? "" : r.saveDate).reversed());
        return out;
    }

    public synchronized SavedReceipt load(String fileName) {
        File f = new File(receiptsDir, fileName);
        if (!f.exists()) return null;
        try (FileReader fr = new FileReader(f)) {
            SavedReceipt r = gson.fromJson(fr, SavedReceipt.class);
            if (r != null) r.fileName = fileName;
            return r;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.RC_002, COMPONENT, "load", e, "File=" + fileName);
            return null;
        }
    }

    public synchronized String save(ReceiptData data, String renderedText, boolean labeled, String titleOverride) {
        SavedReceipt r = new SavedReceipt();
        r.data = data;
        r.renderedText = renderedText;
        r.labeled = labeled;
        r.title = titleOverride != null ? titleOverride : data.storeName;
        r.saveDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        r.fileName = "receipt_" + stamp + ".json";
        try (FileWriter fw = new FileWriter(new File(receiptsDir, r.fileName))) {
            gson.toJson(r, fw);
            Diagnostics.info(COMPONENT, "save", "Saved receipt " + r.fileName);
            return r.fileName;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.RC_001, COMPONENT, "save", e, "Receipt save failed");
            return null;
        }
    }

    public synchronized boolean delete(String fileName) {
        File f = new File(receiptsDir, fileName);
        boolean ok = f.exists() && f.delete();
        if (ok) Diagnostics.info(COMPONENT, "delete", "Deleted receipt " + fileName);
        return ok;
    }

    /** Export a rendered receipt to /storage/emulated/0/Download/ as a .txt file. */
    public synchronized String exportToDownloads(String renderedText, String suggestedName) {
        try {
            String baseName = safeName(suggestedName == null || suggestedName.isEmpty() ? "receipt" : suggestedName);
            byte[] data = renderedText.getBytes("UTF-8");
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, baseName + ".txt");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                android.net.Uri uri = context.getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            os.write(data);
                            os.flush();
                        }
                    }
                    Diagnostics.info(COMPONENT, "exportToDownloads", "Exported via MediaStore: " + baseName + ".txt");
                    return baseName + ".txt";
                }
            }
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (dir != null) {
                if (!dir.exists()) dir.mkdirs();
                File f = new File(dir, baseName + ".txt");
                try (FileOutputStream fos = new FileOutputStream(f)) {
                    fos.write(data);
                    fos.flush();
                }
                Diagnostics.info(COMPONENT, "exportToDownloads", "Exported direct: " + f.getAbsolutePath());
                return f.getAbsolutePath();
            }
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.FL_002, COMPONENT, "exportToDownloads", e, "Export failed");
        }
        return null;
    }

    private String safeName(String s) {
        String clean = s.replaceAll("[^a-zA-Z0-9 _-]", "_").trim();
        return clean.length() > 60 ? clean.substring(0, 60) : clean;
    }
}