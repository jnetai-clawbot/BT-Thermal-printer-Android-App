package com.jnetai.thermal;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.ThemeUI;
import java.util.List;

public class PrinterSelectActivity extends AppCompatActivity {
    private static final String COMPONENT = "PrinterSelectActivity";
    private SettingsStore settings;
    private BluetoothHelper bt;
    private LinearLayout listLayout;
    private TextView statusLabel;

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);
        bt = new BluetoothHelper(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);

        root.addView(ThemeUI.header(this, "Select Printer"));
        statusLabel = new TextView(this);
        statusLabel.setTextSize(14);
        root.addView(statusLabel);

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);

        android.widget.Button refreshBtn = ThemeUI.button(this, "Refresh Printer List");
        refreshBtn.setOnClickListener(v -> buildList());
        root.addView(refreshBtn);
        root.addView(ThemeUI.info(this, "Connect your printer to the phone (Settings > Bluetooth) first.\nDefault printer: PT210 Thermal Printer."));

        scroll.addView(root);
        setContentView(scroll);
        buildList();
    }

    private android.view.ViewGroup.LayoutParams buttonIndex() {
        return new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void buildList() {
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            statusLabel.setText("Bluetooth permission needed - grant it and refresh.");
            return;
        }
        statusLabel.setText("");
        listLayout.removeAllViews();
        if (!bt.isSupported()) {
            statusLabel.setText("Bluetooth is not supported on this device.");
            Diagnostics.log(ErrorCodes.BT_001, COMPONENT, "buildList", "Unsupported");
            return;
        }
        List<BluetoothHelper.DeviceInfo> devices = bt.getBondedDevices();
        if (devices.isEmpty()) {
            statusLabel.setText("No paired devices found.\nPair the PT210 in Android Settings > Bluetooth first.");
            Diagnostics.log(ErrorCodes.BT_003, COMPONENT, "buildList", "No paired devices");
            return;
        }
        statusLabel.setText("Paired devices: " + devices.size() + "  (tap to set as default printer)");
        String current = settings.getPrinterAddress();
        for (BluetoothHelper.DeviceInfo d : devices) {
            android.widget.Button b = ThemeUI.secondaryButton(this, d.toString()
                    + (d.address.equals(current) ? "   [DEFAULT]" : ""));
            b.setOnClickListener(v -> selectDevice(d));
            android.view.ViewGroup.MarginLayoutParams lp =
                    (android.view.ViewGroup.MarginLayoutParams) b.getLayoutParams();
            lp.setMargins(0, 0, 0, ThemeUI.dp(this, 6));
            b.setLayoutParams(lp);
            listLayout.addView(b);
        }
    }

    private void selectDevice(BluetoothHelper.DeviceInfo d) {
        settings.setPrinterAddress(d.address);
        settings.setPrinterName(d.name == null || d.name.isEmpty() ? "PT210 Thermal Printer" : d.name);
        Toast.makeText(this, "Default printer set to: " + d.name, Toast.LENGTH_LONG).show();
        Diagnostics.info(COMPONENT, "selectDevice", "Set default: " + d.name + " " + d.address);
        buildList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}