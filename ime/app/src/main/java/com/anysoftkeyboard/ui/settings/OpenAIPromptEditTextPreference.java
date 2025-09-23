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
        // Create a custom dialog with three buttons: Clear, Cancel, OK
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getDialogTitle());
        
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.openai_prompt_dialog, null);
        
        EditText editText = dialogView.findViewById(R.id.editText);
        editText.setText(getText());
        
        builder.setView(dialogView);
        
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String newValue = editText.getText().toString();
            if (callChangeListener(newValue)) {
                setText(newValue);
            }
        });
        
        builder.setNegativeButton(android.R.string.cancel, null);
        
        builder.setNeutralButton(R.string.openai_prompt_clear_button, (dialog, which) -> {
            // Clear the text
            editText.setText("");
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Enable/disable the OK button based on text input
        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (okButton != null) {
            okButton.setEnabled(false);
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
}