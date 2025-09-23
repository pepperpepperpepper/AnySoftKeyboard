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
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;

/** Triggers a voice recognition using OpenAI's Whisper API. */
public class OpenAITrigger implements Trigger {

    private static final String TAG = "OpenAITrigger";
    
    private final InputMethodService mInputMethodService;
    private final OpenAITranscriber mOpenAITranscriber;
    private final AudioRecorderManager mAudioRecorderManager;
    private final SharedPreferences mSharedPreferences;
    
    private String mLastRecognitionResult;
    private String mRecordedAudioFilename;
    private String mAudioMediaType;
    
    private boolean mIsRecording = false;
    
    /** Callback interface for recording state changes */
    public interface RecordingStateCallback {
        void onRecordingStateChanged(boolean isRecording);
    }
    
    /** Callback interface for transcription state changes */
    public interface TranscriptionStateCallback {
        void onTranscriptionStateChanged(boolean isTranscribing);
    }
    
    /** Callback interface for transcription errors */
    public interface TranscriptionErrorCallback {
        void onTranscriptionError(String error);
    }
    
    /** Callback interface for when recording ends and audio is sent to OpenAI */
    public interface RecordingEndedCallback {
        void onRecordingEnded();
    }
    
    /** Callback interface for when transcribed text has been written to input field */
    public interface TextWrittenCallback {
        void onTextWritten(String text);
    }
    
    private RecordingStateCallback mRecordingStateCallback;
    private TranscriptionStateCallback mTranscriptionStateCallback;
    private TranscriptionErrorCallback mTranscriptionErrorCallback;
    private RecordingEndedCallback mRecordingEndedCallback;
    private TextWrittenCallback mTextWrittenCallback;
    
    public OpenAITrigger(InputMethodService inputMethodService) {
        mInputMethodService = inputMethodService;
        mOpenAITranscriber = new OpenAITranscriber();
        mAudioRecorderManager = new AudioRecorderManager(inputMethodService);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputMethodService);
        
