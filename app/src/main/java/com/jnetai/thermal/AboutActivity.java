package com.jnetai.thermal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import com.jnetai.thermal.update.UpdateChecker;
import com.jnetai.thermal.util.ThemeUI;

public class AboutActivity extends AppCompatActivity {
    private static final String COMPONENT = "AboutActivity";
    private static final String REPO_OWNER = "jnetai-clawbot";
    private static final String REPO_NAME = "BT-Thermal-printer-Android-App";

    private String versionName = "unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Diagnostics.log(ErrorCodes.GE_001, COMPONENT, "onCreate", e, "Version lookup failed");
        }

        androidx.core.widget.NestedScrollView scroll = new androidx.core.widget.NestedScrollView(this);
        scroll.setBackgroundColor(ThemeUI.BG_DARK);
        LinearLayout root = ThemeUI.vertical(this);
        root.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        TextView appTitle = ThemeUI.header(this, "J~Net Thermal Printer");
        appTitle.setGravity(android.view.Gravity.CENTER);
        root.addView(appTitle);

        TextView madeBy = new TextView(this);
        madeBy.setText("Made by jnetai.com");
        madeBy.setTextSize(16);
        madeBy.setTextColor(ThemeUI.PRIMARY_LIGHT);
        madeBy.setGravity(android.view.Gravity.CENTER);
        madeBy.setPadding(0, 0, 0, ThemeUI.dp(this, 8));
        root.addView(madeBy);

        TextView ver = new TextView(this);
        ver.setText("Version " + versionName);
        ver.setTextSize(15);
        ver.setTextColor(ThemeUI.TEXT_WHITE);
        ver.setGravity(android.view.Gravity.CENTER);
        ver.setPadding(0, 0, 0, ThemeUI.dp(this, 20));
        root.addView(ver);

        Button updateBtn = ThemeUI.button(this, "Check for Update");
        updateBtn.setOnClickListener(v -> checkForUpdate());
        root.addView(updateBtn);

        Button shareBtn = ThemeUI.secondaryButton(this, "Share App");
        shareBtn.setOnClickListener(v -> shareApp());
        root.addView(shareBtn);

        Button releasesBtn = ThemeUI.secondaryButton(this, "Open GitHub Releases");
        releasesBtn.setOnClickListener(v -> openUrl(UpdateChecker.FALLOVER_REPO_URL(REPO_OWNER, REPO_NAME)));
        root.addView(releasesBtn);

        TextView desc = ThemeUI.info(this, "Bluetooth thermal receipt printer app (PT210 compatible).\n"
                + "Receipts, labels, QR codes, image & file printing.\n"
                + "For support visit jnetai.com");
        desc.setGravity(android.view.Gravity.CENTER);
        desc.setPadding(0, ThemeUI.dp(this, 20), 0, 0);
        root.addView(desc);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void checkForUpdate() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
        new UpdateChecker(REPO_OWNER, REPO_NAME, versionName).checkForUpdate(new UpdateChecker.UpdateCallback() {
            @Override
            public void onResult(boolean updateAvailable, String latestTag, String releaseUrl) {
                AboutActivity.this.runOnUiThread(() -> {
                    if (updateAvailable) {
                        new android.app.AlertDialog.Builder(AboutActivity.this)
                                .setTitle("Update Available")
                                .setMessage("Latest version is " + latestTag + ".\nYour version: " + versionName)
                                .setPositiveButton("Update", (d, w) -> openUrl(releaseUrl))
                                .setNegativeButton("Never mind", null)
                                .show();
                    } else {
                        Toast.makeText(AboutActivity.this, "You're on the latest version (" + versionName + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                AboutActivity.this.runOnUiThread(() -> {
                    Toast.makeText(AboutActivity.this, "Update check failed - opening releases page", Toast.LENGTH_LONG).show();
                    openUrl(UpdateChecker.FALLOVER_REPO_URL(REPO_OWNER, REPO_NAME));
                });
            }
        });
    }

    private void shareApp() {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "J~Net Thermal Printer");
            share.putExtra(Intent.EXTRA_TEXT, "Check out J~Net Thermal Printer - Bluetooth thermal receipt printer app!\n"
                    + UpdateChecker.FALLOVER_REPO_URL(REPO_OWNER, REPO_NAME));
            startActivity(Intent.createChooser(share, "Share J~Net Thermal Printer"));
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.SHR_001, COMPONENT, "shareApp", e, "Share failed");
            Toast.makeText(this, "Could not open share dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.SHR_001, COMPONENT, "openUrl", e, "URL=" + url);
            Toast.makeText(this, "Could not open " + url, Toast.LENGTH_LONG).show();
        }
    }
}