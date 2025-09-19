/*
 * Copyright (C) 2011 Google Inc.
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
import android.view.inputmethod.InputMethodSubtype;

/** Triggers a voice recognition by using {@link ImeTrigger}, {@link IntentApiTrigger}, or {@link OpenAITrigger}. */
public class VoiceRecognitionTrigger {

  private final InputMethodService mInputMethodService;

  private Trigger mTrigger;

  private ImeTrigger mImeTrigger;
  private IntentApiTrigger mIntentApiTrigger;
  private OpenAITrigger mOpenAITrigger;

  public VoiceRecognitionTrigger(InputMethodService inputMethodService) {
    mInputMethodService = inputMethodService;
    mTrigger = getTrigger();
  }

  private Trigger getTrigger() {
    // Check if OpenAI speech-to-text is enabled and configured
    if (OpenAITrigger.isAvailable(mInputMethodService)) {
      return getOpenAITrigger();
    } else if (ImeTrigger.isInstalled(mInputMethodService)) {
      // Prioritize IME as it's usually a better experience
      return getImeTrigger();
    } else if (IntentApiTrigger.isInstalled(mInputMethodService)) {
      return getIntentTrigger();
    } else {
      return null;
    }
  }

  private Trigger getIntentTrigger() {
    if (mIntentApiTrigger == null) {
      mIntentApiTrigger = new IntentApiTrigger(mInputMethodService);
    }
    return mIntentApiTrigger;
  }

  private Trigger getImeTrigger() {
    if (mImeTrigger == null) {
      mImeTrigger = new ImeTrigger(mInputMethodService);
    }
    return mImeTrigger;
  }

  private Trigger getOpenAITrigger() {
    if (mOpenAITrigger == null) {
      mOpenAITrigger = new OpenAITrigger(mInputMethodService);
    }
    return mOpenAITrigger;
  }

  public boolean isInstalled() {
    return mTrigger != null;
  }

  public boolean isEnabled() {
    return true;
  }

  // For testing
  public String getKind() {
    if (mOpenAITrigger != null) {
      return "openai";
    } else if (mImeTrigger != null && mIntentApiTrigger != null) {
      return "both";
    } else if (mImeTrigger != null) {
      return "ime";
    } else if (mIntentApiTrigger != null) {
      return "intent";
    } else {
      return "none";
    }
  }

  /**
   * Starts a voice recognition
   *
   * @param language The language in which the recognition should be done. If the recognition is
   *     done through the Google voice typing, the parameter is ignored and the recognition is done
   *     using the locale of the calling IME.
   * @see InputMethodSubtype
   */
  public void startVoiceRecognition(String language) {
    if (mTrigger != null) {
      mTrigger.startVoiceRecognition(language);
    }
  }

  public void onStartInputView() {
    if (mTrigger != null) {
      mTrigger.onStartInputView();
    }

    // The trigger is refreshed as the system may have changed in the meanwhile.
    mTrigger = getTrigger();
  }
  
  /**
   * Checks if currently recording voice input.
   * This allows the UI to update the microphone button state.
   */
  public boolean isRecording() {
    if (mTrigger instanceof OpenAITrigger) {
      return ((OpenAITrigger) mTrigger).isRecording();
    }
    return false;
  }
}
