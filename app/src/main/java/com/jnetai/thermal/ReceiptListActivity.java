package com.jnetai.thermal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.core.BluetoothHelper;
import com.jnetai.thermal.core.PrintManager;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.store.ReceiptStore;
import com.jnetai.thermal.util.Perms;
import com.jnetai.thermal.util.ThemeUI;
import java.util.List;

public class ReceiptListActivity extends AppCompatActivity {
    private static final String COMPONENT = "ReceiptListActivity";
    private ReceiptStore store;
    private LinearLayout listLayout;
    private BluetoothHelper bt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = ReceiptStore.getInstance(this);
        bt = new BluetoothHelper(this);

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);

        root.addView(ThemeUI.header(this, "Saved Receipts"));
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        listLayout.removeAllViews();
        List<ReceiptStore.SavedReceipt> receipts = store.loadAll();
        if (receipts.isEmpty()) {
            listLayout.addView(ThemeUI.info(this, "No saved receipts yet.\nCreate receipts in Print Template mode and tap Save."));
            return;
        }
        for (ReceiptStore.SavedReceipt r : receipts) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10), ThemeUI.dp(this, 10));
            card.setBackground(ThemeUI.rounded(ThemeUI.CARD, ThemeUI.dp(this, 10)));
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, ThemeUI.dp(this, 6), 0, ThemeUI.dp(this, 6));
            card.setLayoutParams(cardLp);

            TextView title = new TextView(this);
            title.setText((r.title == null || r.title.isEmpty() ? "Receipt" : r.title)
                    + (r.labeled ? " (label)" : ""));
            title.setTextSize(15);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            title.setTextColor(ThemeUI.TEXT_WHITE);
            card.addView(title);

            TextView date = new TextView(this);
            date.setText(r.saveDate != null ? r.saveDate : "");
            date.setTextSize(12);
            date.setTextColor(ThemeUI.TEXT_MUTED);
            card.addView(date);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            addCardButton(row, "View", () -> showReceipt(r));
            addCardButton(row, "Print", () -> printReceipt(r));
            addCardButton(row, "Email", () -> emailReceipt(r));
            addCardButton(row, "Export", () -> exportReceipt(r));
            addCardButton(row, "Delete", () -> deleteReceipt(r));
            card.addView(row);
            listLayout.addView(card);
        }
    }

    private void addCardButton(LinearLayout row, String label, Runnable action) {
        Button b = ThemeUI.secondaryButton(this, label);
        b.setTextSize(12);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(ThemeUI.dp(this, 2), ThemeUI.dp(this, 2), ThemeUI.dp(this, 2), 0);
        b.setLayoutParams(lp);
        row.addView(b);
    }

    private void showReceipt(ReceiptStore.SavedReceipt r) {
        new android.app.AlertDialog.Builder(this)
                .setTitle(r.title == null ? "Receipt" : r.title)
                .setMessage(r.renderedText == null ? "No content" : r.renderedText)
                .setPositiveButton("OK", null)
                .show();
    }

    private void printReceipt(ReceiptStore.SavedReceipt r) {
        if (!Perms.hasBluetoothConnect(this)) {
            Perms.ensureBluetooth(this);
            return;
        }
        if (r.renderedText == null) {
            Toast.makeText(this, "Receipt has no printable content", Toast.LENGTH_SHORT).show();
            return;
        }
        PrintManager pm = new PrintManager(this, bt);
        PrintManager.PrintResult res = pm.printText(r.renderedText);
        if (res == PrintManager.PrintResult.SUCCESS) {
            Toast.makeText(this, "Receipt sent to printer", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Print failed: " + res.name(), Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.PR_001, COMPONENT, "printReceipt", "Result=" + res);
        }
    }

    private void emailReceipt(ReceiptStore.SavedReceipt r) {
        try {
            Intent email = new Intent(Intent.ACTION_SEND);
            email.setType("text/plain");
            email.putExtra(Intent.EXTRA_SUBJECT, "Receipt " + (r.title == null ? "" : r.title));
            email.putExtra(Intent.EXTRA_TEXT, r.renderedText == null ? "No content" : r.renderedText);
            startActivity(Intent.createChooser(email, "Email Receipt"));
        } catch (Exception e) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.RC_003, COMPONENT, "emailReceipt", e, "Email failed");
        }
    }

    private void exportReceipt(ReceiptStore.SavedReceipt r) {
        String out = store.exportToDownloads(r.renderedText == null ? "No content" : r.renderedText,
                (r.title == null || r.title.isEmpty() ? "receipt" : r.title) + "_" + r.fileName.replace(".json", ""));
        if (out != null) {
            Toast.makeText(this, "Exported: " + out, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Export failed", Toast.LENGTH_LONG).show();
            Diagnostics.log(ErrorCodes.FL_002, COMPONENT, "exportReceipt", "Export returned null");
        }
    }

    private void deleteReceipt(ReceiptStore.SavedReceipt r) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Receipt")
                .setMessage("Delete this saved receipt?")
                .setPositiveButton("Delete", (d, w) -> {
                    store.delete(r.fileName);
                    refresh();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.close();
    }
}