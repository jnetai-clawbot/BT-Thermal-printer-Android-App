package com.jnetai.thermal.store;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.Template;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TemplateStore {
    private static final String COMPONENT = "TemplateStore";
    private static TemplateStore instance;
    private final Context context;
    private final Gson gson;
    private final File templatesDir;
    private final File logosDir;

    public static synchronized TemplateStore getInstance(Context context) {
        if (instance == null) {
            instance = new TemplateStore(context.getApplicationContext());
        }
        return instance;
    }

    private TemplateStore(Context context) {
        this.context = context;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        templatesDir = new File(context.getFilesDir(), "templates");
        logosDir = new File(context.getFilesDir(), "logos");
        if (!templatesDir.exists()) templatesDir.mkdirs();
        if (!logosDir.exists()) logosDir.mkdirs();
    }

    private File fileFor(String name) {
        return new File(templatesDir, safeName(name) + ".json");
    }

    private String safeName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_ -]", "_").trim();
    }

    public synchronized List<Template> loadAll() {
        List<Template> list = new ArrayList<>();
        File[] files = templatesDir.listFiles((d, f) -> f.endsWith(".json"));
        if (files == null) return list;
        for (File f : files) {
            try (FileInputStream fis = new FileInputStream(f)) {
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[1024];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    sb.append(new String(buf, 0, n, "UTF-8"));
                }
                Template t = gson.fromJson(sb.toString(), Template.class);
                if (t != null) list.add(t);
            } catch (Exception e) {
                Diagnostics.log(ErrorCodes.TM_004, COMPONENT, "loadAll", e, "File=" + f.getName());
            }
        }
        Collections.sort(list, Comparator.comparing(t -> t.name == null ? "" : t.name));
        return list;
    }

    public synchronized Template load(String name) {
        File f = fileFor(name);
        if (!f.exists()) return null;
        try (FileInputStream fis = new FileInputStream(f)) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[1024];
            int n;
            while ((n = fis.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, "UTF-8"));
            }
            return gson.fromJson(sb.toString(), Template.class);
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.TM_001, COMPONENT, "load", e, "Name=" + name);
            return null;
        }
    }

    public synchronized boolean save(Template t) {
        if (t.name == null || t.name.trim().isEmpty()) {
            Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "save", "Empty template name");
            return false;
        }
        try {
            String json = gson.toJson(t);
            File f = fileFor(t.name);
            File tmp = new File(templatesDir, f.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(json.getBytes("UTF-8"));
            }
            if (!tmp.renameTo(f)) {
                if (f.exists()) f.delete();
                tmp.renameTo(f);
            }
            Diagnostics.info(COMPONENT, "save", "Saved template: " + t.name);
            return true;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "save", e, "Name=" + t.name);
            return false;
        }
    }

    public synchronized boolean delete(String name) {
        try {
            File f = fileFor(name);
            boolean del = f.exists() && f.delete();
            if (del) Diagnostics.info(COMPONENT, "delete", "Deleted template: " + name);
            return del;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.TM_003, COMPONENT, "delete", e, "Name=" + name);
            return false;
        }
    }

    public synchronized boolean saveLogo(String templateName, Bitmap bmp) {
        try {
            File f = new File(logosDir, safeName(templateName) + ".png");
            try (FileOutputStream fos = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            Diagnostics.info(COMPONENT, "saveLogo", "Saved logo: " + f.getName());
            return true;
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "saveLogo", e, "Template=" + templateName);
            return false;
        }
    }

    public synchronized Bitmap loadLogo(String templateName) {
        File f = new File(logosDir, safeName(templateName) + ".png");
        if (!f.exists()) return null;
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.TM_001, COMPONENT, "loadLogo", e, "File=" + f.getName());
            return null;
        }
    }

    public synchronized boolean deleteLogo(String templateName) {
        File f = new File(logosDir, safeName(templateName) + ".png");
        return f.exists() && f.delete();
    }

    public String logoPathFor(String templateName) {
        File f = new File(logosDir, safeName(templateName) + ".png");
        return f.getAbsolutePath();
    }

    public void ensureDefaultTemplates() {
        List<Template> existing = loadAll();
        for (Template t : existing) {
            if ("Simple Receipt".equals(t.name) || "Label 58x40".equals(t.name)) {
                return;
            }
        }
        boolean hasReceipt = false;
        boolean hasLabel = false;
        for (Template t : existing) {
            if ("Simple Receipt".equals(t.name)) hasReceipt = true;
            if ("Label 58x40".equals(t.name)) hasLabel = true;
        }
        if (!hasReceipt) {
            Template t = new Template();
            t.name = "Simple Receipt";
            t.title = "RECEIPT";
            t.widthMm = 58;
            t.isLabel = false;
            t.feedBefore = 2;
            t.feedAfter = 4;
            t.logoEnabled = false;
            addTemplateLine(t, "text", "{store}", 1, false, 0);
            addTemplateLine(t, "text", "{address}", 0, false, 0);
            addTemplateLine(t, "text", "{phone}", 0, false, 0);
            addTemplateLine(t, "text", "================================", 0, false, 1);
            addTemplateLine(t, "text", "{title}", 2, true, 0);
            addTemplateLine(t, "text", "No: {number}  {date} {time}", 1, false, 0);
            addTemplateLine(t, "text", "Cashier: {cashier}", 0, false, 0);
            addTemplateLine(t, "text", "--------------------------------", 0, false, 0);
            addTemplateLine(t, "items", "", 0, true, 0);
            addTemplateLine(t, "text", "--------------------------------", 0, false, 0);
            addTemplateLine(t, "text", "TOTAL     {total}", 2, true, 0);
            addTemplateLine(t, "text", "Cash: {cash}  Change: {change}", 0, false, 0);
            addTemplateLine(t, "text", "{footer}", 1, false, 0);
            save(t);
        }
        if (!hasLabel) {
            Template t = new Template();
            t.name = "Label 58x40";
            t.title = "";
            t.isLabel = true;
            t.labelWidthMm = 58;
            t.labelHeightMm = 40;
            t.feedBefore = 0;
            t.feedAfter = 6;
            addTemplateLine(t, "text", "{header}", 2, true, 0);
            addTemplateLine(t, "text", "{items}", 0, false, 0);
            save(t);
        }
    }

    private void addTemplateLine(Template t, String kind, String text, int size, boolean bold, int align) {
        Template.TemplateLine l = new Template.TemplateLine();
        l.kind = kind;
        l.text = text;
        l.size = size;
        l.bold = bold;
        l.align = align;
        t.lines.add(l);
    }
}