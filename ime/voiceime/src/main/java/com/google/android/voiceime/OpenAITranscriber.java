/*
 * Copyright (C) 2024 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Handles transcription requests to OpenAI's Whisper API. */
public class OpenAITranscriber {

    private static final String TAG = "OpenAITranscriber";
    private static final OkHttpClient httpClient = new OkHttpClient();
    
    public interface TranscriptionCallback {
        void onResult(String result);
        void onError(String error);
    }
    
    /**
     * Starts an asynchronous transcription request to OpenAI's Whisper API.
     * 
     * @param context Android context for accessing resources
     * @param filename Path to the audio file to transcribe
     * @param mediaType MIME type of the audio file (e.g., "audio/mp4", "audio/ogg")
     * @param apiKey OpenAI API key for authentication
     * @param endpoint OpenAI API endpoint URL
     * @param model Whisper model to use (e.g., "whisper-1")
     * @param language Language code for transcription (e.g., "en", "es")
     * @param addTrailingSpace Whether to add a trailing space to the result
     * @param callback Callback for handling the transcription result and errors
     */
    public void startAsync(
            @NonNull Context context,
            @NonNull String filename,
            @NonNull String mediaType,
            @NonNull String apiKey,
            @NonNull String endpoint,
            @NonNull String model,
            @NonNull String language,
            boolean addTrailingSpace,
            @NonNull TranscriptionCallback callback) {
        
        // Validate inputs
        if (apiKey.isEmpty()) {
            callback.onError(context.getString(R.string.openai_error_api_key_unset));
            return;
        }
        
        if (endpoint.isEmpty()) {
            callback.onError(context.getString(R.string.openai_error_endpoint_unset));
            return;
        }
        
        // Run transcription in a background thread
        new Thread(() -> {
            try {
                String result = performTranscription(filename, mediaType, apiKey, endpoint, model, language);
                
                // Post result to main thread
                postResultToMainThread(result, addTrailingSpace, callback);
                
            } catch (Exception e) {
                Log.e(TAG, "Transcription failed", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null) {
                    errorMessage = context.getString(R.string.openai_error_transcription_failed);
                }
                postErrorToMainThread(errorMessage, callback);
            }
        }).start();
    }
    
    private String performTranscription(
            String filename,
            String mediaType,
            String apiKey,
            String endpoint,
            String model,
            String language) throws IOException {
        
        File audioFile = new File(filename);
        if (!audioFile.exists()) {
            throw new IOException("Audio file does not exist: " + filename);
        }
        
        if (audioFile.length() == 0) {
            throw new IOException("Audio file is empty: " + filename);
        }
        
        Log.d(TAG, "Transcribing file: " + filename + " (" + audioFile.length() + " bytes)");
        
        // Create multipart request body
        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse(mediaType));
        
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(), fileBody)
                .addFormDataPart("model", model)
                .addFormDataPart("response_format", "text");
        
        // Add language parameter if not empty
        if (!language.isEmpty()) {
            requestBodyBuilder.addFormDataPart("language", language);
        }
        
        RequestBody requestBody = requestBodyBuilder.build();
        
        // Build request with headers
        Headers headers = new Headers.Builder()
                .add("Authorization", "Bearer " + apiKey)
                .add("Content-Type", "multipart/form-data")
                .build();
        
        Request request = new Request.Builder()
                .url(endpoint)
                .headers(headers)
                .post(requestBody)
                .build();
        
        Log.d(TAG, "Sending request to: " + endpoint);
        
        // Execute request
        try (Response response = httpClient.newCall(request).execute()) {
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }
            
            if (response.body() == null) {
                throw new IOException("Empty response body");
            }
            
            String result = response.body().string().trim();
            Log.d(TAG, "Transcription result: " + result);
            
            return result;
        }
    }
    
    private void postResultToMainThread(
            String result,
            boolean addTrailingSpace,
            TranscriptionCallback callback) {
        
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                String finalResult = result;
                if (addTrailingSpace) {
                    finalResult = result + " ";
                }
                callback.onResult(finalResult);
            } catch (Exception e) {
                Log.e(TAG, "Error in callback", e);
                callback.onError("Callback error: " + e.getMessage());
            }
        });
    }
    
    private void postErrorToMainThread(String error, TranscriptionCallback callback) {
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        mainHandler.post(() -> {
            try {
                callback.onError(error);
            } catch (Exception e) {
                Log.e(TAG, "Error in error callback", e);
            }
        });
    }
}