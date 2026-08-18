package com.jnetai.thermal;

import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
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

public class TextPrintActivity extends AppCompatActivity {
    private static final String COMPONENT = "TextPrintActivity";
    private EditText textInput;
    private TextView charLabel;
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

        root.addView(ThemeUI.header(this, "Copy & Paste Print"));
        root.addView(ThemeUI.info(this, "Type or paste text below, then print.\nFont type A is used - text is printed left to right."));

        textInput = ThemeUI.input(this, "", android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textInput.setMinLines(8);
        textInput.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams taLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        taLp.height = ThemeUI.dp(this, 200);
        textInput.setLayoutParams(taLp);
        root.addView(textInput);

        charLabel = ThemeUI.info(this, "0 chars");
        root.addView(charLabel);
        textInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                charLabel.setText(s.length() + " chars");
            }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
        });

        Button pasteBtn = ThemeUI.secondaryButton(this, "Paste from Clipboard");
        pasteBtn.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                textInput.setText(clip);
                Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(pasteBtn);

        Button printBtn = ThemeUI.button(this, "Print Text");
        printBtn.setOnClickListener(v -> printText());
        root.addView(printBtn);

        Button selectBtn = ThemeUI.secondaryButton(this, "Select Printer");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        root.addView(selectBtn);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void printText() {
        ThemeUI.hideKeyboard(this, textInput);
        String text = textInput.getText().toString();
        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Nothing to print", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printText(text);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "Text printed (" + text.length() + " chars)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_001, COMPONENT, "printText", "Result=" + r);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}