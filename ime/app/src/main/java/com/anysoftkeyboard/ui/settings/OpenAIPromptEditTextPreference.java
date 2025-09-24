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
import android.os.Bundle;
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
        
        editText.setText(getText());
        
        builder.setView(dialogView);
        
        // Set up AlertDialog buttons (OK and Cancel)
        builder.setPositiveButton("USE", (dialog, which) -> {
            String newValue = editText.getText().toString();
            if (callChangeListener(newValue)) {
                setText(newValue);
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Set up Save button click handler
        saveButton.setOnClickListener(v -> {
            String currentText = editText.getText().toString().trim();
            if (!currentText.isEmpty()) {
                // Open saved prompts fragment with pre-filled text
                openSavedPromptsFragmentWithText(currentText);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Please enter prompt text to save", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up Clear button click handler
        clearButton.setOnClickListener(v -> {
            editText.setText("");
            // Automatically save the empty string as the prompt value
            String newValue = "";
            if (callChangeListener(newValue)) {
                setText(newValue);
            }
            // Note: dialog stays open so user can continue editing if needed
        });
        
        // Enable/disable OK button based on text input
        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (okButton != null) {
            okButton.setEnabled(editText.getText().length() > 0);
            editText.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    okButton.setEnabled(s.length() > 0);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }
    
    private void openSavedPromptsFragmentWithText(String promptText) {
        try {
            // Get the current fragment manager from the context
            Context context = getContext();
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
                
// Create saved prompts dialog fragment
            OpenAISavedPromptsDialogFragment dialogFragment = new OpenAISavedPromptsDialogFragment();
            
            // Pass the prompt text as argument
            Bundle args = new Bundle();
            args.putString("pre_filled_prompt_text", promptText);
            dialogFragment.setArguments(args);
            
            // Show the dialog
            dialogFragment.show(activity.getSupportFragmentManager(), "OpenAISavedPromptsDialog");            }
        } catch (Exception e) {
            e.printStackTrace();
Toast.makeText(getContext(), "Unable to open saved prompts", Toast.LENGTH_SHORT).show();
        }
    }
}