package com.jnetai.thermal.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.jnetai.thermal.diagnostics.Diagnostics;
import com.jnetai.thermal.diagnostics.ErrorCodes;

public class Perms {
    public static final int REQ_BT = 4101;
    public static final int REQ_CAMERA = 4102;
    public static final int REQ_STORAGE = 4103;

    public static boolean hasBluetoothConnect(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static boolean hasCamera(Context ctx) {
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasStorage(Context ctx) {
        if (Build.VERSION.SDK_INT >= 30) return true;
        return ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void request(Activity act, String permission, int code) {
        try {
            ActivityCompat.requestPermissions(act, new String[]{permission}, code);
        } catch (Exception e) {
            Diagnostics.log(ErrorCodes.BT_002, "Perms", "request", e, "Permission=" + permission);
        }
    }

    public static void ensureBluetooth(Activity act) {
        if (!hasBluetoothConnect(act)) {
            request(act, android.Manifest.permission.BLUETOOTH_CONNECT, REQ_BT);
        }
    }

    public static void ensureCamera(Activity act) {
        if (!hasCamera(act)) {
            request(act, android.Manifest.permission.CAMERA, REQ_CAMERA);
        }
    }
}