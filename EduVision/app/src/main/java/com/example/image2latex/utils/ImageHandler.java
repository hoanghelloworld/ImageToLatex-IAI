package com.example.image2latex.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.Toast;

import androidx.viewbinding.ViewBinding;
import com.example.image2latex.databinding.ActivityConversionBinding;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.model.AspectRatio;
import java.util.function.Consumer;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageHandler {
    private static final int GALLERY_REQUEST_CODE = 103;
    private static final int CAMERA_REQUEST_CODE = 102;

    public static void handleActivityResult(Activity activity, int requestCode, int resultCode, 
            Intent data, ViewBinding binding, Uri cameraImageUri, 
            Consumer<Bitmap> processImage) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        try {
            if (requestCode == UCrop.REQUEST_CROP) {
                Uri resultUri = UCrop.getOutput(data);
                if (resultUri != null) {
                    Bitmap bitmap = getBitmapFromUri(activity, resultUri);
                    processImage.accept(bitmap);
                }
            } else if (requestCode == GALLERY_REQUEST_CODE || requestCode == CAMERA_REQUEST_CODE) {
                Uri sourceUri = (requestCode == GALLERY_REQUEST_CODE) ? data.getData() : cameraImageUri;
                if (sourceUri != null) {
                    startCrop(activity, sourceUri);
                }
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Error processing image: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private static Bitmap getBitmapFromUri(Activity activity, Uri uri) throws IOException {
        InputStream input = activity.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        options.inSampleSize = calculateSampleSize(options, 2048);
        options.inJustDecodeBounds = false;

        input = activity.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    private static void startCrop(Activity activity, Uri sourceUri) {
        Uri destinationUri = createCropDestinationUri(activity);
        UCrop uCrop = UCrop.of(sourceUri, destinationUri)
            .withOptions(getCropOptions(activity));
        uCrop.start(activity);
    }

    private static int calculateSampleSize(BitmapFactory.Options options, int maxSize) {
        int sampleSize = 1;
        if (options.outWidth > maxSize || options.outHeight > maxSize) {
            int halfWidth = options.outWidth / 2;
            int halfHeight = options.outHeight / 2;
            
            while ((halfWidth / sampleSize) >= maxSize || (halfHeight / sampleSize) >= maxSize) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    // Methods merged from ImageProcessor
    private static UCrop.Options getCropOptions(Context context) {
        UCrop.Options options = new UCrop.Options();
        options.setToolbarTitle("Crop Formula");
        options.setToolbarColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        options.setStatusBarColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        options.setActiveControlsWidgetColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        options.setFreeStyleCropEnabled(true);
        
        options.setAspectRatioOptions(0,
            new AspectRatio("Free", 0, 0),
            new AspectRatio("4:1", 4, 1),
            new AspectRatio("3:1", 3, 1),
            new AspectRatio("7:2", 7, 2),
            new AspectRatio("1:1", 1, 1)
        );
        return options;
    }

    private static Uri createCropDestinationUri(Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File destinationFile = new File(context.getCacheDir(), 
            "cropped_image_" + timeStamp + ".jpg");
        return Uri.fromFile(destinationFile);
    }
}