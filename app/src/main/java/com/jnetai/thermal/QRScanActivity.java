package com.jnetai.thermal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScanActivity extends AppCompatActivity {
    private static final String COMPONENT = "QRScanActivity";
    private PreviewView previewView;
    private ExecutorService analysisExecutor;
    private BarcodeScanner scanner;
    private boolean processed = false;

    private final ActivityResultLauncher<String> cameraPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required to scan QR codes", Toast.LENGTH_LONG).show();
                    Diagnostics.log(ErrorCodes.BT_002, COMPONENT, "cameraPermission", "Camera permission denied");
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewView = new PreviewView(this);
        previewView.setBackgroundColor(0xFF000000);
        setContentView(previewView);
        analysisExecutor = Executors.newSingleThreadExecutor();

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            cameraPermLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        androidx.camera.lifecycle.ProcessCameraProvider cameraProviderFuture;
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider provider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(new Size(1280, 720))
                        .build();
                analysis.setAnalyzer(analysisExecutor, this::analyze);

                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;
                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, analysis);
            } catch (Exception e) {
                Diagnostics.log(ErrorCodes.QR_003, COMPONENT, "startCamera", e, "CameraX bind failed");
                Toast.makeText(this, "Camera start failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyze(@NonNull ImageProxy imageProxy) {
        if (processed) {
            imageProxy.close();
            return;
        }
        try {
            @SuppressWarnings("UnstableApiUsage")
            InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (processed) return;
                        for (Barcode b : barcodes) {
                            String text = b.getRawValue() != null ? b.getRawValue() : b.getDisplayValue();
                            if (text != null && !text.isEmpty()) {
                                processed = true;
                                runOnUiThread(() -> deliverResult(text));
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Diagnostics.log(ErrorCodes.QR_004, COMPONENT, "analyze", e, "ML Kit scan failure");
                    })
                    .addOnCompleteListener(task -> imageProxy.close());
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.QR_003, COMPONENT, "analyze", e, "Analysis exception");
            imageProxy.close();
        }
    }

    private void deliverResult(String text) {
        android.content.Intent result = new Intent();
        result.putExtra("qr_text", text);
        setResult(RESULT_OK, result);
        Diagnostics.info(COMPONENT, "scan", "Scanned QR: " + text.length() + " chars");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.close();
        if (analysisExecutor != null) analysisExecutor.shutdown();
    }
}