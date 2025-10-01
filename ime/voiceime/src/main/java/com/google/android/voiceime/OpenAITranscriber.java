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
import java.util.HashMap;
import java.util.Map;

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
     * @param mediaType MIME type of the audio file (e.g., "audio/mp4")
     * @param apiKey OpenAI API key for authentication
     * @param endpoint OpenAI API endpoint URL
     * @param model OpenAI transcription model to use (e.g., "whisper-1", "gpt-4o-transcribe")
     * @param language Language code for transcription (e.g., "en", "es")
     * @param temperature Controls randomness in transcription (0.0 = more accurate, 1.0 = more creative)
     * @param responseFormat Output format for the transcription (json, text, srt, vtt)
     * @param chunkingStrategy How to handle long audio files (auto, none)
     * @param prompt Optional prompt to guide transcription style and spelling (max 224 tokens)
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
            @NonNull String temperature,
            @NonNull String responseFormat,
            @NonNull String chunkingStrategy,
            @NonNull String prompt,
            boolean addTrailingSpace,
            boolean useDefaultPrompt,
            @NonNull String defaultPromptType,
            boolean appendCustomPrompt,
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
                String result = performTranscription(filename, mediaType, apiKey, endpoint, model, language, temperature, responseFormat, chunkingStrategy, prompt, useDefaultPrompt, defaultPromptType, appendCustomPrompt);
                
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
            String language,
            String temperature,
            String responseFormat,
            String chunkingStrategy,
            String prompt,
            boolean useDefaultPrompt,
            String defaultPromptType,
            boolean appendCustomPrompt) throws IOException {
        
        File audioFile = new File(filename);
        if (!audioFile.exists()) {
            throw new IOException("Audio file does not exist: " + filename);
        }
        
        if (audioFile.length() == 0) {
            throw new IOException("Audio file is empty: " + filename);
        }
        
        Log.d(TAG, "Transcribing file: " + filename + " (" + audioFile.length() + " bytes)");
        Log.d(TAG, "Response format parameter: " + responseFormat);
        
        // Create multipart request body
        RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse(mediaType));
        
        // Capture form field values for debug output
        Map<String, String> formFieldValues = new HashMap<>();
        formFieldValues.put("model", model);
        formFieldValues.put("response_format", "text"); // Always use text for actual API call
        
        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(), fileBody)
                .addFormDataPart("model", model)
                .addFormDataPart("response_format", "text"); // Always use text for actual API call
        
        // Add language parameter if not empty
        if (!language.isEmpty()) {
            requestBodyBuilder.addFormDataPart("language", language);
            formFieldValues.put("language", language);
        }
        
        // Add temperature parameter if valid
        try {
            float tempValue = Float.parseFloat(temperature);
            if (tempValue >= 0.0f && tempValue <= 1.0f) {
                requestBodyBuilder.addFormDataPart("temperature", temperature);
                formFieldValues.put("temperature", temperature);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid temperature value: " + temperature + ", using default");
        }
        
        // Add chunking strategy if not "none" and model supports it
        if (!"none".equals(chunkingStrategy) && isChunkingStrategySupported(model)) {
            String chunkingStrategyValue = formatChunkingStrategy(chunkingStrategy);
            requestBodyBuilder.addFormDataPart("chunking_strategy", chunkingStrategyValue);
            formFieldValues.put("chunking_strategy", chunkingStrategyValue);
        }
        
        // Handle default prompts and custom prompt combination
        String finalPrompt = prompt;
        if (useDefaultPrompt) {
            OpenAIDefaultPrompts.PromptType promptTypeEnum = OpenAIDefaultPrompts.PromptType.fromValue(defaultPromptType);
            String defaultPrompt = OpenAIDefaultPrompts.getDefaultPrompt(model, promptTypeEnum);
            finalPrompt = OpenAIDefaultPrompts.combinePrompts(defaultPrompt, prompt, appendCustomPrompt);
            Log.d(TAG, "Using default prompt type: " + defaultPromptType + ", append custom: " + appendCustomPrompt);
        }
        
        // Add prompt parameter if not empty
        if (!finalPrompt.isEmpty()) {
            Log.d(TAG, "Adding final prompt to request: " + finalPrompt);
            requestBodyBuilder.addFormDataPart("prompt", finalPrompt);
            formFieldValues.put("prompt", finalPrompt);
        } else {
            Log.d(TAG, "Final prompt is empty, not adding to request");
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
            
            String responseBody = response.body() != null ? response.body().string() : "No response body";
            
            if (!response.isSuccessful()) {
                if ("debug".equals(responseFormat)) {
                    return createDebugOutput(request, response, responseBody, null, formFieldValues);
                } else {
                    throw new IOException("HTTP " + response.code() + ": " + responseBody);
                }
            }
            
            if (response.body() == null) {
                if ("debug".equals(responseFormat)) {
                    return createDebugOutput(request, response, "Empty response body", null, formFieldValues);
                } else {
                    throw new IOException("Empty response body");
                }
            }
            
            String result = responseBody.trim();
            // Replace newlines with spaces to maintain sentence separation
            result = result.replaceAll("\\n+", " ");
            // Replace multiple spaces with single space
            result = result.replaceAll(" +", " ");
            Log.d(TAG, "Transcription result: " + result);
            Log.d(TAG, "Checking if debug format - responseFormat: '" + responseFormat + "', equals debug: " + "debug".equals(responseFormat));
            
            // If debug format, return debug information
            if ("debug".equals(responseFormat)) {
                Log.d(TAG, "Creating debug output");
                return createDebugOutput(request, response, result, null, formFieldValues);
            }
            
            return result;
        }
    }
    
    private String createDebugOutput(Request request, Response response, String responseBody, Exception error, Map<String, String> formFieldValues) {
        StringBuilder debugOutput = new StringBuilder();
        debugOutput.append("=== OPENAI API DEBUG INFORMATION ===\n\n");
        
        // Request information
        debugOutput.append("=== REQUEST ===\n");
        debugOutput.append("URL: ").append(request.url()).append("\n");
        debugOutput.append("Method: ").append(request.method()).append("\n");
        
        // Headers
        debugOutput.append("Headers:\n");
        for (String name : request.headers().names()) {
            // Mask Authorization header for security
            String value = request.headers().get(name);
            if ("Authorization".equalsIgnoreCase(name) && value != null && value.startsWith("Bearer ")) {
                value = "Bearer ***MASKED***";
            }
            debugOutput.append("  ").append(name).append(": ").append(value).append("\n");
        }
        
// Request body (form data)
        debugOutput.append("Form Data:\n");
        try {
            if (request.body() instanceof MultipartBody) {
                MultipartBody multipartBody = (MultipartBody) request.body();
                for (MultipartBody.Part part : multipartBody.parts()) {
                    okhttp3.Headers partHeaders = part.headers();
                    String contentDisposition = partHeaders != null ? partHeaders.get("Content-Disposition") : null;
                    
                    if (contentDisposition != null) {
                        debugOutput.append("  ").append(contentDisposition);
                        if (contentDisposition.contains("name=\"file\"")) {
                            debugOutput.append(" (").append(part.body().contentLength()).append(" bytes)");
                        } else if (!contentDisposition.contains("filename=")) {
                            // For form fields, extract the field name and use captured value
                            try {
                                // Extract field name from Content-Disposition
                                String fieldName = extractFieldNameFromContentDisposition(contentDisposition);
                                if (fieldName != null && formFieldValues.containsKey(fieldName)) {
                                    debugOutput.append(": ").append(formFieldValues.get(fieldName));
                                } else {
                                    debugOutput.append(": [value not captured]");
                                }
                            } catch (Exception e) {
                                debugOutput.append(": [Error getting value: ").append(e.getMessage()).append("]");
                            }
                        }
                        debugOutput.append("\n");
                    }
                }
            }
        } catch (Exception e) {
            debugOutput.append("  [Error reading form data: ").append(e.getMessage()).append("]\n");
        }
        
        debugOutput.append("\n");
        
        // Response information
        debugOutput.append("=== RESPONSE ===\n");
        debugOutput.append("Status Code: ").append(response.code()).append("\n");
        debugOutput.append("Message: ").append(response.message()).append("\n");
        
        // Response headers
        debugOutput.append("Headers:\n");
        for (String name : response.headers().names()) {
            debugOutput.append("  ").append(name).append(": ").append(response.headers().get(name)).append("\n");
        }
        
        debugOutput.append("\n");
        
        // Response body
        debugOutput.append("=== RESPONSE BODY ===\n");
        debugOutput.append(responseBody).append("\n");
        
        // Error information if present
        if (error != null) {
            debugOutput.append("\n=== ERROR ===\n");
            debugOutput.append("Exception: ").append(error.getClass().getSimpleName()).append("\n");
            debugOutput.append("Message: ").append(error.getMessage()).append("\n");
        }
        
        debugOutput.append("\n=== END DEBUG INFORMATION ===");
        
        return debugOutput.toString();
    }
    
    private String extractFieldNameFromContentDisposition(String contentDisposition) {
        try {
            // Extract name="fieldname" from Content-Disposition header
            int nameIndex = contentDisposition.indexOf("name=\"");
            if (nameIndex != -1) {
                int startIndex = nameIndex + 6; // 6 is length of "name=\""
                int endIndex = contentDisposition.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return contentDisposition.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error extracting field name from Content-Disposition: " + contentDisposition, e);
        }
        return null;
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
    
    /**
     * Checks if the specified model supports chunking_strategy parameter
     */
    private boolean isChunkingStrategySupported(String model) {
        // whisper-1 does not support chunking_strategy
        // gpt-4o-transcribe and gpt-4o-mini-transcribe support it
        return "gpt-4o-transcribe".equals(model) || "gpt-4o-mini-transcribe".equals(model);
    }
    
    /**
     * Formats the chunking strategy value according to OpenAI API requirements
     */
    private String formatChunkingStrategy(String chunkingStrategy) {
        // For supported models, chunking_strategy should be a JSON object
        if ("auto".equals(chunkingStrategy) || "server_vad".equals(chunkingStrategy)) {
            return "{\"type\": \"server_vad\"}";
        }
        // Return as-is for any other values (though "none" should be filtered out before this)
        return chunkingStrategy;
    }
}