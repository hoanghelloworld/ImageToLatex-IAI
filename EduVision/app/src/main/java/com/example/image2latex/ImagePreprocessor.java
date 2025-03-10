package com.example.image2latex;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
public class ImagePreprocessor {
    public static float[] bitmapToTensor(Bitmap originalBitmap) {
        try {
            Bitmap grayscaleBitmap = toGrayscale(originalBitmap);
            // Resize to (700,150)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, 700, 150, true);
            
            // Create tensor data of shape [150,700]
            float[] tensorData = new float[150 * 700];
            
            // Calculate mean value
            float meanValue = 0;
            for (int y = 0; y < 150; y++) {
                for (int x = 0; x < 700; x++) {
                    int pixel = resizedBitmap.getPixel(x, y);
                    float value = (pixel & 0xFF) / 255.0f;
                    meanValue += value;
                }
            }
            meanValue /= (150 * 700);
            
            // Process image: loop only over height and width
            int index = 0;
            for (int h = 0; h < 150; h++) {
                for (int w = 0; w < 700; w++) {
                    int pixel = resizedBitmap.getPixel(w, h);
                    float value = (pixel & 0xFF) / 255.0f;
                    if (meanValue > 0.5f) {
                        value = 1.0f - value;
                    }
                    // Binarize
                    tensorData[index++] = value > 0.5f ? 1.0f : 0.0f;
                }
            }
            
            // Clean up Bitmaps
            if (grayscaleBitmap != originalBitmap) {
                grayscaleBitmap.recycle();
            }
            if (resizedBitmap != grayscaleBitmap) {
                resizedBitmap.recycle();
            }
            return tensorData;
        } catch (Exception e) {
            throw new RuntimeException("Error preprocessing image: " + e.getMessage(), e);
        }
    }

    private static Bitmap toGrayscale(Bitmap sourceBitmap) {
        int width = sourceBitmap.getWidth();
        int height = sourceBitmap.getHeight();
        Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        float contrast = 2.0f;
        float shift = -0.5f * (contrast - 1.0f) * 255.0f;
        colorMatrix.postConcat(new ColorMatrix(new float[] {
            contrast, 0, 0, 0, shift,
            0, contrast, 0, 0, shift,
            0, 0, contrast, 0, shift,
            0, 0, 0, 1, 0
        }));
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(sourceBitmap, 0, 0, paint);
        return grayscaleBitmap;
    }
}