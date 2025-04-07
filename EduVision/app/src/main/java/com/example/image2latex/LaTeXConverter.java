package com.example.image2latex;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.image2latex.utils.ApiClient;

import org.json.JSONException;

import java.io.IOException;

public class LaTeXConverter {
    private static final String TAG = "LaTeXConverter";
    private volatile boolean cancelRequested = false;
    private final ApiClient apiClient;
    
    public LaTeXConverter(Context context) {
        apiClient = new ApiClient(context);
        Log.d(TAG, "LaTeXConverter initialized with API client");
    }
    
    public void cancelConversion() {
        cancelRequested = true;
        Log.d(TAG, "Cancellation requested");
    }
    
    public void resetCancellation() {
        cancelRequested = false;
    }
    
    /**
     * Convert image data to LaTeX via server API
     * @param bitmap The image to convert
     * @return The LaTeX representation of the image
     */
    public String convert(Bitmap bitmap) {
        resetCancellation();
        Log.d(TAG, "Starting conversion via API...");
        
        try {
            if (cancelRequested) {
                return "Conversion cancelled";
            }
            
            // Send the image to the server for processing
            String result = apiClient.convertImageToLatex(bitmap);
            Log.d(TAG, "Conversion completed successfully");
            return result;
        } catch (IOException e) {
            Log.e(TAG, "IO error during conversion", e);
            return "Error: " + e.getMessage();
        } catch (JSONException e) {
            Log.e(TAG, "JSON error during conversion", e);
            return "Error: Invalid response from server";
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error during conversion", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Convert preprocessed tensor data to LaTeX via server API
     * This method is kept for compatibility with existing code
     * @param tensorData The preprocessed image data
     * @return The LaTeX representation of the image
     */
    public String convert(float[] tensorData) {
        Log.d(TAG, "Tensor data received, but this method is not used in server mode");
        return "Error: Direct tensor processing not supported in server mode";
    }
}