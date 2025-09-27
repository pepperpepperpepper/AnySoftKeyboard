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

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.menny.android.anysoftkeyboard.R;

/**
 * DialogFragment wrapper for OpenAI saved prompts management.
 * This provides a proper dialog background and window management.
 */
public class OpenAISavedPromptsDialogFragment extends DialogFragment implements OpenAISavedPromptsFragment.OnPromptSelectedListener {

    private OpenAISavedPromptsFragment promptsFragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.openai_saved_prompts_dialog_container, container, false);
        
        // Create and embed the actual prompts fragment
        if (getChildFragmentManager().findFragmentById(R.id.prompts_fragment_container) == null) {
            promptsFragment = new OpenAISavedPromptsFragment();
            if (getArguments() != null) {
                promptsFragment.setArguments(getArguments());
            }
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.prompts_fragment_container, promptsFragment)
                    .commit();
        } else {
            // If fragment already exists, set up the listener
            promptsFragment = (OpenAISavedPromptsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.prompts_fragment_container);
        }
        
        // Listener will be set in onResume to ensure fragment is ready
        
        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // Request a window feature to set the title
        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Set dialog properties for proper background and sizing
        if (getDialog() != null && getDialog().getWindow() != null) {
            Window window = getDialog().getWindow();
            
            // Set a solid background
            window.setBackgroundDrawableResource(android.R.color.white);
            
            // Set dialog size to match parent width and wrap height
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            
            // Add some margin for better appearance
            window.getDecorView().setPadding(32, 32, 32, 32);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Set up the listener when fragment is resumed
        if (promptsFragment == null) {
            promptsFragment = (OpenAISavedPromptsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.prompts_fragment_container);
        }
        
        if (promptsFragment != null) {
            android.util.Log.d("OpenAISavedPromptsDialog", "Setting listener on prompts fragment in onResume");
            promptsFragment.setListener(this);
        } else {
            android.util.Log.e("OpenAISavedPromptsDialog", "Prompts fragment is null in onResume!");
        }
    }

    @Override
    public void onPromptSelected(String promptText) {
        android.util.Log.d("OpenAISavedPromptsDialog", "onPromptSelected called with: " + promptText);
        
        // First, update the preference directly (this was working before)
        updatePromptPreference(promptText);
        
        // Dismiss this dialog
        dismiss();
        
        // Try to show the prompt dialog
        if (getActivity() != null) {
            android.util.Log.d("OpenAISavedPromptsDialog", "Activity available, attempting to show prompt dialog");
            // Post to ensure dialog is dismissed before showing the next one
            getActivity().runOnUiThread(() -> {
                showPromptDialogAfterSelection();
            });
        } else {
            android.util.Log.e("OpenAISavedPromptsDialog", "Activity is null!");
        }
    }
    
    private void updatePromptPreference(String promptText) {
        if (getActivity() == null) return;
        
        android.util.Log.d("OpenAISavedPromptsDialog", "Updating prompt preference with: " + promptText);
        
        // Update SharedPreferences directly
        android.content.SharedPreferences prefs = getActivity().getSharedPreferences(
                "com.menny.android.anysoftkeyboard_preferences", android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getActivity().getString(R.string.settings_key_openai_prompt), promptText);
        editor.apply();
        
        android.util.Log.d("OpenAISavedPromptsDialog", "Updated SharedPreferences with prompt text");
        
        // Try to find the OpenAISpeechSettingsFragment and update the preference directly
        try {
            androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            
            // First, look for OpenAISpeechSettingsFragment in current fragments (direct case)
            for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) {
                    android.util.Log.d("OpenAISavedPromptsDialog", "Found OpenAISpeechSettingsFragment directly, updating preference");
                    com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment settingsFragment = (com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) fragment;
                    settingsFragment.updatePromptPreference(promptText);
                    return;
                }
            }
            
            // If not found directly, look inside NavHostFragment (navigation case)
            for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof androidx.navigation.fragment.NavHostFragment) {
                    android.util.Log.d("OpenAISavedPromptsDialog", "Found NavHostFragment, looking inside for OpenAISpeechSettingsFragment");
                    androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) fragment;
                    androidx.fragment.app.FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
                    
                    for (androidx.fragment.app.Fragment childFragment : childFragmentManager.getFragments()) {
                        if (childFragment instanceof com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) {
                            android.util.Log.d("OpenAISavedPromptsDialog", "Found OpenAISpeechSettingsFragment inside NavHostFragment, updating preference");
                            com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment settingsFragment = (com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) childFragment;
                            settingsFragment.updatePromptPreference(promptText);
                            return;
                        }
                    }
                }
            }
            
            android.util.Log.w("OpenAISavedPromptsDialog", "OpenAISpeechSettingsFragment not found, preference only updated in SharedPreferences");
        } catch (Exception e) {
            android.util.Log.e("OpenAISavedPromptsDialog", "Error updating prompt preference", e);
        }
    }
    
    private void showPromptDialogAfterSelection() {
        if (getActivity() == null) return;
        
        android.util.Log.d("OpenAISavedPromptsDialog", "Attempting to show prompt dialog after selection");
        
        // Try to find the OpenAISpeechSettingsFragment and click the Prompt preference directly
        try {
            androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
            
            // First, look for OpenAISpeechSettingsFragment in current fragments (direct case)
            for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) {
                    android.util.Log.d("OpenAISavedPromptsDialog", "Found OpenAISpeechSettingsFragment directly, clicking prompt preference");
                    com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment settingsFragment = (com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) fragment;
                    clickPromptPreference(settingsFragment);
                    return;
                }
            }
            
            // If not found directly, look inside NavHostFragment (navigation case)
            for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
                if (fragment instanceof androidx.navigation.fragment.NavHostFragment) {
                    android.util.Log.d("OpenAISavedPromptsDialog", "Found NavHostFragment, looking inside for OpenAISpeechSettingsFragment");
                    androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) fragment;
                    androidx.fragment.app.FragmentManager childFragmentManager = navHostFragment.getChildFragmentManager();
                    
                    for (androidx.fragment.app.Fragment childFragment : childFragmentManager.getFragments()) {
                        if (childFragment instanceof com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) {
                            android.util.Log.d("OpenAISavedPromptsDialog", "Found OpenAISpeechSettingsFragment inside NavHostFragment, clicking prompt preference");
                            com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment settingsFragment = (com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment) childFragment;
                            clickPromptPreference(settingsFragment);
                            return;
                        }
                    }
                }
            }
            
            // If not found in current fragments, check if we can use MainSettingsActivity navigation
            if (getActivity() instanceof com.anysoftkeyboard.ui.settings.MainSettingsActivity) {
                android.util.Log.d("OpenAISavedPromptsDialog", "Fragment not found, using MainSettingsActivity navigation");
                com.anysoftkeyboard.ui.settings.MainSettingsActivity activity = (com.anysoftkeyboard.ui.settings.MainSettingsActivity) getActivity();
                activity.navigateToOpenAISettings();
            } else {
                // Fallback for other activity types (like direct launch)
                android.util.Log.d("OpenAISavedPromptsDialog", "Activity is not MainSettingsActivity, showing toast");
                android.widget.Toast.makeText(getActivity(), "Prompt applied. Open settings to edit.", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("OpenAISavedPromptsDialog", "Error showing prompt dialog", e);
            android.widget.Toast.makeText(getActivity(), "Prompt applied. Open settings to edit.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void clickPromptPreference(com.anysoftkeyboard.ui.settings.OpenAISpeechSettingsFragment settingsFragment) {
        if (getActivity() == null) return;
        
        try {
            android.util.Log.d("OpenAISavedPromptsDialog", "Attempting to show prompt dialog with delay");
            
            // Show the dialog immediately without delay
            try {
                if (getActivity() != null && !settingsFragment.isDetached()) {
                    android.util.Log.d("OpenAISavedPromptsDialog", "Showing prompt dialog immediately");
                    // Use the fragment's showPromptDialog method which handles the preference click
                    settingsFragment.showPromptDialog();
                } else {
                    android.util.Log.w("OpenAISavedPromptsDialog", "Activity null or fragment detached, cannot show dialog");
                }
            } catch (Exception e) {
                android.util.Log.e("OpenAISavedPromptsDialog", "Error showing prompt dialog", e);
            }
            
        } catch (Exception e) {
            android.util.Log.e("OpenAISavedPromptsDialog", "Error setting up prompt dialog click", e);
        }
    }
    
    
}