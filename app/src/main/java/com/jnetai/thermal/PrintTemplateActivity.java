package com.jnetai.thermal;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.core.PrintManager;
import com.jnetai.thermal.core.TemplateRenderer;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.ReceiptData;
import com.jnetai.thermal.model.Template;
import com.jnetai.thermal.store.ReceiptStore;
import com.jnetai.thermal.store.SettingsStore;
import com.jnetai.thermal.store.TemplateStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.ThemeUI;
import java.util.ArrayList;
import java.util.List;

public class PrintTemplateActivity extends AppCompatActivity {
    private static final String COMPONENT = "PrintTemplateActivity";
    private SettingsStore settings;
    private TemplateStore templateStore;
    private ReceiptStore receiptStore;
    private BluetoothHelper bt;

    private Template currentTemplate;
    private ReceiptData data = new ReceiptData();
    private List<ItemRow> itemRows = new ArrayList<>();

    private EditText storeInput, headerInput, footerInput, numberInput, cashierInput, taxInput, tenderedInput;
    private LinearLayout itemsContainer;
    private TextView previewLabel;
    private Spinner templateSpinner;
    private List<Template> templates;

    private static class ItemRow {
        EditText name, qty, price;
        ItemRow(EditText n, EditText q, EditText p) { name = n; qty = q; price = p; }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = SettingsStore.getInstance(this);
        templateStore = TemplateStore.getInstance(this);
        receiptStore = ReceiptStore.getInstance(this);
        bt = new BluetoothHelper(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);

        root.addView(ThemeUI.header(this, "Print Template (Receipt)"));
        root.addView(ThemeUI.info(this, "This is the default mode. Pick a template, fill in the details,\n"
                + "add items, then Print / Save / Email."));

        addSpinnerField(root, "Template");

        root.addView(ThemeUI.subHeader(this, "Receipt Details"));
        storeInput = ThemeUI.input(this, data.storeName, InputType.TYPE_CLASS_TEXT);
        root.addView(ThemeUI.label(this, "Store / Business Name"));
        root.addView(storeInput);
        headerInput = ThemeUI.input(this, data.header, InputType.TYPE_CLASS_TEXT);
        root.addView(ThemeUI.label(this, "Header line"));
        root.addView(headerInput);
        numberInput = ThemeUI.input(this, data.number, InputType.TYPE_CLASS_TEXT);
        root.addView(ThemeUI.label(this, "Receipt Number"));
        root.addView(numberInput);
        cashierInput = ThemeUI.input(this, data.cashier, InputType.TYPE_CLASS_TEXT);
        root.addView(ThemeUI.label(this, "Cashier"));
        root.addView(cashierInput);
        footerInput = ThemeUI.input(this, data.footer, InputType.TYPE_CLASS_TEXT);
        root.addView(ThemeUI.label(this, "Footer line"));
        root.addView(footerInput);

        root.addView(ThemeUI.subHeader(this, "Items"));
        itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemsContainer);

        Button addItemBtn = ThemeUI.secondaryButton(this, "+ Add Item");
        addItemBtn.setOnClickListener(v -> addItemRow());
        root.addView(addItemBtn);

