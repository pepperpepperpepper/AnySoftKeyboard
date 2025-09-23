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

import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.InputMethodSubtype;

/** Triggers a voice recognition by using {@link ImeTrigger}, {@link IntentApiTrigger}, or {@link OpenAITrigger}. */
public class VoiceRecognitionTrigger {

  private final InputMethodService mInputMethodService;

  private Trigger mTrigger;

  private ImeTrigger mImeTrigger;
  private IntentApiTrigger mIntentApiTrigger;
  private OpenAITrigger mOpenAITrigger;
  
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
      // Set up callback to forward recording state changes
      mOpenAITrigger.setRecordingStateCallback(isRecording -> {
        if (mRecordingStateCallback != null) {
          mRecordingStateCallback.onRecordingStateChanged(isRecording);
        }
      });
    }
    return mOpenAITrigger;
  }

  public boolean isInstalled() {
    android.util.Log.d("LongPressDebug", "VoiceRecognitionTrigger.isInstalled() called, returning: " + (mTrigger != null));
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
    
    // Ensure callback is preserved after trigger refresh
    if (mRecordingStateCallback != null) {
      setRecordingStateCallback(mRecordingStateCallback);
    }
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
  
  /**
   * Sets the callback for recording state changes.
   * @param callback The callback to be notified when recording state changes
   */
public void setRecordingStateCallback(RecordingStateCallback callback) {
    mRecordingStateCallback = callback;
    // If we already have an OpenAITrigger, set the callback on it too
    if (mOpenAITrigger != null) {
      mOpenAITrigger.setRecordingStateCallback(isRecording -> {
        if (mRecordingStateCallback != null) {
          mRecordingStateCallback.onRecordingStateChanged(isRecording);
        }
      });
    }
    // If the current trigger is OpenAI, ensure callback is set
    if (mTrigger instanceof OpenAITrigger) {
      ((OpenAITrigger) mTrigger).setRecordingStateCallback(isRecording -> {
        if (mRecordingStateCallback != null) {
          mRecordingStateCallback.onRecordingStateChanged(isRecording);
        }
      });
    }
  }
  
  public void setTranscriptionStateCallback(TranscriptionStateCallback callback) {
    mTranscriptionStateCallback = callback;
    // If the current trigger is OpenAI, set the callback
    if (mTrigger instanceof OpenAITrigger) {
      ((OpenAITrigger) mTrigger).setTranscriptionStateCallback(isTranscribing -> {
        if (mTranscriptionStateCallback != null) {
          mTranscriptionStateCallback.onTranscriptionStateChanged(isTranscribing);
        }
      });
    }
  }
  
  public void setTranscriptionErrorCallback(TranscriptionErrorCallback callback) {
    mTranscriptionErrorCallback = callback;
    // If the current trigger is OpenAI, set the callback
    if (mTrigger instanceof OpenAITrigger) {
      ((OpenAITrigger) mTrigger).setTranscriptionErrorCallback(error -> {
        if (mTranscriptionErrorCallback != null) {
          mTranscriptionErrorCallback.onTranscriptionError(error);
        }
      });
    }
  }
  
  public void setRecordingEndedCallback(RecordingEndedCallback callback) {
    mRecordingEndedCallback = callback;
    // If the current trigger is OpenAI, set the callback
    if (mTrigger instanceof OpenAITrigger) {
      ((OpenAITrigger) mTrigger).setRecordingEndedCallback(() -> {
        if (mRecordingEndedCallback != null) {
          mRecordingEndedCallback.onRecordingEnded();
        }
      });
    }
  }
  
  public void setTextWrittenCallback(TextWrittenCallback callback) {
    mTextWrittenCallback = callback;
    // If the current trigger is OpenAI, set the callback
    if (mTrigger instanceof OpenAITrigger) {
      ((OpenAITrigger) mTrigger).setTextWrittenCallback(text -> {
        if (mTextWrittenCallback != null) {
          mTextWrittenCallback.onTextWritten(text);
        }
      });
    }
  }
}
