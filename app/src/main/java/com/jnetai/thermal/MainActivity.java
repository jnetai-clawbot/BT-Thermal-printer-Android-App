package com.jnetai.thermal;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.store.TemplateStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.ThemeUI;

public class MainActivity extends AppCompatActivity {
    private static final String COMPONENT = "MainActivity";

    private TextView printerStatus;
    private SettingsStore settings;
    private BluetoothHelper bt;

    private final ActivityResultLauncher<String> btPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Toast.makeText(this, "Bluetooth permission denied - printing will not work", Toast.LENGTH_LONG).show();
                    Diagnostics.log(ErrorCodes.BT_002, COMPONENT, "onCreate", "Bluetooth permission denied");
                } else {
                    refreshPrinterStatus();
                }
            });

    private final ActivityResultLauncher<Intent> enableBtLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                refreshPrinterStatus();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);
        bt = new BluetoothHelper(this);

        TemplateStore.getInstance(this).ensureDefaultTemplates();

        BuildUI();
        requestPermissionsIfNeeded();
        refreshPrinterStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPrinterStatus();
    }

    private void requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return;
            }
        }
        if (Build.VERSION.SDK_INT <= 29 && !Perms.hasStorage(this)) {
            Perms.request(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, Perms.REQ_STORAGE);
        }
        if (!bt.isSupported()) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "requestPermissionsIfNeeded", "Bluetooth unsupported");
        } else if (!bt.isEnabled()) {
            try {
                BluetoothAdapter adapter = bt.getAdapter();
                if (adapter != null) {
                    enableBtLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                }
            } catch (SecurityException e) {
                Diagnostics.log(ErrorCodes.BT_002, COMPONENT, "requestPermissionsIfNeeded", e, "Could not request BT enable");
            }
        }
    }

    private void BuildUI() {
        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout ll = ThemeUI.vertical(this);

        ll.addView(ThemeUI.header(this, "J~Net Thermal Printer"));
        ll.addView(ThemeUI.info(this, "Bluetooth POS Thermal Printer (PT210 compatible)\nReceipts, Labels & QR Codes"));

        printerStatus = new TextView(this);
        printerStatus.setTextSize(14);
        printerStatus.setPadding(0, 0, 0, ThemeUI.dp(this, 10));
        ll.addView(printerStatus);

        addModeButton(ll, "Print Template  (default mode)", v -> open(PrintTemplateActivity.class));
        addModeButton(ll, "Print Copy & Paste Text", v -> open(TextPrintActivity.class));
        addModeButton(ll, "Print a File", v -> open(FilePrintActivity.class));
        addModeButton(ll, "Print an Image", v -> open(ImagePrintActivity.class));
        addModeButton(ll, "QR Code  (generate / scan)", v -> open(QRActivity.class));
        addModeButton(ll, "Templates Manager", v -> open(TemplateListActivity.class));
        addModeButton(ll, "Saved Receipts", v -> open(ReceiptListActivity.class));
        addModeButton(ll, "Select Printer", v -> open(PrinterSelectActivity.class));
        addModeButton(ll, "Printer Test", v -> open(PrinterTestActivity.class));
        addModeButton(ll, "Settings", v -> open(SettingsActivity.class));
        addModeButton(ll, "About", v -> open(AboutActivity.class));

        scroll.addView(ll);
        setContentView(scroll);
    }

    private void addModeButton(LinearLayout ll, String text, View.OnClickListener listener) {
        android.widget.Button b = ThemeUI.button(this, text);
        b.setOnClickListener(listener);
        ll.addView(b);
        android.view.ViewGroup.MarginLayoutParams lp =
                (android.view.ViewGroup.MarginLayoutParams) b.getLayoutParams();
        lp.setMargins(0, ThemeUI.dp(this, 4), 0, ThemeUI.dp(this, 4));
        b.setLayoutParams(lp);
    }

    private void open(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    private void refreshPrinterStatus() {
        if (printerStatus == null) return;
        String name = settings.getPrinterName();
        String addr = settings.getPrinterAddress();
        boolean connected = bt.isConnected();
        String status;
        if (connected) {
            status = "Printer connected: " + (bt.getCurrentDeviceName() != null ? bt.getCurrentDeviceName() : name);
        } else if (addr == null || addr.isEmpty()) {
            status = "No printer selected - tap Select Printer";
        } else {
            status = "Printer: " + name + "  (" + addr + ")";
        }
        printerStatus.setText(status);
        printerStatus.setTextColor(connected ? ThemeUI.PRIMARY_LIGHT : ThemeUI.TEXT_MUTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}