        root.addView(ThemeUI.subHeader(this, "Totals"));
        taxInput = ThemeUI.input(this, "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(ThemeUI.label(this, "Tax"));
        root.addView(taxInput);
        tenderedInput = ThemeUI.input(this, "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(ThemeUI.label(this, "Cash Tendered"));
        root.addView(tenderedInput);

        previewLabel = ThemeUI.info(this, "Preview will appear here.");
        previewLabel.setMaxLines(25);
        root.addView(ThemeUI.subHeader(this, "Preview"));
        root.addView(previewLabel);

        root.addView(ThemeUI.button(this, "Print Receipt"), btnParams());
        root.getChildAt(root.getChildCount() - 1).setOnClickListener(v -> doPrint());
        root.addView(ThemeUI.secondaryButton(this, "Save Receipt (PDF/TXT)"));
        root.getChildAt(root.getChildCount() - 1).setOnClickListener(v -> doSave());
        root.addView(ThemeUI.secondaryButton(this, "Email Receipt"));
        root.getChildAt(root.getChildCount() - 1).setOnClickListener(v -> doEmail());
        root.addView(ThemeUI.secondaryButton(this, "Select Printer"));
        root.getChildAt(root.getChildCount() - 1).setOnClickListener(v -> startActivity(new Intent(this, PrinterSelectActivity.class)));

        scroll.addView(root);
        setContentView(scroll);
        loadTemplates();
    }

    private LinearLayout.LayoutParams btnParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private void addSpinnerField(LinearLayout root, String label) {
        root.addView(ThemeUI.label(this, label));
        templates = templateStore.loadAll();
        List<Template> recTemplates = new ArrayList<>();
        for (Template t : templates) {
            recTemplates.add(t);
        }
        String[] names = new String[recTemplates.size()];
        for (int i = 0; i < recTemplates.size(); i++) names[i] = recTemplates.get(i).name;
        templateSpinner = ThemeUI.spinnerWithListener(this, names.length == 0 ? new String[]{"No templates"} : names,
                names.length == 0 ? "No templates" : names[0], new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                        if (pos >= 0 && pos < recTemplates.size()) {
                            currentTemplate = recTemplates.get(pos);
                            onTemplateChanged();
                        }
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> p) {}
                });
        root.addView(templateSpinner);
    }

    private void loadTemplates() {
        templates = templateStore.loadAll();
        List<Template> recTemplates = new ArrayList<>();
        for (Template t : templates) recTemplates.add(t);
        String[] names = new String[recTemplates.size()];
        for (int i = 0; i < recTemplates.size(); i++) names[i] = recTemplates.get(i).name;
        templateSpinner.setAdapter(new android.widget.ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, names));
        if (recTemplates.isEmpty()) {
            currentTemplate = null;
            return;
        }
        templateSpinner.setSelection(0);
        currentTemplate = recTemplates.get(0);
        onTemplateChanged();
    }

    private void onTemplateChanged() {
        if (currentTemplate == null) return;
        updatePreview();
    }

    private void addItemRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, ThemeUI.dp(this, 4), 0, ThemeUI.dp(this, 4));

        EditText name = ThemeUI.input(this, "", InputType.TYPE_CLASS_TEXT);
        EditText qty = ThemeUI.input(this, "1", InputType.TYPE_CLASS_NUMBER);
        EditText price = ThemeUI.input(this, "0", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        name.setHint("Item name");
        qty.setHint("Qty");
        price.setHint("Price");

        name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
        qty.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        price.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button del = new Button(this);
        del.setText("X");
        del.setTextColor(0xFFFFFFFF);
        del.setBackgroundColor(ThemeUI.SURFACE);
        del.setAllCaps(false);
        row.addView(name);
        row.addView(qty);
        row.addView(price);
        row.addView(del);

        ItemRow ir = new ItemRow(name, qty, price);
        itemRows.add(ir);
        del.setOnClickListener(v -> {
            itemRows.remove(ir);
            itemsContainer.removeView(row);
            updatePreview();
        });

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) { updatePreview(); }
        };
        name.addTextChangedListener(watcher);
        qty.addTextChangedListener(watcher);
        price.addTextChangedListener(watcher);

        itemsContainer.addView(row);
        updatePreview();
    }

    private void syncDataFromInputs() {
        data.storeName = storeInput.getText().toString();
        data.header = headerInput.getText().toString();
        data.footer = footerInput.getText().toString();
        data.number = numberInput.getText().toString();
        data.cashier = cashierInput.getText().toString();
        data.items.clear();
        for (ItemRow row : itemRows) {
            String n = row.name.getText().toString().trim();
            if (n.isEmpty()) continue;
            String q = row.qty.getText().toString().trim();
            String p = row.price.getText().toString().trim();
            double price = 0;
            try { price = p.isEmpty() ? 0 : Double.parseDouble(p); } catch (NumberFormatException e) { price = 0; }
            data.items.add(new ReceiptData.ReceiptItem(n, q.isEmpty() ? "1" : q, price));
        }
        try { data.tax = taxInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(taxInput.getText().toString().trim()); }
        catch (NumberFormatException e) { data.tax = 0; }
        try { data.tendered = tenderedInput.getText().toString().trim().isEmpty() ? 0 : Double.parseDouble(tenderedInput.getText().toString().trim()); }
        catch (NumberFormatException e) { data.tendered = 0; }
        data.recomputeTotals();
    }

    private void updatePreview() {
        if (previewLabel == null || currentTemplate == null) return;
        syncDataFromInputs();
        try {
            String preview = TemplateRenderer.previewText(currentTemplate, data);
            previewLabel.setText(preview.isEmpty() ? "No preview content" : preview);
        } catch (Exception e) {
            previewLabel.setText("Preview error: " + e.getMessage());
            Diagnostics.log(ErrorCodes.PR_003, COMPONENT, "updatePreview", e, "Preview failed");
        }
    }

    private void doPrint() {
        if (currentTemplate == null) {
            Toast.makeText(this, "Create a template first", Toast.LENGTH_LONG).show();
            return;
        }
        syncDataFromInputs();
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        ThemeUI.hideKeyboard(this, storeInput);
        android.graphics.Bitmap logo = templateStore.loadLogo(currentTemplate.name);
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult r = pm.printTemplate(currentTemplate, data, logo);
        if (r == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "Receipt sent to printer", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + r.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_001, COMPONENT, "doPrint", "Result=" + r);
        }
    }

    private void doSave() {
        if (currentTemplate == null) {
            Toast.makeText(this, "Create a template first", Toast.LENGTH_LONG).show();
            return;
        }
        syncDataFromInputs();
        String rendered = TemplateRenderer.previewText(currentTemplate, data);
        String fileName = receiptStore.save(data, rendered, currentTemplate.isLabel,
                currentTemplate.isLabel ? currentTemplate.name : (data.storeName.isEmpty() ? "Receipt" : data.storeName));
        if (fileName == null) {
            Toast.makeText(this, "Could not save receipt", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.RC_001, COMPONENT, "doSave", "Save returned null");
            return;
        }
        String export = receiptStore.exportToDownloads(rendered, (data.storeName.isEmpty() ? "receipt" : data.storeName) + "_" + fileName.replace(".json", ""));
        String msg = "Receipt saved" + (export != null ? " to Downloads" : " (internal only)");
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        Diagnostics.info(COMPONENT, "doSave", "Saved " + fileName + " export=" + export);
    }

    private void doEmail() {
        if (currentTemplate == null) {
            Toast.makeText(this, "Create a template first", Toast.LENGTH_LONG).show();
            return;
        }
        syncDataFromInputs();
        String rendered = TemplateRenderer.previewText(currentTemplate, data);
        try {
            Intent email = new Intent(Intent.ACTION_SEND);
            email.setType("text/plain");
            email.putExtra(Intent.EXTRA_SUBJECT, "Receipt " + (data.number.isEmpty() ? "" : data.number + " ") + (data.storeName.isEmpty() ? "from J~Net Thermal Printer" : data.storeName));
            email.putExtra(Intent.EXTRA_TEXT, rendered);
            startActivity(Intent.createChooser(email, "Send Receipt via Email"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.RC_003, COMPONENT, "doEmail", e, "Email intent failed");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}