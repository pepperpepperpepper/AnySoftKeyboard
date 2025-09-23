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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that keeps the device awake while recording audio.
 * This service ensures that the CPU stays active during voice recording
 * and displays a notification to indicate recording is in progress.
 */
public class VoiceRecordingForegroundService extends Service {

    private static final String TAG = "VoiceRecordingService";
    private static final String CHANNEL_ID = "voice_recording_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private PowerManager.WakeLock mWakeLock;
    private static boolean sIsRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Voice recording service started");
        
        // Create and show notification
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        
        sIsRunning = true;
        
        // If the service is killed, restart it with the original intent
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Voice recording service destroyed");
        releaseWakeLock();
        sIsRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't provide binding
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Recording",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows when voice recording is active");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        // Create an intent that would open the app if the notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                getPackageManager().getLaunchIntentForPackage(getPackageName()),
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Recording")
                .setContentText("Listening...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                mWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "AnySoftKeyboard:VoiceRecordingWakeLock"
                );
                mWakeLock.setReferenceCounted(false);
                mWakeLock.acquire();
                Log.d(TAG, "Wake lock acquired");
            }
        }
    }

    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
            Log.d(TAG, "Wake lock released");
        }
    }

    /**
     * Starts the voice recording foreground service.
     * @param context The context to start the service from
     */
    public static void startService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, VoiceRecordingForegroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Voice recording service start requested");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start voice recording service", e);
        }
    }

    /**
     * Stops the voice recording foreground service.
     * @param context The context to stop the service from
     */
    public static void stopService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, VoiceRecordingForegroundService.class);
            context.stopService(serviceIntent);
            Log.d(TAG, "Voice recording service stop requested");
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop voice recording service", e);
        }
    }

    /**
     * Checks if the voice recording service is currently running.
     * @return true if the service is running, false otherwise
     */
    public static boolean isRunning() {
        return sIsRunning;
    }
}