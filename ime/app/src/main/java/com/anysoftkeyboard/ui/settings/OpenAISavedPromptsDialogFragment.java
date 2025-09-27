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
            
            // Set up the listener after the fragment is created
            promptsFragment.setListener(this);
        } else {
            // If fragment already exists, set up the listener
            promptsFragment = (OpenAISavedPromptsFragment) getChildFragmentManager()
                    .findFragmentById(R.id.prompts_fragment_container);
            promptsFragment.setListener(this);
        }
        
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
    public void onPromptSelected(String promptText) {
        // Dismiss this dialog
        dismiss();
        
        // Show the prompt dialog with the selected text
        if (getActivity() != null) {
            // Post to ensure dialog is dismissed before showing the next one
            getActivity().runOnUiThread(() -> {
                new android.os.Handler().postDelayed(() -> {
                    showPromptDialogInSettings();
                }, 100);
            });
        }
    }
    
    private void showPromptDialogInSettings() {
        if (getActivity() == null) return;
        
        // Try to find the OpenAISpeechSettingsFragment and show the prompt dialog
        androidx.fragment.app.FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        
        // Look for the settings fragment in the current fragments
        for (androidx.fragment.app.Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof OpenAISpeechSettingsFragment) {
                ((OpenAISpeechSettingsFragment) fragment).showPromptDialog();
                return;
            }
        }
        
        // If not found in current fragments, try to find it in the back stack
        if (fragmentManager.getBackStackEntryCount() > 0) {
            // Try to find the fragment by going through back stack entries
            androidx.fragment.app.FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1);
            String tag = entry.getName();
            
            // Try to find the fragment with the back stack tag
            androidx.fragment.app.Fragment fragment = fragmentManager.findFragmentByTag(tag);
            if (fragment instanceof OpenAISpeechSettingsFragment) {
                ((OpenAISpeechSettingsFragment) fragment).showPromptDialog();
                return;
            }
            
            // If still not found, pop the back stack and try again
            fragmentManager.popBackStack();
            new android.os.Handler().postDelayed(() -> {
                showPromptDialogInSettings();
            }, 300);
        } else {
            // Last resort: use intent extra to trigger prompt dialog when settings are shown
            if (getActivity() != null) {
                getActivity().getIntent().putExtra("open_prompt_dialog", true);
                // Show a toast to inform user
                android.widget.Toast.makeText(getActivity(), "Prompt saved. Please reopen settings to see it.", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }
}