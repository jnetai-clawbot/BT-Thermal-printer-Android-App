package com.jnetai.thermal;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.Template;
import com.jnetai.thermal.store.TemplateStore;
import com.jnetai.thermal.util.ThemeUI;
import java.util.List;

public class TemplateListActivity extends AppCompatActivity {
    private static final String COMPONENT = "TemplateListActivity";
    private TemplateStore store;
    private LinearLayout listLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = TemplateStore.getInstance(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);

        root.addView(ThemeUI.header(this, "Templates"));
        root.addView(ThemeUI.info(this, "Create receipt and label templates, then edit their layout lines.\n"
                + "Print Template (default mode) uses the selected template style."));

        Button newBtn = ThemeUI.button(this, "+ New Receipt Template");
        newBtn.setOnClickListener(v -> createTemplate(false));
        root.addView(newBtn);

        Button newLabelBtn = ThemeUI.secondaryButton(this, "+ New Label Template");
        newLabelBtn.setOnClickListener(v -> createTemplate(true));
        root.addView(newLabelBtn);

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);

        root.addView(ThemeUI.info(this, "Template line placeholders:\n"
                + "{store} {header} {footer} {number} {cashier} {date} {time} {title}\n"
                + "{subtotal} {tax} {total} {cash} {change} {qty} {items}"));

        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        listLayout.removeAllViews();
        List<Template> templates = store.loadAll();
        if (templates.isEmpty()) {
            listLayout.addView(ThemeUI.info(this, "No templates yet - create one above."));
            return;
        }
        for (Template t : templates) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10));
            card.setBackground(ThemeUI.rounded(ThemeUI.CARD, ThemeUI.dp(this, 10)));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, ThemeUI.dp(this, 6), 0, ThemeUI.dp(this, 6));
            card.setLayoutParams(cardLp);

            TextView name = new TextView(this);
            name.setText(t.name + (t.isLabel ? "  [LABEL " + t.labelWidthMm + "x" + t.labelHeightMm + "mm]"
                    : "  [" + t.widthMm + "mm]"));
            name.setTextSize(16);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            name.setTextColor(ThemeUI.TEXT_WHITE);
            card.addView(name);

            TextView lines = new TextView(this);
            lines.setText(t.lines.size() + " lines" + (t.logoEnabled ? "  ·  logo" : ""));
            lines.setTextSize(12);
            lines.setTextColor(ThemeUI.TEXT_MUTED);
            lines.setPadding(0, 0, 0, ThemeUI.dp(this, 4));
            card.addView(lines);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            addRowButton(row, "Edit", () -> editTemplate(t));
            addRowButton(row, "Duplicate", () -> duplicateTemplate(t));
            addRowButton(row, "Delete", () -> deleteTemplate(t));
            card.addView(row);

            listLayout.addView(card);
        }
    }

    private void addRowButton(LinearLayout row, String label, Runnable action) {
        Button b = ThemeUI.secondaryButton(row.getContext(), label);
        b.setTextSize(13);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(ThemeUI.dp(this, 2), 0, ThemeUI.dp(this, 2), 0);
        b.setLayoutParams(lp);
        row.addView(b);
    }

    private void createTemplate(boolean label) {
        final android.widget.EditText input = ThemeUI.input(this, label ? "My Label" : "My Template",
                android.text.InputType.TYPE_CLASS_TEXT);
        new android.app.AlertDialog.Builder(this)
                .setTitle(label ? "New Label Template" : "New Receipt Template")
                .setView(wrapInContainer(input))
                .setPositiveButton("Create", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Template t = new Template();
                    t.name = name;
                    t.isLabel = label;
                    if (label) {
                        t.labelWidthMm = 58;
                        t.labelHeightMm = 40;
                    }
                    Template.TemplateLine h = new Template.TemplateLine();
                    h.kind = "text";
                    h.text = label ? "{header}" : "{store}";
                    h.align = 1;
                    h.size = 2;
                    h.bold = true;
                    t.lines.add(h);
                    Template.TemplateLine items = new Template.TemplateLine();
                    items.kind = "items";
                    t.lines.add(items);
                    if (store.save(t)) {
                        Toast.makeText(this, "Template created", Toast.LENGTH_SHORT).show();
                        editTemplate(t);
                    } else {
                        Toast.makeText(this, "Template name already exists", Toast.LENGTH_LONG).show();
                        Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "createTemplate", "Save returned false for " + name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private android.widget.LinearLayout wrapInContainer(android.widget.EditText input) {
        android.widget.LinearLayout wrap = new android.widget.LinearLayout(this);
        wrap.setOrientation(android.widget.LinearLayout.VERTICAL);
        wrap.setPadding(ThemeUI.dp(this, 20), ThemeUI.dp(this, 8), ThemeUI.dp(this, 20), 0);
        wrap.addView(input);
        return wrap;
    }

    private void editTemplate(Template t) {
        startActivity(new Intent(this, TemplateEditActivity.class)
                .putExtra("template_name", t.name));
    }

    private void duplicateTemplate(Template t) {
        Template copy = t.cloneTemplate();
        copy.name = t.name + " Copy";
        if (store.save(copy)) {
            Toast.makeText(this, "Duplicated as: " + copy.name, Toast.LENGTH_SHORT).show();
            refreshList();
        } else {
            Toast.makeText(this, "Duplicate failed (name may already exist)", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "duplicateTemplate", "Save copy failed for " + copy.name);
        }
    }

    private void deleteTemplate(Template t) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Template")
                .setMessage("Delete template '" + t.name + "'? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    store.delete(t.name);
                    store.deleteLogo(t.name);
                    Toast.makeText(this, "Template deleted", Toast.LENGTH_SHORT).show();
                    refreshList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}