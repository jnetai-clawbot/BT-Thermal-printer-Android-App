package com.jnetai.thermal.update;

import android.os.AsyncTask;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {
    private final String repoOwner;
    private final String repoName;
    private final String currentVersion;

    public UpdateChecker(String repoOwner, String repoName, String currentVersion) {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.currentVersion = currentVersion;
    }

    public interface UpdateCallback {
        void onResult(boolean updateAvailable, String latestTag, String releaseUrl);
        void onError(String message);
    }

    public void checkForUpdate(UpdateCallback callback) {
        AsyncTask.execute(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    Diagnostics.log(ErrorCodes.UP_002, "UpdateChecker", "checkForUpdate", "HTTP " + code);
                    callback.onError("Update check failed: HTTP " + code);
                    return;
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                }

                String json = response.toString();
                String latestTag = extractTag(json);
                String htmlUrl = extractHtmlUrl(json);

                if (latestTag == null) {
                    Diagnostics.log(ErrorCodes.UP_003, "UpdateChecker", "checkForUpdate", "Could not parse tag from response");
                    callback.onError("Could not parse release information");
                    return;
                }

                boolean updateAvailable = !currentVersion.equals(latestTag);
                callback.onResult(updateAvailable, latestTag, htmlUrl);

            } catch (Exception e) {
                Diagnostics.log(ErrorCodes.UP_001, "UpdateChecker", "checkForUpdate", e, "Network error");
                callback.onError("Network error: " + e.getMessage());
            }
        });
    }

    private String extractTag(String json) {
        try {
            int idx = json.indexOf("\"tag_name\"");
            if (idx < 0) return null;
            int colon = json.indexOf(":", idx);
            if (colon < 0) return null;
            int start = json.indexOf("\"", colon);
            if (start < 0) return null;
            int end = json.indexOf("\"", start + 1);
            if (end < 0) return null;
            return json.substring(start + 1, end);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractHtmlUrl(String json) {
        try {
            int idx = json.indexOf("\"html_url\"");
            if (idx < 0) return null;
            int colon = json.indexOf(":", idx);
            if (colon < 0) return null;
            int start = json.indexOf("\"", colon);
            if (start < 0) return null;
            int end = json.indexOf("\"", start + 1);
            if (end < 0) return null;
            return json.substring(start + 1, end);
        } catch (Exception e) {
            return null;
        }
    }

    public static String FALLOVER_REPO_URL(String repoOwner, String repoName) {
        return "https://github.com/" + repoOwner + "/" + repoName + "/releases";
    }
}