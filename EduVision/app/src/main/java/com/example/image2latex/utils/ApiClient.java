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
    private static final int TIMEOUT_SECONDS = 30;
    
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
     * @return The HTML content as string
     * @throws IOException If any error occurs during the request
     * @throws JSONException If any error occurs while creating JSON
     */
    public String renderLatexToHtml(String latexContent) throws IOException, JSONException {
        // Create JSON request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("latex", latexContent);
        
        String jsonStr = requestBody.toString();
        Log.d(TAG, "Sending JSON: " + jsonStr);
        
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
            
            return response.body().string();
        }
    }
}