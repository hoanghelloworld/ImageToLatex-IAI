package com.example.image2latex.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ServerConfig {
    private static final String TAG = "ServerConfig";
    private static final String CONFIG_FILE = "server.properties";
    private static final String DEFAULT_SERVER_URL = "http://127.0.0.1:8088";
    private static final String DEFAULT_ENDPOINT = "/image_to_latex";
    private static final String DEFAULT_RENDER_ENDPOINT = "/render_latex";
    
    private String serverUrl;
    private String endpoint;
    private String renderEndpoint;
    
    private static ServerConfig instance;
    
    private ServerConfig(Context context) {
        loadConfig(context);
    }
    
    public static synchronized ServerConfig getInstance(Context context) {
        if (instance == null) {
            instance = new ServerConfig(context);
        }
        return instance;
    }
    
    private void loadConfig(Context context) {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        
        try (InputStream inputStream = assetManager.open(CONFIG_FILE)) {
            properties.load(inputStream);
            serverUrl = properties.getProperty("server.url", DEFAULT_SERVER_URL);
            endpoint = properties.getProperty("server.endpoint", DEFAULT_ENDPOINT);
            renderEndpoint = properties.getProperty("server.render_endpoint", DEFAULT_RENDER_ENDPOINT);
            Log.d(TAG, "Loaded server configuration: " + serverUrl + endpoint);
        } catch (IOException e) {
            Log.w(TAG, "Could not load server.properties file, using defaults", e);
            serverUrl = DEFAULT_SERVER_URL;
            endpoint = DEFAULT_ENDPOINT;
            renderEndpoint = DEFAULT_RENDER_ENDPOINT;
        }
    }
    
    public String getApiUrl() {
        return serverUrl + endpoint;
    }
    
    public String getRenderApiUrl() {
        return serverUrl + renderEndpoint;
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getRenderEndpoint() {
        return renderEndpoint;
    }
} 