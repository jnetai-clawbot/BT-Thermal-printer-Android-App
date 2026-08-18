package com.jnetai.thermal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.core.PrintManager;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.ThemeUI;

public class PrinterTestActivity extends AppCompatActivity {
    private static final String COMPONENT = "PrinterTestActivity";
    private TextView statusLabel;
    private SettingsStore settings;
    private BluetoothHelper bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);
        bt = new BluetoothHelper(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);

        root.addView(ThemeUI.header(this, "Printer Test"));
        statusLabel = new TextView(this);
        statusLabel.setTextSize(14);
        root.addView(statusLabel);
        refreshStatus();

        android.widget.Button testBtn = ThemeUI.button(this, "Run Printer Test");
        testBtn.setOnClickListener(v -> runTest());
        root.addView(testBtn);

        android.widget.Button selectBtn = ThemeUI.secondaryButton(this, "Select Printer");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        root.addView(selectBtn);

        root.addView(ThemeUI.info(this, "Prints a test pattern (characters, colours and a QR code).\nSet the printer in Settings > Bluetooth first, or tap Select Printer."));

        scroll.addView(root);
        setContentView(scroll);
    }

    private void refreshStatus() {
        String name = settings.getPrinterName();
        String addr = settings.getPrinterAddress();
        if (addr == null || addr.isEmpty()) {
            statusLabel.setText("No printer selected yet.\nTap Select Printer to choose your PT210.");
        } else {
            statusLabel.setText("Testing printer: " + name + "  (" + addr + ")");
        }
    }

    private void runTest() {
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        String addr = settings.getPrinterAddress();
        if (addr == null || addr.isEmpty()) {
            Toast.makeText(this, "Select a printer first", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_005, COMPONENT, "runTest", "No printer selected");
            return;
        }
        statusLabel.setText("Printing test pattern...");
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.testPattern();
        setResultMessage(r);
    }

    private void setResultMessage(PrintManager.PrintResult r) {
        switch (r) {
            case SUCCESS:
                statusLabel.setText("Test print sent successfully.\nCheck the printer for the test page.");
                Toast.makeText(this, "Test print sent", Toast.LENGTH_SHORT).show();
                break;
            case NO_PRINTER:
                statusLabel.setText("No printer selected. Tap Select Printer.");
                break;
            case CONNECTION_FAILED:
                statusLabel.setText("Could not connect to the printer.\nMake sure it is powered ON and paired in Android Bluetooth settings.");
                break;
            case NOT_CONNECTED:
                statusLabel.setText("Printer disconnected during the test.\nPower the printer on and try again.");
                break;
            default:
                statusLabel.setText("Test failed. See Diagnostics log.");
                Diagnostics.log(ErrorCodes.PR_004, COMPONENT, "setResultMessage", "Test failed result=" + r);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}