        setupAudioRecorderCallbacks();
    }
    
    /**
     * Sets the callback for recording state changes.
     * @param callback The callback to be notified when recording state changes
     */
    public void setRecordingStateCallback(RecordingStateCallback callback) {
        mRecordingStateCallback = callback;
    }
    
    /**
     * Sets the callback for transcription state changes.
     * @param callback The callback to be notified when transcription state changes
     */
    public void setTranscriptionStateCallback(TranscriptionStateCallback callback) {
        mTranscriptionStateCallback = callback;
    }
    
    /**
     * Sets the callback for transcription errors.
     * @param callback The callback to be notified when transcription errors occur
     */
    public void setTranscriptionErrorCallback(TranscriptionErrorCallback callback) {
        mTranscriptionErrorCallback = callback;
    }
    
    /**
     * Sets the callback for when recording ends and audio is sent to OpenAI.
     * @param callback The callback to be notified when recording ends
     */
    public void setRecordingEndedCallback(RecordingEndedCallback callback) {
        mRecordingEndedCallback = callback;
    }
    
    /**
     * Sets the callback for when transcribed text has been written to input field.
     * @param callback The callback to be notified when text is written
     */
    public void setTextWrittenCallback(TextWrittenCallback callback) {
        mTextWrittenCallback = callback;
    }
    
    /**
     * Notifies the callback about recording state changes.
     * @param isRecording The new recording state
     */
    private void notifyRecordingStateChanged(boolean isRecording) {
        if (mRecordingStateCallback != null) {
            mRecordingStateCallback.onRecordingStateChanged(isRecording);
        }
    }
    
    /**
     * Notifies the callback about transcription state changes.
     * @param isTranscribing The new transcription state
     */
    private void notifyTranscriptionStateChanged(boolean isTranscribing) {
        if (mTranscriptionStateCallback != null) {
            mTranscriptionStateCallback.onTranscriptionStateChanged(isTranscribing);
        }
    }
    
    /**
     * Notifies the callback about transcription errors.
     * @param error The error message
     */
    private void notifyTranscriptionError(String error) {
        if (mTranscriptionErrorCallback != null) {
            mTranscriptionErrorCallback.onTranscriptionError(error);
        }
    }
    
    /**
     * Notifies the callback that recording has ended and audio is sent to OpenAI.
     */
    private void notifyRecordingEnded() {
        if (mRecordingEndedCallback != null) {
            mRecordingEndedCallback.onRecordingEnded();
        }
    }
    
    /**
     * Notifies the callback that transcribed text has been written to input field.
     * @param text The text that was written
     */
    private void notifyTextWritten(String text) {
        if (mTextWrittenCallback != null) {
            mTextWrittenCallback.onTextWritten(text);
        }
    }
    
    private void setupAudioRecorderCallbacks() {
        mAudioRecorderManager.setOnRecordingStopped((success, errorMessage) -> {
            mIsRecording = false; // Always reset recording state when stopped
            notifyRecordingStateChanged(false); // Notify about state change
            if (success) {
                // Notify that recording has ended and we're sending to OpenAI
                notifyRecordingEnded();
                startTranscription();
            } else {
                Log.e(TAG, "Recording failed: " + errorMessage);
                showError(errorMessage);
            }
        });
        
        mAudioRecorderManager.setOnUpdateMicrophoneAmplitude(amplitude -> {
            // Update UI if needed - could be extended to show recording indicator
            Log.d(TAG, "Microphone amplitude: " + amplitude);
        });
    }
    
    public static boolean isAvailable(Context context) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs == null) {
                return false; // Handle test environment where prefs might be null
            }
            String enabledKey = context.getString(R.string.settings_key_openai_enabled);
            String apiKeyKey = context.getString(R.string.settings_key_openai_api_key);
            
            boolean enabled = prefs.getBoolean(enabledKey, false);
            
            // If OpenAI is not enabled, just return false silently
            if (!enabled) {
                return false;
            }
            
            // OpenAI is enabled, now check if it's configured
            String apiKey = prefs.getString(apiKeyKey, "");
            
            if (apiKey.isEmpty()) {
                // OpenAI is enabled but no API key - show error
                android.util.Log.d("LongPressDebug", "OpenAI enabled but API key empty - showing configuration error toast");
                showConfigurationError(context);
                return false;
            }
            
            return true; // OpenAI is enabled and configured
        } catch (Exception e) {
            // Handle any exceptions in test environment
            return false;
        }
    }
    
    private static void showConfigurationError(Context context) {
        // Show error as toast - ensure it's shown on UI thread
        android.util.Log.d("LongPressDebug", "showConfigurationError called - posting toast to main thread");
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            android.util.Log.d("LongPressDebug", "showConfigurationError: Showing API key unset toast");
            android.widget.Toast.makeText(
                context, 
                context.getString(R.string.openai_error_api_key_unset), 
                android.widget.Toast.LENGTH_LONG
            ).show();
        });
    }
    
    @Override
    public void startVoiceRecognition(String language) {
        Log.d(TAG, "OpenAI voice recognition triggered for language: " + language);
        android.util.Log.d("LongPressDebug", "OpenAI startVoiceRecognition called - this might interfere with long press");
        
        // Check if OpenAI speech-to-text is enabled and configured
        if (!isConfigured()) {
            showError(mInputMethodService.getString(R.string.openai_error_api_key_unset));
            return;
        }
        
        // Toggle recording state
        if (mIsRecording) {
            // Stop recording if currently recording
            Log.d(TAG, "Stopping recording");
            stopRecording();
        } else {
            // Setup audio format based on preferences
            setupAudioFormat();
            
            // Start recording
            Log.d(TAG, "Starting recording");
            startRecording();
        }
    }
    
    private boolean isConfigured() {
        String apiKey = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_api_key), "");
        return apiKey != null && !apiKey.isEmpty();
    }
    
    private boolean isValidResponseFormat(String format) {
        return "json".equals(format) || "text".equals(format) || 
               "srt".equals(format) || "vtt".equals(format) || 
               "verbose_json".equals(format);
    }
    
    private boolean isValidChunkingStrategy(String strategy) {
        return "auto".equals(strategy) || "none".equals(strategy);
    }
    
    private void setupAudioFormat() {
        // Audio format is now fixed to M4A since OGG option was removed
        
        File cacheDir = mInputMethodService.getExternalCacheDir();
        if (cacheDir != null) {
            mRecordedAudioFilename = new File(cacheDir, "recorded.m4a").getAbsolutePath();
            mAudioMediaType = "audio/mp4";
        } else {
            // Use defaults
            mAudioMediaType = "audio/mp4";
        }
    }
    
    private void startRecording() {
        // Check permissions
        if (!mAudioRecorderManager.hasPermissions()) {
            showError(mInputMethodService.getString(R.string.openai_error_microphone_permission));
            return;
        }
        
        try {
            mAudioRecorderManager.startRecording(mRecordedAudioFilename, false); // Always use M4A format
            mIsRecording = true;
            notifyRecordingStateChanged(true); // Notify about state change
            Log.d(TAG, "Started recording to: " + mRecordedAudioFilename);
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            showError(mInputMethodService.getString(R.string.openai_error_recording_failed));
        }
    }
    
    private void stopRecording() {
        if (mAudioRecorderManager.isRecording()) {
            mAudioRecorderManager.stopRecording();
        }
        mIsRecording = false;
        notifyRecordingStateChanged(false); // Notify about state change
    }
    
    private void startTranscription() {
        Log.d(TAG, "Starting transcription for file: " + mRecordedAudioFilename);
        
        File audioFile = new File(mRecordedAudioFilename);
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: " + mRecordedAudioFilename);
            showError("Audio file not found: " + mRecordedAudioFilename);
            return;
        }
        
        if (audioFile.length() == 0) {
            Log.e(TAG, "Audio file is empty: " + mRecordedAudioFilename);
            showError("Audio file is empty");
            return;
        }
        
        // Get OpenAI configuration from preferences
        String apiKey = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_api_key), "");
        String endpoint = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_endpoint), 
            "https://api.openai.com/v1/audio/transcriptions");
        String model = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_model), "whisper-1");
        String language = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_language), "en");
        String temperature = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_temperature), "0.0");
        String responseFormat = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_response_format), "text");
        String chunkingStrategy = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_chunking_strategy), "auto");
        String prompt = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_prompt), "");
        
        // Validate temperature value
        try {
            float tempValue = Float.parseFloat(temperature);
            if (tempValue < 0.0f || tempValue > 1.0f) {
                Log.w(TAG, "Temperature value out of range (0.0-1.0): " + temperature + ", using default 0.0");
                temperature = "0.0";
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid temperature value: " + temperature + ", using default 0.0");
            temperature = "0.0";
        }
        
        // Validate response format
        if (!isValidResponseFormat(responseFormat)) {
            Log.w(TAG, "Invalid response format: " + responseFormat + ", using default json");
            responseFormat = "json";
        }
        
        // Validate chunking strategy
        if (!isValidChunkingStrategy(chunkingStrategy)) {
            Log.w(TAG, "Invalid chunking strategy: " + chunkingStrategy + ", using default auto");
            chunkingStrategy = "auto";
        }
        boolean addTrailingSpace = mSharedPreferences.getBoolean(
            mInputMethodService.getString(R.string.settings_key_openai_add_trailing_space), true);
        
        // Update status to "Transcribing"
        updateTranscriptionStatus(true);
        notifyTranscriptionStateChanged(true);
        
        // Start transcription
        mOpenAITranscriber.startAsync(
            mInputMethodService,
            mRecordedAudioFilename,
            mAudioMediaType,
            apiKey,
            endpoint,
            model,
            language,
            temperature,
            responseFormat,
            chunkingStrategy,
            prompt,
            addTrailingSpace,
            new OpenAITranscriber.TranscriptionCallback() {
                @Override
                public void onResult(String result) {
                    Log.d(TAG, "Transcription successful: " + result);
                    updateTranscriptionStatus(false);
                    notifyTranscriptionStateChanged(false);
                    onTranscriptionResult(result);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Transcription failed: " + error);
                    updateTranscriptionStatus(false);
                    notifyTranscriptionStateChanged(false);
                    notifyTranscriptionError(error);
                    onTranscriptionError(error);
                }
            }
        );
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    private long estimateAudioDuration(long fileSize) {
        // M4A/AAC: roughly 32-64 kbps for voice
        return fileSize / (16 * 1024); // Assuming ~16KB per second (128 kbps)
    }
    
    private String formatDuration(long seconds) {
        if (seconds < 60) {
            return seconds + " seconds";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return String.format("%d:%02d", minutes, remainingSeconds);
        }
    }
    
    private void showAlert(String message) {
        // Use Toast instead of AlertDialog since InputMethodService doesn't have a valid window token
        // Ensure Toast is shown on UI thread using Handler
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            android.widget.Toast.makeText(mInputMethodService, message, android.widget.Toast.LENGTH_LONG).show();
        });
    }
    
    private void onTranscriptionResult(String result) {
        Log.d(TAG, "Transcription result: " + result);
        mLastRecognitionResult = result;
        
        // Commit the result to the input connection
        commitResult();
        
        // Notify that text has been written to the input field
        notifyTextWritten(result);
        
        // Clean up audio file
        cleanupAudioFile();
    }
    
    private void onTranscriptionError(String error) {
        Log.e(TAG, "Transcription error: " + error);
        notifyTranscriptionError(error);
        showError(error);
        cleanupAudioFile();
    }
    
    private void commitResult() {
        if (mLastRecognitionResult == null) {
            return;
        }
        
        try {
            android.view.inputmethod.InputConnection conn = mInputMethodService.getCurrentInputConnection();
            if (conn == null) {
                Log.w(TAG, "No input connection available");
                return;
            }
            
            if (!conn.beginBatchEdit()) {
                Log.w(TAG, "Could not begin batch edit");
                return;
            }
            
            try {
                // Commit the transcribed text
                conn.commitText(mLastRecognitionResult, 1);
                mLastRecognitionResult = null;
            } finally {
                conn.endBatchEdit();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error committing transcription result", e);
        }
    }
    
    private void cleanupAudioFile() {
        try {
            File file = new File(mRecordedAudioFilename);
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d(TAG, "Audio file deleted: " + mRecordedAudioFilename);
                } else {
                    Log.w(TAG, "Failed to delete audio file: " + mRecordedAudioFilename);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up audio file", e);
        }
    }
    
    private void showError(String message) {
        // Show error as toast
        Log.e(TAG, "Error: " + message);
        
        // Ensure Toast is shown on UI thread using Handler
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            android.widget.Toast.makeText(mInputMethodService, message, android.widget.Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Updates the transcription status for UI feedback.
     * @param isTranscribing true when transcription is in progress, false otherwise
     */
    private void updateTranscriptionStatus(boolean isTranscribing) {
        // Update the space bar or other UI elements to show "Transcribing..." status
        Log.d(TAG, "Transcription status: " + (isTranscribing ? "transcribing" : "idle"));
        
        // Notify the main keyboard service about transcription state changes
        // This will update the space bar to show "📝 TRANSCRIBING" or back to "English"
        notifyTranscriptionStateChanged(isTranscribing);
    }
    
    /**
     * Checks if currently recording.
     * This allows the UI to update the microphone button state.
     */
    public boolean isRecording() {
        return mIsRecording;
    }
    
    @Override
    public void onStartInputView() {
        Log.d(TAG, "onStartInputView called");
        // Reset any pending state
        mLastRecognitionResult = null;
        mIsRecording = false;
        notifyRecordingStateChanged(false); // Notify about state change
        
        // Stop any ongoing recording
        mAudioRecorderManager.stopRecording();
        
        // Clean up any leftover audio files
        cleanupAudioFile();
    }
    
    }