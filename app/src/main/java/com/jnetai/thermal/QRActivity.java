package com.jnetai.thermal;

import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.core.PrintManager;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.QRGenerator;
import com.jnetai.thermal.util.ThemeUI;
import java.io.OutputStream;

public class QRActivity extends AppCompatActivity {
    private static final String COMPONENT = "QRActivity";
    private EditText contentInput;
    private ImageView qrPreview;
    private TextView statusLabel;
    private Bitmap currentQr;
    private String currentContent = "";
    private SettingsStore settings;
    private BluetoothHelper bt;

    private final ActivityResultLauncher<Intent> scanLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String scanned = result.getData().getStringExtra("qr_text");
                    if (scanned != null) {
                        contentInput.setText(scanned);
                        generateQr(true);
                        statusLabel.setText("QR code read from camera");
                    } else {
                        statusLabel.setText("Scanning cancelled");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);
        bt = new BluetoothHelper(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        root.addView(ThemeUI.header(this, "QR Code"));

        root.addView(ThemeUI.info(this, "Enter text below and press Generate.\n"
                + "You can save it as a PNG, print it, or scan a QR code with the camera."));

        contentInput = ThemeUI.input(this, "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        contentInput.setMinLines(3);
        root.addView(contentInput);

        Button genBtn = ThemeUI.button(this, "Generate QR Code");
        genBtn.setOnClickListener(v -> generateQr(true));
        root.addView(genBtn);

        qrPreview = new ImageView(this);
        qrPreview.setAdjustViewBounds(true);
        qrPreview.setPadding(0, ThemeUI.dp(this, 10), 0, ThemeUI.dp(this, 10));
        root.addView(qrPreview);

        statusLabel = ThemeUI.info(this, "No QR code generated yet");
        root.addView(statusLabel);

        Button saveBtn = ThemeUI.secondaryButton(this, "Save PNG to Downloads");
        saveBtn.setOnClickListener(v -> saveQrPng());
        root.addView(saveBtn);

        Button printBtn = ThemeUI.button(this, "Print QR Code");
        printBtn.setOnClickListener(v -> printQr());
        root.addView(printBtn);

        Button scanBtn = ThemeUI.button(this, "Scan QR Code (Camera)");
        scanBtn.setOnClickListener(v -> openScanner());
        root.addView(scanBtn);

        Button copyBtn = ThemeUI.secondaryButton(this, "Copy QR Text");
        copyBtn.setOnClickListener(v -> copyText());
        root.addView(copyBtn);

        Button selectBtn = ThemeUI.secondaryButton(this, "Select Printer");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        root.addView(selectBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void generateQr(boolean showToast) {
        String content = contentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Enter some text first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int size = Math.max(400, Math.min(1200, content.length() * 6 + 400));
            currentQr = QRGenerator.generateQr(content, size);
            currentContent = content;
            qrPreview.setImageBitmap(currentQr);
            statusLabel.setText("QR code generated (" + content.length() + " chars)");
            if (showToast) Toast.makeText(this, "QR code generated", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "QR generation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.QR_001, COMPONENT, "generateQr", e, "content=" + content);
        }
    }

    private void saveQrPng() {
        if (currentQr == null) {
            generateQr(true);
            if (currentQr == null) return;
        }
        try {
            String baseName = "qr_" + System.currentTimeMillis() + ".png";
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, baseName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
                if (uri != null) {
                    try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                        if (os != null) {
                            currentQr.compress(Bitmap.CompressFormat.PNG, 100, os);
                        }
                    }
                    statusLabel.setText("Saved to Downloads: " + baseName);
                    Toast.makeText(this, "QR PNG saved to /storage/emulated/0/Download/", Toast.LENGTH_LONG).show();
                    Diagnostics.info(COMPONENT, "saveQrPng", "Saved via MediaStore: " + baseName);
                    return;
                }
            }
            java.io.File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) dir.mkdirs();
            java.io.File f = new java.io.File(dir, baseName);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(f)) {
                currentQr.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            statusLabel.setText("Saved to: " + f.getAbsolutePath());
            Toast.makeText(this, "QR PNG saved: " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Diagnostics.info(COMPONENT, "saveQrPng", "Saved direct: " + f.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.QR_002, COMPONENT, "saveQrPng", e, "Save QR to Downloads failed");
        }
    }

    private void printQr() {
        if (currentContent.isEmpty()) {
            generateQr(true);
            if (currentContent.isEmpty()) return;
        }
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printQR(currentContent);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "QR code printed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.QR_002, COMPONENT, "printQr", "Result=" + r);
        }
    }

    private void openScanner() {
        Intent i = new Intent(this, QRScanActivity.class);
        scanLauncher.launch(i);
    }

    private void copyText() {
        if (currentContent.isEmpty()) {
            Toast.makeText(this, "Generate a QR first", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("QR text", currentContent));
        Toast.makeText(this, "QR text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}