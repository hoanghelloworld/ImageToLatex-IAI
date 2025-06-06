package com.example.image2latex.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final int TIMEOUT_SECONDS = 90;
    
    private final OkHttpClient client;
    private final ServerConfig serverConfig;
    
    public ApiClient(Context context) {
        // Configure OkHttpClient with timeouts
        client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
        
        serverConfig = ServerConfig.getInstance(context);
    }
    
    /**
     * Convert a bitmap image to base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
    
    /**
     * Send an image to the server for LaTeX conversion
     */
    public String convertImageToLatex(Bitmap image) throws IOException, JSONException {
        String base64Image = bitmapToBase64(image);
        
        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("image", base64Image);
        
        // Create request
        Request request = new Request.Builder()
                .url(serverConfig.getApiUrl())
                .post(RequestBody.create(requestBody.toString(), JSON))
                .build();
        
        Log.d(TAG, "Sending request to: " + serverConfig.getApiUrl());
        
        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            if (jsonResponse.has("latex")) {
                return jsonResponse.getString("latex");
            } else if (jsonResponse.has("error")) {
                throw new IOException("Server error: " + jsonResponse.getString("error"));
            } else {
                throw new IOException("Invalid server response");
            }
        }
    }
    
    /**
     * Send LaTeX content to the server for HTML rendering
     * @param latexContent The LaTeX content to render
     * @return A map containing HTML and CSS content
     * @throws IOException If any error occurs during the request
     * @throws JSONException If any error occurs while creating JSON
     */
    public JSONObject renderLatexToHtml(String latexContent) throws IOException, JSONException {
        // Log the complete content being processed
        Log.d(TAG, "Processing full LaTeX document: " + 
              (latexContent.length() > 100 ? latexContent.substring(0, 100) + "..." : latexContent));
        
        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        // Ensure we're sending the complete document without truncation
        requestBody.put("latex", latexContent);
        
        String jsonStr = requestBody.toString();
        Log.d(TAG, "Sending JSON length: " + jsonStr.length());
        
        // Create request with proper headers
        Request request = new Request.Builder()
                .url(serverConfig.getRenderApiUrl())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonStr, JSON))
                .build();
        
        Log.d(TAG, "Sending render request to: " + serverConfig.getRenderApiUrl());
        
        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                Log.e(TAG, "Error response: " + response.code() + " - " + errorBody);
                throw new IOException("Unexpected response code: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);
            
            // Check for error response
            if (jsonResponse.has("error")) {
                throw new IOException("Server error: " + jsonResponse.getString("error"));
            }
            
            // Return the JSON object containing html and css
            return jsonResponse;
        }
    }
    
    /**
     * Fetch an image from the server
     * @param imageName Name of the image file to fetch
     * @return Byte array containing the image data
     * @throws IOException If any error occurs during the request
     */
    public byte[] fetchImage(String imageName) throws IOException {
        // Create request
        String imageUrl = serverConfig.getServerUrl() + "/" + imageName;
        Request request = new Request.Builder()
                .url(imageUrl)
                .get()
                .build();
        
        Log.d(TAG, "Fetching image from: " + imageUrl);
        
        // Execute request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            
            return response.body().bytes();
        }
    }
}