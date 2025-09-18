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
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

/** Triggers voice recognition by switching to Whisper to Input IME. */
public class OpenAITrigger implements Trigger {

    private static final String TAG = "OpenAITrigger";
    private static final String WHISPER_TO_INPUT_PACKAGE = "com.example.whispertoinput";
    
    private final InputMethodService mInputMethodService;
    private final SharedPreferences mSharedPreferences;
    
    public OpenAITrigger(InputMethodService inputMethodService) {
        mInputMethodService = inputMethodService;
        mSharedPreferences = getSharedPreferences();
    }
    
    protected SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(mInputMethodService);
    }
    
    public static boolean isAvailable(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = prefs.getBoolean(context.getString(R.string.settings_key_openai_enabled), false);
        
        // Check if Whisper to Input app is installed
        boolean whisperInstalled = isWhisperToInputInstalled(context);
        
        return enabled && whisperInstalled;
    }
    
    public boolean isAvailable() {
        boolean enabled = mSharedPreferences.getBoolean(mInputMethodService.getString(R.string.settings_key_openai_enabled), false);
        
        // Check if Whisper to Input app is installed
        boolean whisperInstalled = isWhisperToInputInstalled(mInputMethodService);
        
        return enabled && whisperInstalled;
    }
    
    private static boolean isWhisperToInputInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(WHISPER_TO_INPUT_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
    
    @Override
    public void startVoiceRecognition(String language) {
        Log.d(TAG, "Starting OpenAI voice recognition by switching to Whisper to Input IME");
        
        // Check if Whisper to Input is available
        if (!isWhisperToInputInstalled(mInputMethodService)) {
            showError(mInputMethodService.getString(R.string.openai_error_whisper_not_installed));
            return;
        }
        
        // Switch to Whisper to Input IME
        switchToWhisperToInput();
    }
    
    private void switchToWhisperToInput() {
        try {
            InputMethodManager imm = (InputMethodManager) mInputMethodService.getSystemService(Context.INPUT_METHOD_SERVICE);
            
            // Show input method picker so user can select Whisper to Input
            imm.showInputMethodPicker();
            
            Log.d(TAG, "Showing input method picker for user to select Whisper to Input IME");
        } catch (Exception e) {
            Log.e(TAG, "Error switching to Whisper to Input IME", e);
            showError("Failed to switch to Whisper to Input: " + e.getMessage());
        }
    }
    
    
    
    private void showError(String message) {
        // Show error as toast
        createToast(mInputMethodService, message, android.widget.Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error: " + message);
    }
    
    protected android.widget.Toast createToast(Context context, String message, int duration) {
        return android.widget.Toast.makeText(context, message, duration);
    }
    
    @Override
    public void onStartInputView() {
        Log.d(TAG, "onStartInputView called");
        // No state to reset since we're just switching to another IME
    }
    
    }