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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import com.menny.android.anysoftkeyboard.R;

public class OpenAIPromptEditTextPreference extends EditTextPreference {

    public OpenAIPromptEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public OpenAIPromptEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OpenAIPromptEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenAIPromptEditTextPreference(@NonNull Context context) {
        super(context);
    }

@Override
    protected void onClick() {
        // Create a custom dialog with Save and Clear buttons in layout, plus OK and Cancel
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getDialogTitle());
        
        // Inflate custom layout
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.openai_prompt_dialog, null);
        
        EditText editText = dialogView.findViewById(R.id.editText);
        Button saveButton = dialogView.findViewById(R.id.save_button);
        Button clearButton = dialogView.findViewById(R.id.clear_button);
        
        // Get the current text and store it as the original state
        // Use array to make it effectively final for lambda expressions
        final String[] originalText = {getText()};
        
        // Log the current text for debugging
        android.util.Log.d("OpenAIPromptEditTextPreference", "Current prompt text: " + originalText[0]);
        
        editText.setText(originalText[0]);
        
        builder.setView(dialogView);
        
        // Set up AlertDialog buttons (USE and Cancel)
        builder.setPositiveButton("USE", (dialog, which) -> {
            // This will be overridden below to prevent automatic dismissal
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Override the USE button click to prevent automatic dismissal
        dialog.getWindow().getDecorView().post(() -> {
            Button useButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (useButton != null) {
                // Initially disable USE button since no changes have been made yet
                useButton.setEnabled(false);
                
                // Set up custom click handler for USE button
                useButton.setOnClickListener(v -> {
                    String newValue = editText.getText().toString();
                    if (callChangeListener(newValue)) {
                        setText(newValue);
                        // Update original text to current text after saving
                        originalText[0] = newValue;
                        // Disable USE button since state is now in sync
                        useButton.setEnabled(false);
                        // Show feedback that text was saved
                        Toast.makeText(getContext(), "Prompt In Use", Toast.LENGTH_SHORT).show();
                    }
                    // Note: dialog stays open for further editing
                });
                
                // Set up text change listener to enable/disable USE button based on state changes
                editText.addTextChangedListener(new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        // Enable USE button only if text has changed from original state
                        String currentText = editText.getText().toString();
                        boolean hasChanged = !currentText.equals(originalText[0]);
                        useButton.setEnabled(hasChanged);
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {}
                });
            }
        });
        
        // Set up Save button click handler
        saveButton.setOnClickListener(v -> {
            String currentText = editText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                // Save the current prompt to the saved prompts list
                saveCurrentPromptAndOpenDialog(currentText);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter prompt text to save", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up Clear button click handler
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            // The text change listener will handle enabling/disabling the USE button
            // No automatic save - user must click USE to apply the empty state
        });
    }
    
    private void saveCurrentPromptAndOpenDialog(String promptText) {
        try {
            // Create the prompts manager and save the current prompt
            OpenAISavedPromptsManager promptsManager = new OpenAISavedPromptsManager(getContext());
            OpenAISavedPrompt newPrompt = new OpenAISavedPrompt(promptText);
            
            if (promptsManager.savePrompt(newPrompt)) {
                android.util.Log.d("OpenAIPromptEditTextPreference", "Prompt saved successfully: " + promptText);
                Toast.makeText(getContext(), "Prompt saved successfully", Toast.LENGTH_SHORT).show();
                
                // Now open the saved prompts dialog to show the newly saved prompt
                openSavedPromptsDialog();
            } else {
                android.util.Log.e("OpenAIPromptEditTextPreference", "Failed to save prompt: " + promptText);
                Toast.makeText(getContext(), "Failed to save prompt", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("OpenAIPromptEditTextPreference", "Error saving prompt", e);
            Toast.makeText(getContext(), "Error saving prompt", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openSavedPromptsDialog() {
        try {
            // Get the current fragment manager from the context
            Context context = getContext();
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
                
                // Create saved prompts dialog fragment
                OpenAISavedPromptsDialogFragment dialogFragment = new OpenAISavedPromptsDialogFragment();
                
                // Show the dialog
                dialogFragment.show(activity.getSupportFragmentManager(), "OpenAISavedPromptsDialog");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Unable to open saved prompts", Toast.LENGTH_SHORT).show();
        }
    }
}