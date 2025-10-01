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
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

/** Manages audio recording for OpenAI speech-to-text functionality. */
public class AudioRecorderManager {

    private static final String TAG = "AudioRecorderManager";
    private static final int MAX_RECORDING_DURATION_MS = 30 * 1000; // 30 seconds max
    private static final int WAKE_LOCK_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes timeout
    
    public interface RecordingCallback {
        void onRecordingStopped(boolean success, String errorMessage);
    }
    
    public interface AmplitudeUpdateCallback {
        void onUpdateAmplitude(int amplitude);
    }
    
    private final Context mContext;
    private MediaRecorder mMediaRecorder;
    private RecordingCallback mRecordingCallback;
    private AmplitudeUpdateCallback mAmplitudeCallback;
    private boolean mIsRecording = false;
    private Thread mAmplitudeUpdateThread;
    private PowerManager.WakeLock mWakeLock;
    
    public AudioRecorderManager(@NonNull Context context) {
        mContext = context.getApplicationContext();
        initializeWakeLock();
    }
    
    /**
     * Initializes the wake lock for keeping the device awake during recording.
     */
    private void initializeWakeLock() {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AnySoftKeyboard::AudioRecordingWakeLock"
            );
            mWakeLock.setReferenceCounted(false);
        }
    }
    
    /**
     * Sets the callback for when recording stops.
     */
    public void setOnRecordingStopped(@NonNull RecordingCallback callback) {
        mRecordingCallback = callback;
    }
    
    /**
     * Sets the callback for amplitude updates during recording.
     */
    public void setOnUpdateMicrophoneAmplitude(@NonNull AmplitudeUpdateCallback callback) {
        mAmplitudeCallback = callback;
    }
    
    /**
     * Checks if required permissions are granted.
     */
    public boolean hasPermissions() {
        int recordAudioPermission = ContextCompat.checkSelfPermission(
            mContext, android.Manifest.permission.RECORD_AUDIO);
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Starts audio recording to the specified file.
     * 
     * @param outputFilePath Path where the audio file should be saved
     * @param useOggFormat Whether to use OGG format (false for M4A)
     * @throws IOException if recording cannot be started
     * @throws IllegalStateException if permissions are not granted
     */
    public void startRecording(@NonNull String outputFilePath, boolean useOggFormat) 
            throws IOException, IllegalStateException {
        
        if (!hasPermissions()) {
            throw new IllegalStateException("RECORD_AUDIO permission not granted");
        }
        
        if (mIsRecording) {
            Log.w(TAG, "Already recording, stopping current recording");
            stopRecording();
        }
        
        try {
            // Create output file if it doesn't exist
            File outputFile = new File(outputFilePath);
            File parentDir = outputFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Initialize MediaRecorder
            mMediaRecorder = new MediaRecorder();
            
            // Set audio source
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            
            // Set output format based on preference
            if (useOggFormat) {
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.OGG);
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); // Use standard AAC instead of AAC_ELD for OpenAI compatibility
                } else {
                    mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                }
            }
            
            // Set output file
            mMediaRecorder.setOutputFile(outputFilePath);
            
            // Configure recording parameters
            mMediaRecorder.setAudioChannels(1); // Mono
            mMediaRecorder.setAudioSamplingRate(16000); // 16kHz (good for speech)
            mMediaRecorder.setAudioEncodingBitRate(64000); // 64kbps
            
            // Prepare and start recording
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            
            // Acquire wake lock to keep device awake during recording
            acquireWakeLock();
            
            mIsRecording = true;
            Log.d(TAG, "Recording started to: " + outputFilePath);
            
            // Start amplitude updates
            startAmplitudeUpdates();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            cleanup();
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            cleanup();
            throw new IOException("Failed to start recording: " + e.getMessage(), e);
        }
    }
    
    /**
     * Stops the current recording.
     */
    public void stopRecording() {
        if (!mIsRecording || mMediaRecorder == null) {
            Log.w(TAG, "Not recording, nothing to stop");
            return;
        }
        
        Log.d(TAG, "Stopping recording");
        
        try {
            // Stop recording FIRST before stopping amplitude updates to prevent interference
            mMediaRecorder.stop();
            mIsRecording = false;
            
            // Release wake lock since recording is done
            releaseWakeLock();
            
            // Stop amplitude updates after recording is stopped
            stopAmplitudeUpdates();
            
            // Notify callback of success
            if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingStopped(true, null);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            mIsRecording = false;
            
            // Release wake lock in case of error
            releaseWakeLock();
            
            // Notify callback of failure
            if (mRecordingCallback != null) {
                mRecordingCallback.onRecordingStopped(false, "Failed to stop recording: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }
    
    /**
     * Checks if currently recording.
     */
    public boolean isRecording() {
        return mIsRecording;
    }
    
    private void startAmplitudeUpdates() {
        if (mAmplitudeCallback == null) {
            return;
        }
        
        mAmplitudeUpdateThread = new Thread(() -> {
            while (mIsRecording && mMediaRecorder != null) {
                try {
                    if (mMediaRecorder != null) {
                        int amplitude = mMediaRecorder.getMaxAmplitude();
                        mAmplitudeCallback.onUpdateAmplitude(amplitude);
                    }
                    Thread.sleep(100); // Update every 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.w(TAG, "Error getting amplitude", e);
                }
            }
        });
        
        mAmplitudeUpdateThread.start();
    }
    
    private void stopAmplitudeUpdates() {
        if (mAmplitudeUpdateThread != null) {
            mAmplitudeUpdateThread.interrupt();
            try {
                mAmplitudeUpdateThread.join(1000); // Wait up to 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mAmplitudeUpdateThread = null;
        }
    }
    
    private void cleanup() {
        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing MediaRecorder", e);
            }
            mMediaRecorder = null;
        }
        
        // Ensure wake lock is released during cleanup
        releaseWakeLock();
        
        stopAmplitudeUpdates();
        mIsRecording = false;
    }
    
    /**
     * Acquires the wake lock to keep the device awake during recording.
     */
    private void acquireWakeLock() {
        if (mWakeLock != null && !mWakeLock.isHeld()) {
            try {
                mWakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
                Log.d(TAG, "Wake lock acquired for " + (WAKE_LOCK_TIMEOUT_MS / 1000) + " seconds");
            } catch (Exception e) {
                Log.e(TAG, "Failed to acquire wake lock", e);
            }
        }
    }
    
    /**
     * Releases the wake lock when recording is complete.
     */
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            try {
                mWakeLock.release();
                Log.d(TAG, "Wake lock released");
            } catch (Exception e) {
                Log.e(TAG, "Failed to release wake lock", e);
            }
        }
    }
    
    /**
     * Automatically stops recording after maximum duration.
     * This should be called when starting recording to set up auto-stop.
     */
    public void setupAutoStop() {
        new Thread(() -> {
            try {
                Thread.sleep(WAKE_LOCK_TIMEOUT_MS); // Use 5-minute timeout instead of 30 seconds
                if (mIsRecording) {
                    Log.d(TAG, "Auto-stopping recording after " + (WAKE_LOCK_TIMEOUT_MS / 1000) + " seconds");
                    stopRecording();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}