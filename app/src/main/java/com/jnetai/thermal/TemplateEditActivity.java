package com.jnetai.thermal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.model.Template;
import com.jnetai.thermal.store.TemplateStore;
import com.jnetai.thermal.util.ThemeUI;

public class TemplateEditActivity extends AppCompatActivity {
    private static final String COMPONENT = "TemplateEditActivity";
    private TemplateStore store;
    private Template template;
    private String originalName;
    private LinearLayout content;
    private ImageView logoPreview;

    private final ActivityResultLauncher<Intent> logoPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    setLogo(result.getData().getData());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = TemplateStore.getInstance(this);
        originalName = getIntent().getStringExtra("template_name");
        template = store.load(originalName);
        if (template == null) {
            Toast.makeText(this, "Template not found", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        content = ThemeUI.vertical(this);
        scroll.addView(content);
        setContentView(scroll);
        buildUI();
    }

    private void buildUI() {
        content.removeAllViews();
        content.addView(ThemeUI.header(this, "Edit Template: " + template.name));

        content.addView(ThemeUI.subHeader(this, "Template Details"));
        final EditText nameInput = ThemeUI.input(this, template.name, android.text.InputType.TYPE_CLASS_TEXT);
        content.addView(ThemeUI.label(this, "Name"));
        content.addView(nameInput);

        Button renameBtn = ThemeUI.secondaryButton(this, "Save Name & Details");
        renameBtn.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(originalName) && store.load(newName) != null) {
                Toast.makeText(this, "A template with that name already exists", Toast.LENGTH_LONG).show();
                return;
            }
            String oldName = template.name;
            template.name = newName.isEmpty() ? oldName : newName;
            originalName = template.name;
            if (store.save(template)) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                buildUI();
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_LONG).show();
                Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "saveDetails", "Save failed for " + template.name);
            }
        });
        content.addView(renameBtn);

        content.addView(ThemeUI.subHeader(this, "Logo"));
        final android.widget.Switch logoSwitch = ThemeUI.toggle(this, template.logoEnabled, (b, on) -> {
            template.logoEnabled = on;
            store.save(template);
        });
        LinearLayout logoRow = ThemeUI.switchRow(this, "Print logo at top", template.logoEnabled, (b, on) -> {
            template.logoEnabled = on;
            store.save(template);
            logoSwitch.setChecked(on);
        });
        content.addView(logoRow);

        logoPreview = new ImageView(this);
        logoPreview.setAdjustViewBounds(true);
        logoPreview.setMaxHeight(ThemeUI.dp(this, 120));
        refreshLogo();
        content.addView(logoPreview);

        Button chooseLogoBtn = ThemeUI.secondaryButton(this, "Choose Logo Image");
        chooseLogoBtn.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType("image/*");
            logoPicker.launch(pick);
        });
        content.addView(chooseLogoBtn);

        Button clearLogoBtn = ThemeUI.secondaryButton(this, "Remove Logo");
        clearLogoBtn.setOnClickListener(v -> {
            store.deleteLogo(template.name);
            template.logoEnabled = false;
            store.save(template);
            refreshLogo();
            Toast.makeText(this, "Logo removed", Toast.LENGTH_SHORT).show();
        });
        content.addView(clearLogoBtn);

        content.addView(ThemeUI.subHeader(this, "Layout"));
        if (template.isLabel) {
            content.addView(ThemeUI.info(this, "Label width: " + template.labelWidthMm + "mm  x  height: " + template.labelHeightMm + "mm  (edit in Settings)"));
        } else {
            content.addView(ThemeUI.info(this, "Paper width: " + template.widthMm + "mm  (edit in Settings)"));
        }

        content.addView(ThemeUI.subHeader(this, "Lines"));
        rebuildLines();
    }

    private void rebuildLines() {
        int start = findHeaderIndex("Lines");
        while (content.getChildCount() > start + 1) {
            content.removeViewAt(content.getChildCount() - 1);
        }
        if (template.lines.isEmpty()) {
            content.addView(ThemeUI.info(this, "No lines yet. Add a line below."));
        }
        for (int i = 0; i < template.lines.size(); i++) {
            final Template.TemplateLine line = template.lines.get(i);
            final int idx = i;
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10));
            card.setBackground(ThemeUI.rounded(ThemeUI.CARD, ThemeUI.dp(this, 8)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, ThemeUI.dp(this, 6), 0, ThemeUI.dp(this, 6));
            card.setLayoutParams(lp);

            String typeLabel = "spacer".equals(line.kind) ? "Spacer x" + line.spacerCount
                    : "items".equals(line.kind) ? "ITEM LIST"
                    : (line.text == null || line.text.isEmpty()) ? "(empty text)"
                    : truncate(line.text, 34);
            TextView title = new TextView(this);
            title.setText((idx + 1) + ". " + typeLabel + "   " + lineStyleDesc(line));
            title.setTextSize(14);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setTextColor(ThemeUI.TEXT_WHITE);
            card.addView(title);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            addSmallButton(row, "Edit", () -> editLine(idx));
            addSmallButton(row, "Up", () -> move(idx, -1));
            addSmallButton(row, "Down", () -> move(idx, 1));
            addSmallButton(row, "Delete", () -> {
                template.lines.remove(idx);
                store.save(template);
                rebuildLines();
            });
            card.addView(row);
            content.addView(card);
        }

        Button addTextBtn = ThemeUI.secondaryButton(this, "+ Add Text Line");
        addTextBtn.setOnClickListener(v -> addLine("text"));
        content.addView(addTextBtn);

        Button addItemsBtn = ThemeUI.secondaryButton(this, "+ Add Item List Line");
        addItemsBtn.setOnClickListener(v -> addLine("items"));
        content.addView(addItemsBtn);

        Button addSpacerBtn = ThemeUI.secondaryButton(this, "+ Add Spacer");
        addSpacerBtn.setOnClickListener(v -> {
            Template.TemplateLine l = new Template.TemplateLine();
            l.kind = "spacer";
            l.spacerCount = 1;
            template.lines.add(l);
            store.save(template);
            rebuildLines();
        });
        content.addView(addSpacerBtn);

        Button doneBtn = ThemeUI.button(this, "Done");
        doneBtn.setOnClickListener(v -> finish());
        content.addView(doneBtn);
    }

    private int findHeaderIndex(String title) {
        for (int i = 0; i < content.getChildCount(); i++) {
            android.view.View v = content.getChildAt(i);
            if (v instanceof android.widget.TextView) {
                String s = ((android.widget.TextView) v).getText().toString();
                if (s.equals(title)) return i;
            }
        }
        return content.getChildCount() - 1;
    }

    private String lineStyleDesc(Template.TemplateLine line) {
        StringBuilder sb = new StringBuilder();
        if (line.size > 1) sb.append("x").append(line.size).append(" ");
        if (line.bold) sb.append("B ");
        if (line.underline) sb.append("U ");
        sb.append(line.align == 1 ? "C" : line.align == 2 ? "R" : "L");
        if (line.dash) sb.append(" dash");
        return sb.toString().trim();
    }

    private String truncate(String s, int n) {
        return s.length() > n ? s.substring(0, n) + "..." : s;
    }

    private void addSmallButton(LinearLayout row, String label, Runnable action) {
        Button b = ThemeUI.secondaryButton(this, label);
        b.setTextSize(12);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(ThemeUI.dp(this, 2), ThemeUI.dp(this, 2), ThemeUI.dp(this, 2), 0);
        b.setLayoutParams(lp);
        row.addView(b);
    }

    private void move(int idx, int dir) {
        int target = idx + dir;
        if (target < 0 || target >= template.lines.size()) return;
        Template.TemplateLine line = template.lines.remove(idx);
        template.lines.add(target, line);
        store.save(template);
        rebuildLines();
    }

    private void addLine(String kind) {
        Template.TemplateLine l = new Template.TemplateLine();
        l.kind = kind;
        l.text = "items".equals(kind) ? "" : kind.equals("text") ? "{store}" : "";
        template.lines.add(l);
        store.save(template);
        editLine(template.lines.size() - 1);
    }

    private void editLine(final int idx) {
        final Template.TemplateLine line = template.lines.get(idx);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(ThemeUI.dp(this, 20), ThemeUI.dp(this, 8), ThemeUI.dp(this, 20), 0);

        final EditText textInput = ThemeUI.input(this, line.text, android.text.InputType.TYPE_CLASS_TEXT);
        panel.addView(ThemeUI.label(this, "Text (placeholders supported)"));
        panel.addView(textInput);

        String[] kinds = {"text", "items", "spacer"};
        final Spinner kindSp = ThemeUI.spinnerWithListener(this, kinds, line.kind, null);
        panel.addView(ThemeUI.label(this, "Kind"));
        panel.addView(kindSp);

        String[] aligns = {"Left", "Center", "Right"};
        final Spinner alignSp = ThemeUI.spinnerWithListener(this, aligns, aligns[line.align], null);
        panel.addView(ThemeUI.label(this, "Alignment"));
        panel.addView(alignSp);

        String[] sizes = {"Normal", "Large", "Extra Large", "Huge"};
        final Spinner sizeSp = ThemeUI.spinnerWithListener(this, sizes, sizes[Math.min(line.size - 1, 3)], null);
        panel.addView(ThemeUI.label(this, "Font Size"));
        panel.addView(sizeSp);

        final android.widget.Switch boldSw = ThemeUI.toggle(this, line.bold, null);
        panel.addView(ThemeUI.switchRow(this, "Bold", line.bold, (b, on) -> boldSw.setChecked(on)));
        panel.addView(boldSw);

        final android.widget.Switch ulSw = ThemeUI.toggle(this, line.underline, null);
        panel.addView(ThemeUI.switchRow(this, "Underline", line.underline, (b, on) -> ulSw.setChecked(on)));
        panel.addView(ulSw);

        final android.widget.Switch dashSw = ThemeUI.toggle(this, line.dash, null);
        panel.addView(ThemeUI.switchRow(this, "Dashed line", line.dash, (b, on) -> dashSw.setChecked(on)));
        panel.addView(dashSw);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Edit line " + (idx + 1))
                .setView(panel)
                .setPositiveButton("Save", (d, w) -> {
                    line.text = textInput.getText().toString();
                    line.kind = (String) kindSp.getSelectedItem();
                    line.align = alignSp.getSelectedItemPosition();
                    line.size = sizeSp.getSelectedItemPosition() + 1;
                    line.bold = boldSw.isChecked();
                    line.underline = ulSw.isChecked();
                    line.dash = dashSw.isChecked();
                    if ("spacer".equals(line.kind) && line.spacerCount < 1) line.spacerCount = 1;
                    if (!store.save(template)) {
                        Diagnostics.log(ErrorCodes.TM_002, COMPONENT, "editLine", "Save failed");
                        Toast.makeText(this, "Save failed", Toast.LENGTH_LONG).show();
                    }
                    rebuildLines();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLogo(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
            is.close();
            if (bmp == null) {
                Toast.makeText(this, "Could not decode logo image", Toast.LENGTH_LONG).show();
                Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "setLogo", "Decode failed");
                return;
            }
            if (store.saveLogo(template.name, bmp)) {
                template.logoEnabled = true;
                store.save(template);
                refreshLogo();
                Toast.makeText(this, "Logo set", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Logo load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.IM_001, COMPONENT, "setLogo", e, "Uri=" + uri);
        }
    }

    private void refreshLogo() {
        if (logoPreview == null) return;
        android.graphics.Bitmap logo = store.loadLogo(template.name);
        if (logo != null) {
            logoPreview.setImageBitmap(logo);
        } else {
            logoPreview.setImageDrawable(null);
        }
    }
}