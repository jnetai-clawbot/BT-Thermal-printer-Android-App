package com.jnetai.thermal.diagnostics;

import android.os.Build;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Diagnostics {
    private static final String TAG = "JNT-Diag";
    private static final int MAX_ENTRIES = 500;
    private static final List<DiagnosticEntry> entries = Collections.synchronizedList(new ArrayList<>());

    public static class DiagnosticEntry {
        public final String errorCode;
        public final String timestamp;
        public final String component;
        public final String operation;
        public final String exceptionType;
        public final String stackTrace;
        public final String metadata;
        public final String androidInfo;

        DiagnosticEntry(String errorCode, String component, String operation,
                         String exceptionType, String stackTrace, String metadata) {
            this.errorCode = errorCode;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            this.component = component;
            this.operation = operation;
            this.exceptionType = exceptionType;
            this.stackTrace = stackTrace;
            this.metadata = metadata;
            this.androidInfo = "Android " + Build.VERSION.SDK_INT + " (" + Build.VERSION.RELEASE + ") " + Build.MODEL;
        }

        public String toDisplayString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(errorCode).append("] ").append(timestamp).append("\n");
            sb.append("Component: ").append(component).append("\n");
            sb.append("Operation: ").append(operation).append("\n");
            if (exceptionType != null) sb.append("Exception: ").append(exceptionType).append("\n");
            if (metadata != null) sb.append("Metadata: ").append(metadata).append("\n");
            sb.append("Device: ").append(androidInfo).append("\n");
            if (stackTrace != null) sb.append("Stack:\n").append(stackTrace).append("\n");
            return sb.toString();
        }
    }

    public static void log(String errorCode, String component, String operation, Throwable throwable, String metadata) {
        String exceptionType = throwable != null ? throwable.getClass().getName() : null;
        String stackTrace = null;
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        }
        DiagnosticEntry entry = new DiagnosticEntry(errorCode, component, operation, exceptionType, stackTrace, metadata);
        entries.add(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.remove(0);
        }
        Log.e(TAG, entry.toDisplayString());
    }

    public static void log(String errorCode, String component, String operation, String metadata) {
        log(errorCode, component, operation, null, metadata);
    }

    public static void info(String component, String operation, String metadata) {
        Log.i(TAG, "[" + component + "] " + operation + " | " + metadata);
    }

    public static List<DiagnosticEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public static void clear() {
        entries.clear();
    }

    public static String getFullReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== J~Net Thermal Printer Diagnostics Report ===\n");
        sb.append("Device: ").append(Build.MODEL).append("\n");
        sb.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("Entries: ").append(entries.size()).append("\n\n");
        for (DiagnosticEntry entry : getEntries()) {
            sb.append(entry.toDisplayString()).append("\n");
        }
        return sb.toString();
    }
}