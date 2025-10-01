/*
 * Copyright (c) 2025 AnySoftKeyboard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anysoftkeyboard.ui.settings;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.menny.android.anysoftkeyboard.R;
import net.evendanan.pixel.UiUtils;

public class OpenAISpeechSettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.prefs_openai_speech);
        
        // Set up the prompt preference with clear functionality
        OpenAIPromptEditTextPreference promptPreference = findPreference(getString(R.string.settings_key_openai_prompt));
        if (promptPreference != null) {
            promptPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Let the preference handle the value change
                return true;
            });
        }
        
        // Set up default prompt type preference to auto-select based on model
        ListPreference defaultPromptTypePreference = findPreference(getString(R.string.settings_key_openai_default_prompt_type));
        if (defaultPromptTypePreference != null) {
            defaultPromptTypePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                // Update summary to show selected option
                if (newValue instanceof String) {
                    String[] entries = getResources().getStringArray(R.array.openai_default_prompt_type_entries);
                    String[] values = getResources().getStringArray(R.array.openai_default_prompt_type_values);
                    for (int i = 0; i < values.length; i++) {
                        if (values[i].equals(newValue)) {
                            defaultPromptTypePreference.setSummary(entries[i]);
                            break;
                        }
                    }
                }
                return true;
            });
            
            // Set initial summary
            String currentValue = defaultPromptTypePreference.getValue();
            if (currentValue != null) {
                String[] entries = getResources().getStringArray(R.array.openai_default_prompt_type_entries);
                String[] values = getResources().getStringArray(R.array.openai_default_prompt_type_values);
                for (int i = 0; i < values.length; i++) {
                    if (values[i].equals(currentValue)) {
                        defaultPromptTypePreference.setSummary(entries[i]);
                        break;
                    }
                }
            }
        }
        
        // Set up the saved prompts preference
        Preference savedPromptsPreference = findPreference(getString(R.string.settings_key_openai_saved_prompts));
        if (savedPromptsPreference != null) {
            savedPromptsPreference.setOnPreferenceClickListener(preference -> {
                // Open the saved prompts management dialog
                OpenAISavedPromptsDialogFragment dialogFragment = new OpenAISavedPromptsDialogFragment();
                if (getActivity() != null) {
                    dialogFragment.show(getActivity().getSupportFragmentManager(), "OpenAISavedPromptsDialog");
                }
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Check if we should open prompt dialog
        if (getActivity() != null && getActivity().getIntent() != null) {
            boolean shouldOpenPrompt = getActivity().getIntent().getBooleanExtra("open_prompt_dialog", false);
            String promptTextToLoad = getActivity().getIntent().getStringExtra("prompt_text_to_load");
            
            if (shouldOpenPrompt) {
                // Post to ensure preferences are fully loaded
                view.post(() -> {
                    // If we have prompt text to load, update the preference first
                    if (promptTextToLoad != null) {
                        updatePromptPreference(promptTextToLoad);
                        // Clear the extra so it doesn't reload again
                        getActivity().getIntent().removeExtra("prompt_text_to_load");
                    }
                    
                    Preference promptPreference = findPreference(getString(R.string.settings_key_openai_prompt));
                    if (promptPreference != null) {
                        promptPreference.performClick();
                    }
                });
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        UiUtils.setActivityTitle(this, R.string.openai_speech_settings_title);
    }

    public void showPromptDialog() {
        Preference promptPreference = findPreference(getString(R.string.settings_key_openai_prompt));
        if (promptPreference != null) {
            android.util.Log.d("OpenAISpeechSettings", "Showing prompt dialog");
            promptPreference.performClick();
        }
    }
    
    public void updatePromptPreference(String promptText) {
        OpenAIPromptEditTextPreference promptPreference = findPreference(getString(R.string.settings_key_openai_prompt));
        if (promptPreference != null) {
            android.util.Log.d("OpenAISpeechSettings", "Updating prompt preference with: " + promptText);
            promptPreference.setText(promptText);
            // Also ensure the SharedPreferences is updated
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences(
                    "com.menny.android.anysoftkeyboard_preferences", android.content.Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString(getString(R.string.settings_key_openai_prompt), promptText);
            editor.apply();
            android.util.Log.d("OpenAISpeechSettings", "Also updated SharedPreferences with prompt: " + promptText);
        }
    }
}