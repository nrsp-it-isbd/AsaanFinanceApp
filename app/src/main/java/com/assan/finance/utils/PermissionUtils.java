package com.assan.finance.utils;

import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class PermissionUtils {

    private static final int REQUEST_PERMISSION_CODE = 1001;

    // This method checks if permissions are granted or not
    public static boolean checkPermission(Activity activity, String permission) {
        return ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    // This method requests permissions from the user
    public static void requestPermission(Activity activity, String permission) {
        ActivityCompat.requestPermissions(activity, new String[]{permission}, REQUEST_PERMISSION_CODE);
    }

    // This method handles permission results
    public static boolean handlePermissionResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            // Permission granted
            // Permission denied
            return grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    // Example method to request multiple permissions
    public static void requestMultiplePermissions(Activity activity, String[] permissions) {
        ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSION_CODE);
    }
}
