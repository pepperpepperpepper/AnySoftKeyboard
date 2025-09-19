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
    private boolean mUseOggFormat;
    private boolean mIsRecording = false;
    
    public OpenAITrigger(InputMethodService inputMethodService) {
        mInputMethodService = inputMethodService;
        mOpenAITranscriber = new OpenAITranscriber();
        mAudioRecorderManager = new AudioRecorderManager(inputMethodService);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(inputMethodService);
        
        setupAudioRecorderCallbacks();
    }
    
    private void setupAudioRecorderCallbacks() {
        mAudioRecorderManager.setOnRecordingStopped((success, errorMessage) -> {
            mIsRecording = false; // Always reset recording state when stopped
            if (success) {
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
            String apiKey = prefs.getString(apiKeyKey, "");
            
            return enabled && !apiKey.isEmpty();
        } catch (Exception e) {
            // Handle any exceptions in test environment
            return false;
        }
    }
    
    @Override
    public void startVoiceRecognition(String language) {
        Log.d(TAG, "OpenAI voice recognition triggered for language: " + language);
        
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
    
    private void setupAudioFormat() {
        String audioFormat = mSharedPreferences.getString(
            mInputMethodService.getString(R.string.settings_key_openai_audio_format), "m4a");
        
        mUseOggFormat = "ogg".equals(audioFormat);
        
        File cacheDir = mInputMethodService.getExternalCacheDir();
        if (cacheDir != null) {
            String filename = mUseOggFormat ? "recorded.ogg" : "recorded.m4a";
            mRecordedAudioFilename = new File(cacheDir, filename).getAbsolutePath();
            mAudioMediaType = mUseOggFormat ? "audio/ogg" : "audio/mp4";
        } else {
            // Use defaults
            mUseOggFormat = false;
            mAudioMediaType = "audio/mp4";
        }
    }
    
    private void startRecording() {
        // Check permissions
        if (!mAudioRecorderManager.hasPermissions()) {
            showError("Audio recording permission not granted");
            return;
        }
        
        try {
            mAudioRecorderManager.startRecording(mRecordedAudioFilename, mUseOggFormat);
            mIsRecording = true;
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
    }
    
    private void startTranscription() {
        Log.d(TAG, "Starting transcription for file: " + mRecordedAudioFilename);
        
        // Instead of calling OpenAI API, we'll show the file length
        File audioFile = new File(mRecordedAudioFilename);
        if (audioFile.exists()) {
            long fileSize = audioFile.length();
            String fileSizeText = formatFileSize(fileSize);
            
            // Calculate expected duration (rough estimate)
            long expectedDurationSeconds = estimateAudioDuration(fileSize);
            String expectedDurationText = formatDuration(expectedDurationSeconds);
            
            // Show alert with file information
            String message = String.format("OpenAI recording made with %d number of bytes.", fileSize);
            
            showAlert(message);
            
            // Clean up the audio file
            cleanupAudioFile();
        } else {
            Log.e(TAG, "Audio file does not exist: " + mRecordedAudioFilename);
            showError("Audio file not found: " + mRecordedAudioFilename);
        }
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
        // Rough estimate based on audio format
        if (mUseOggFormat) {
            // OGG Vorbis: roughly 32-64 kbps for voice
            return fileSize / (16 * 1024); // Assuming ~16KB per second (128 kbps)
        } else {
            // M4A/AAC: roughly 32-64 kbps for voice
            return fileSize / (16 * 1024); // Assuming ~16KB per second (128 kbps)
        }
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
        new android.app.AlertDialog.Builder(mInputMethodService)
            .setTitle("Audio Recording Test")
            .setMessage(message)
            .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
            .setCancelable(true)
            .show();
    }
    
    private void onTranscriptionResult(String result) {
        Log.d(TAG, "Transcription result: " + result);
        mLastRecognitionResult = result;
        
        // Commit the result to the input connection
        commitResult();
        
        // Clean up audio file
        cleanupAudioFile();
    }
    
    private void onTranscriptionError(String error) {
        Log.e(TAG, "Transcription error: " + error);
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
        android.widget.Toast.makeText(mInputMethodService, message, android.widget.Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
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
        
        // Stop any ongoing recording
        mAudioRecorderManager.stopRecording();
        
        // Clean up any leftover audio files
        cleanupAudioFile();
    }
    
    }