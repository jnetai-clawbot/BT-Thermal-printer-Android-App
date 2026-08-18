package com.jnetai.thermal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
import com.jnetai.thermal.util.ThemeUI;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FilePrintActivity extends AppCompatActivity {
    private static final String COMPONENT = "FilePrintActivity";
    private TextView fileLabel;
    private TextView contentPreview;
    private Uri selectedUri;
    private String loadedText = "";
    private boolean loadedImage = false;
    private SettingsStore settings;
    private BluetoothHelper bt;

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedUri = result.getData().getData();
                    loadSelectedFile(selectedUri);
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

        root.addView(ThemeUI.header(this, "Print a File"));
        root.addView(ThemeUI.info(this, "Pick a text file (.txt, .csv, .log, .json...) to print as text, "
                + "or an image (.png, .jpg) to print as a picture."));

        Button pickBtn = ThemeUI.button(this, "Choose File");
        pickBtn.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("*/*");
            filePicker.launch(pick);
        });
        root.addView(pickBtn);

        fileLabel = ThemeUI.info(this, "No file selected");
        root.addView(fileLabel);

        contentPreview = new TextView(this);
        contentPreview.setTextColor(ThemeUI.TEXT_GREY);
        contentPreview.setTextSize(13);
        contentPreview.setMaxLines(12);
        root.addView(contentPreview);

        Button printTextBtn = ThemeUI.button(this, "Print as Text");
        printTextBtn.setOnClickListener(v -> printAsText());
        root.addView(printTextBtn);

        Button printImageBtn = ThemeUI.secondaryButton(this, "Print as Image");
        printImageBtn.setOnClickListener(v -> printAsImage());
        root.addView(printImageBtn);

        Button selectBtn = ThemeUI.secondaryButton(this, "Select Printer");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        root.addView(selectBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadSelectedFile(Uri uri) {
        loadedText = "";
        loadedImage = false;
        fileLabel.setText("File: " + uri.getLastPathSegment());
        contentPreview.setText("Loading...");
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Toast.makeText(this, "Could not read file", Toast.LENGTH_SHORT).show();
                Diagnostics.log(ErrorCodes.FL_001, COMPONENT, "loadSelectedFile", "openInputStream returned null");
                return;
            }
            String mime = getContentResolver().getType(uri);
            boolean image = mime != null && mime.startsWith("image/");
            if (image) {
                byte[] bytes = readAll(is);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp == null) {
                    contentPreview.setText("Image could not be decoded.");
                    Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "loadSelectedFile", "Bitmap decode failed for image file");
                    return;
                }
                loadedImage = true;
                selectedImage = bmp;
                contentPreview.setText("Image loaded: " + bmp.getWidth() + " x " + bmp.getHeight());
            } else {
                StringBuilder sb = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
                String line;
                int lines = 0;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                    if (++lines > 500) break;
                }
                loadedText = sb.toString();
                contentPreview.setText(loadedText);
                fileLabel.setText("File: " + uri.getLastPathSegment() + "  (first " + Math.min(lines, 500) + " lines)");
            }
        } catch (Exception e) {
            Toast.makeText(this, "File read failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.FL_001, COMPONENT, "loadSelectedFile", e, "Uri=" + uri);
        }
    }

    private byte[] readAll(InputStream is) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private android.graphics.Bitmap selectedImage;

    private void printAsText() {
        if (selectedUri == null) {
            Toast.makeText(this, "Choose a file first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (loadedImage) {
            Toast.makeText(this, "That file is an image - use Print as Image", Toast.LENGTH_LONG).show();
            return;
        }
        if (loadedText.trim().isEmpty()) {
            Toast.makeText(this, "File is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printText(loadedText);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "File printed as text", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_001, COMPONENT, "printAsText", "Result=" + r);
        }
    }

    private void printAsImage() {
        if (selectedUri == null) {
            Toast.makeText(this, "Choose a file first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!loadedImage || selectedImage == null) {
            Toast.makeText(this, "That file is not an image - use Print as Text", Toast.LENGTH_LONG).show();
            return;
        }
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printImage(selectedImage);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "Image printed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_006, COMPONENT, "printAsImage", "Result=" + r);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}