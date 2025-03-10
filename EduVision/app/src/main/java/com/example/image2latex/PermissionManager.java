package com.example.image2latex.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {
    
    // Permission checking methods from PermissionUtils
    public static boolean checkStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, 
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public static boolean checkCameraPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, 
            Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getStoragePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return Manifest.permission.READ_MEDIA_IMAGES;
        }
        return Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    // Permission request and result handling from PermissionManager
    public static boolean checkPermission(Activity activity, String permission, int requestCode) {
        if (Build.VERSION.SDK_INT >= 33 && 
            Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        }
        
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    public static void handlePermissionResult(Activity activity, int requestCode, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Permission granted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}