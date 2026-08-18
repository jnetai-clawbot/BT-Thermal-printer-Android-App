package com.jnetai.thermal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
import com.jnetai.thermal.util.ThemeUI;

public class ImagePrintActivity extends AppCompatActivity {
    private static final String COMPONENT = "ImagePrintActivity";
    private ImageView preview;
    private TextView imageLabel;
    private Bitmap selectedBitmap;
    private SettingsStore settings;
    private BluetoothHelper bt;

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    loadImage(uri);
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

        root.addView(ThemeUI.header(this, "Print an Image"));
        root.addView(ThemeUI.info(this, "The image is converted to black & white for the thermal printer.\n"
                + "Density/darkness is set in Settings."));

        Button pickBtn = ThemeUI.button(this, "Choose Image");
        pickBtn.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            imagePicker.launch(pick);
        });
        root.addView(pickBtn);

        imageLabel = ThemeUI.info(this, "No image selected");
        root.addView(imageLabel);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setPadding(0, ThemeUI.dp(this, 8), 0, ThemeUI.dp(this, 8));
        root.addView(preview);

        Button printBtn = ThemeUI.button(this, "Print Image");
        printBtn.setOnClickListener(v -> printImage());
        root.addView(printBtn);

        Button selectBtn = ThemeUI.secondaryButton(this, "Select Printer");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        root.addView(selectBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadImage(Uri uri) {
        try {
            Bitmap bmp = decodeBitmap(uri);
            selectedBitmap = bmp;
            if (selectedBitmap == null) {
                Toast.makeText(this, "Could not decode image", Toast.LENGTH_SHORT).show();
                Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "loadImage", "Decode returned null, uri=" + uri);
                return;
            }
            imageLabel.setText("Image: " + selectedBitmap.getWidth() + " x " + selectedBitmap.getHeight() + " px");
            preview.setImageBitmap(selectedBitmap);
            Diagnostics.info(COMPONENT, "loadImage", "Loaded image " + selectedBitmap.getWidth() + "x" + selectedBitmap.getHeight());
        } catch (Exception e) {
            Toast.makeText(this, "Image load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "loadImage", e, "Uri=" + uri);
        }
    }

    private Bitmap decodeBitmap(Uri uri) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                android.graphics.ImageDecoder.Source src =
                        android.graphics.ImageDecoder.createSource(getContentResolver(), uri);
                return android.graphics.ImageDecoder.decodeBitmap(src, (decoder, info, s) -> {
                });
            }
        } catch (Exception e) {
            Diagnostics.info(COMPONENT, "decodeBitmap", "ImageDecoder failed, using BitmapFactory: " + e.getMessage());
        }
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (java.io.IOException e) {
            Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "decodeBitmap", e, "Uri=" + uri);
            return null;
        }
    }

    private void printImage() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Choose an image first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printImage(selectedBitmap);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "Image sent to printer", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_006, COMPONENT, "printImage", "Result=" + r);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}