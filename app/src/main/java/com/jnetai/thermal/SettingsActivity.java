package com.jnetai.thermal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.EscPos;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.util.ThemeUI;

public class SettingsActivity extends AppCompatActivity {
    private static final String COMPONENT = "SettingsActivity";
    private SettingsStore settings;
    private LinearLayout content;
    private EditText copiesInput;
    private TextView densityValue, feedBeforeValue, feedAfterValue, labelWValue, labelHValue;
    private SeekBar densityBar, feedBeforeBar, feedAfterBar;
    private Spinner widthSpinner, alignSpinner, charsetSpinner, qrModeSpinner, qrModuleSpinner;
    private String[] widthItems = {"58 mm", "80 mm"};
    private String[] alignItems = {"Left", "Center", "Right"};
    private String[] charsetItems = {"UTF-8", "CP437", "CP850", "Windows-1252", "GBK"};
    private String[] qrModeItems = {"Raster (compatible)", "ESC/POS (printer QR)"};
    private String[] qrModuleItems = {"2", "3", "4", "5", "6", "7", "8"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        content = ThemeUI.vertical(this);
        scroll.addView(content);
        setContentView(scroll);
        buildUI();
    }

    private void buildUI() {
        content.removeAllViews();
        content.addView(ThemeUI.header(this, "Print Settings"));

        content.addView(ThemeUI.subHeader(this, "Printer"));
        Button selectBtn = ThemeUI.secondaryButton(this, "Printer: " + settings.getPrinterName()
                + "  (" + (settings.getPrinterAddress().isEmpty() ? "none" : settings.getPrinterAddress()) + ")");
        selectBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));
        content.addView(selectBtn);
        Button testBtn = ThemeUI.secondaryButton(this, "Run Printer Test");
        testBtn.setOnClickListener(v -> startActivity(new Intent(this, PrinterTestActivity.class)));
        content.addView(testBtn);

        content.addView(ThemeUI.subHeader(this, "Layout"));
        addSpinnerRow("Paper Width", widthItems, settings.getPaperWidthMm() == 58 ? "58 mm" : "80 mm", widthSpinner -> {
            settings.setPaperWidthMm(widthSpinner.getSelectedItemPosition() == 0 ? 58 : 80);
        });
        addSpinnerRow("Alignment", alignItems, alignValue(), alignSpinner -> {
            settings.setAlignment(alignSpinner.getSelectedItemPosition());
        });
        addSwitchRow("Portrait / Landscape", "Print rotates image 90°", settings.isLandscape(), settings::setLandscape);

        content.addView(ThemeUI.subHeader(this, "Text"));
        addSpinnerRow("Default Font Size", new String[]{"Normal", "Large", "Extra Large"}, fontValue(), fontSpinner -> {
            settings.setFontSize(fontSpinner.getSelectedItemPosition() + 1);
        });
        addSwitchRow("Bold default", "Make printed text bold", settings.isBold(), settings::setBold);
        addSwitchRow("Underline default", "Underline printed text", settings.isUnderline(), settings::setUnderline);

        content.addView(ThemeUI.subHeader(this, "Darkness / Density"));
        densityValue = ThemeUI.info(this, "Density: " + settings.getDensity());
        densityBar = new SeekBar(this);
        densityBar.setMax(100);
        densityBar.setProgress(settings.getDensity());
        densityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                densityValue.setText("Density: " + p);
                settings.setDensity(p);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        content.addView(densityValue);
        content.addView(densityBar);

        content.addView(ThemeUI.subHeader(this, "Paper Feed & Cut"));
        feedBeforeValue = ThemeUI.info(this, "Feed before print (lines): " + settings.getFeedBefore());
        feedBeforeBar = new SeekBar(this);
        feedBeforeBar.setMax(20);
        feedBeforeBar.setProgress(settings.getFeedBefore());
        feedBeforeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                feedBeforeValue.setText("Feed before print (lines): " + p);
                settings.setFeedBefore(p);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        content.addView(feedBeforeValue);
        content.addView(feedBeforeBar);

        feedAfterValue = ThemeUI.info(this, "Feed after print (lines): " + settings.getFeedAfter());
        feedAfterBar = new SeekBar(this);
        feedAfterBar.setMax(30);
        feedAfterBar.setProgress(settings.getFeedAfter());
        feedAfterBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                feedAfterValue.setText("Feed after print (lines): " + p);
                settings.setFeedAfter(p);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        content.addView(feedAfterValue);
        content.addView(feedAfterBar);
        addSwitchRow("Cut paper after print", "Send paper cut command after print", settings.isCutPaper(), settings::setCutPaper);

        content.addView(ThemeUI.subHeader(this, "Copies"));
        copiesInput = ThemeUI.input(this, String.valueOf(settings.getCopies()), android.text.InputType.TYPE_CLASS_NUMBER);
        content.addView(copiesInput);

        content.addView(ThemeUI.subHeader(this, "Encoding"));
        addSpinnerRow("Character Set", charsetItems, charsetValue(), charsetSpinner -> {
            settings.setCharset(charsetToPref((String) charsetSpinner.getSelectedItem()));
        });

        content.addView(ThemeUI.subHeader(this, "QR Code"));
        addSpinnerRow("QR Print Mode", qrModeItems, settings.isQrRasterMode() ? "Raster (compatible)" : "ESC/POS (printer QR)", qrModeSpinner -> {
            settings.setQrRasterMode(qrModeSpinner.getSelectedItemPosition() == 0);
        });
        addSpinnerRow("Module Size (ESC/POS)", qrModuleItems, String.valueOf(settings.getQrModuleSize()), qrModuleSpinner -> {
            settings.setQrModuleSize(2 + qrModuleSpinner.getSelectedItemPosition());
        });

        content.addView(ThemeUI.subHeader(this, "Labels"));
        labelWValue = ThemeUI.info(this, "Label width (mm): " + settings.getLabelWidthMm());
        SeekBar lw = new SeekBar(this);
        lw.setMax(112 - 20);
        lw.setProgress(settings.getLabelWidthMm() - 20);
        lw.setOnSeekBarChangeListener(simpleBar(labelWValue, "Label width (mm): ", v -> settings.setLabelWidthMm(v)));
        content.addView(labelWValue);
        content.addView(lw);

        labelHValue = ThemeUI.info(this, "Label height (mm): " + settings.getLabelHeightMm());
        SeekBar lh = new SeekBar(this);
        lh.setMax(200 - 10);
        lh.setProgress(settings.getLabelHeightMm() - 10);
        lh.setOnSeekBarChangeListener(simpleBar(labelHValue, "Label height (mm): ", v -> settings.setLabelHeightMm(v)));
        content.addView(labelHValue);
        content.addView(lh);

        content.addView(ThemeUI.subHeader(this, "Other"));
        addSwitchRow("Show print confirmation dialog", "Ask before sending a print job", settings.isShowPrintDialog(), settings::setShowPrintDialog);

        content.addView(ThemeUI.subHeader(this, "Date & Time"));
        content.addView(ThemeUI.info(this, "Print the date and time of receipt generation on the receipt.\n"
                + "Placeholders: {date} {time} {datetime}. You can also set a custom date/time."));
        addSwitchRow("Print date & time on receipt", "Add date/time of generation to receipts", settings.isPrintDateTime(), settings::setPrintDateTime);

        String[] dateFormats = {"dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy", "MMM dd, yyyy"};
        addSpinnerRow("Date Format", dateFormats, dateFormatValue(), sp ->
                settings.setDateFormat((String) sp.getSelectedItem()));

        String[] timeFormats = {"HH:mm", "HH:mm:ss", "h:mm a", "h:mm:ss a", "HH:mm (24h)"};
        addSpinnerRow("Time Format", timeFormats, timeFormatValue(), sp ->
                settings.setTimeFormat((String) sp.getSelectedItem()));

        addSwitchRow("Override date & time", "Use a custom date/time instead of the generation time", settings.isDateTimeOverride(), settings::setDateTimeOverride);

        Button setDateTimeBtn = ThemeUI.secondaryButton(this, "Set Custom Date & Time");
        setDateTimeBtn.setOnClickListener(v -> showDateTimePicker());
        content.addView(setDateTimeBtn);

        Button diagnosticsBtn = ThemeUI.secondaryButton(this, "View Diagnostics Log");
        diagnosticsBtn.setOnClickListener(v -> showDiagnostics());
        content.addView(diagnosticsBtn);

        Button saveBtn = ThemeUI.button(this, "Save Settings");
        saveBtn.setOnClickListener(v -> saveAll());
        content.addView(saveBtn);
    }

    private SeekBar.OnSeekBarChangeListener simpleBar(TextView valueLabel, String prefix, java.util.function.Consumer<Integer> setter) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean f) {
                int v = p + (("Label width (mm): ".equals(prefix)) ? 20 : 10);
                valueLabel.setText(prefix + v);
                setter.accept(v);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
    }

    private String dateFormatValue() {
        String f = settings.getDateFormat();
        for (String s : new String[]{"dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd.MM.yyyy", "MMM dd, yyyy"}) {
            if (s.equals(f)) return s;
        }
        return "dd/MM/yyyy";
    }

    private String timeFormatValue() {
        String f = settings.getTimeFormat();
        String[] opts = {"HH:mm", "HH:mm:ss", "h:mm a", "h:mm:ss a", "HH:mm (24h)"};
        for (String s : opts) {
            if (s.equals(f)) return s;
        }
        return "HH:mm";
    }

    private void showDateTimePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(settings.getDateTimeOverrideMillis());
        new android.app.DatePickerDialog(this, (view, year, month, day) -> {
            final java.util.Calendar chosen = java.util.Calendar.getInstance();
            chosen.set(year, month, day);
            new android.app.TimePickerDialog(this, (tv, hour, minute) -> {
                chosen.set(java.util.Calendar.HOUR_OF_DAY, hour);
                chosen.set(java.util.Calendar.MINUTE, minute);
                chosen.set(java.util.Calendar.SECOND, 0);
                chosen.set(java.util.Calendar.MILLISECOND, 0);
                settings.setDateTimeOverrideMillis(chosen.getTimeInMillis());
                settings.setDateTimeOverride(true);
                Toast.makeText(this, "Custom date & time set: "
                        + java.text.DateFormat.getDateTimeInstance().format(chosen.getTime()), Toast.LENGTH_LONG).show();
                Diagnostics.info("SettingsActivity", "showDateTimePicker", "Override set to " + chosen.getTimeInMillis());
            }, cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE), true).show();
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private String alignValue() {
        int a = settings.getAlignment();
        return a == 1 ? "Center" : a == 2 ? "Right" : "Left";
    }

    private String fontValue() {
        int f = settings.getFontSize();
        return f >= 3 ? "Extra Large" : f == 2 ? "Large" : "Normal";
    }

    private String charsetValue() {
        String c = settings.getCharset();
        if (EscPos.ENC_CP437.equals(c)) return "CP437";
        if (EscPos.ENC_CP850.equals(c)) return "CP850";
        if (EscPos.ENC_WIN1252.equals(c)) return "Windows-1252";
        if (EscPos.ENC_GBK.equals(c)) return "GBK";
        return "UTF-8";
    }

    private String charsetToPref(String label) {
        switch (label) {
            case "CP437": return EscPos.ENC_CP437;
            case "CP850": return EscPos.ENC_CP850;
            case "Windows-1252": return EscPos.ENC_WIN1252;
            case "GBK": return EscPos.ENC_GBK;
            default: return EscPos.ENC_UTF8;
        }
    }

    private void addSpinnerRow(String label, String[] items, String selected, java.util.function.Consumer<Spinner> onPick) {
        content.addView(ThemeUI.label(this, label));
        Spinner sp = ThemeUI.spinnerWithListener(this, items, selected, new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                if (onPick != null) onPick.accept((Spinner) p);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        content.addView(sp);
        if (selected != null) {
            content.addView(ThemeUI.selectionLabel(this, selected));
        }
    }

    private void addSwitchRow(String label, String desc, boolean checked, java.util.function.Consumer<Boolean> onToggle) {
        LinearLayout row = ThemeUI.switchRow(this, label, checked, (btn, isChecked) -> onToggle.accept(isChecked));
        content.addView(row);
        if (desc != null && !desc.isEmpty()) {
            content.addView(ThemeUI.info(this, desc));
        }
    }

    private void saveAll() {
        try {
            int copies = Integer.parseInt(copiesInput.getText().toString().trim());
            settings.setCopies(copies);
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            Diagnostics.info(COMPONENT, "saveAll", "Settings saved");
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Copies must be a number (1-9)", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.ST_001, COMPONENT, "saveAll", e, "Invalid copies value");
        }
    }

    private void showDiagnostics() {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Diagnostics Log")
                .setMessage(Diagnostics.getFullReport())
                .setPositiveButton("OK", null)
                .setNegativeButton("Copy", (d, w) -> {
                    android.content.ClipboardManager cm =
                            (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Diagnostics",
                            Diagnostics.getFullReport()));
                    Toast.makeText(this, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Clear", (d, w) -> {
                    Diagnostics.clear();
                    Toast.makeText(this, "Diagnostics cleared", Toast.LENGTH_SHORT).show();
                })
                .create();
        dialog.show();
    }
}