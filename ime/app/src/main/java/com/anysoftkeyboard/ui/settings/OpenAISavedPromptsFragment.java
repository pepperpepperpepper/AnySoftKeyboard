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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for managing saved OpenAI prompts (CRUD operations).
 */
public class OpenAISavedPromptsFragment extends Fragment {

    private OpenAISavedPromptsManager promptsManager;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private PromptAdapter adapter;
    private View addButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        promptsManager = new OpenAISavedPromptsManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.openai_saved_prompts_fragment, container, false);
        
        recyclerView = view.findViewById(R.id.prompts_recycler_view);
        emptyView = view.findViewById(R.id.empty_view);
        addButton = view.findViewById(R.id.add_button);
        
        setupRecyclerView();
        setupAddButton();
        loadPrompts();
        
        return view;
    }

    private void setupRecyclerView() {
        adapter = new PromptAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupAddButton() {
        addButton.setOnClickListener(v -> showAddEditDialog(null));
    }

    private void loadPrompts() {
        List<OpenAISavedPrompt> prompts = promptsManager.getAllPrompts();
        adapter.setPrompts(prompts);
        
        if (prompts.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showAddEditDialog(@Nullable OpenAISavedPrompt prompt) {
        boolean isEdit = prompt != null;
        
        View dialogView = getLayoutInflater().inflate(R.layout.openai_saved_prompt_dialog, null);
        EditText textEdit = dialogView.findViewById(R.id.prompt_text_edit);
        Button saveButton = dialogView.findViewById(R.id.save_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        if (isEdit) {
            textEdit.setText(prompt.getText());
        } else {
            // Check if we have pre-filled text from arguments
            Bundle args = getArguments();
            if (args != null && args.containsKey("pre_filled_prompt_text")) {
                String preFilledText = args.getString("pre_filled_prompt_text");
                textEdit.setText(preFilledText);
            }
        }

        // Enable/disable save button based on input
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                saveButton.setEnabled(!TextUtils.isEmpty(textEdit.getText().toString().trim()));
            }
        };
        
        textEdit.addTextChangedListener(textWatcher);
        
        // Initial state
        saveButton.setEnabled(!TextUtils.isEmpty(textEdit.getText().toString().trim()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? R.string.openai_saved_prompts_edit : R.string.openai_saved_prompts_add)
                .setView(dialogView)
                .create();

        saveButton.setOnClickListener(v -> {
            String text = textEdit.getText().toString().trim();
            
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(getContext(), R.string.openai_saved_prompts_error_text_required, Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (isEdit) {
                prompt.setText(text);
                if (promptsManager.updatePrompt(prompt)) {
                    Toast.makeText(getContext(), R.string.openai_saved_prompts_save_success, Toast.LENGTH_SHORT).show();
                    loadPrompts();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), R.string.openai_saved_prompts_error_save_failed, Toast.LENGTH_SHORT).show();
                }
            } else {
                OpenAISavedPrompt newPrompt = new OpenAISavedPrompt(text);
                if (promptsManager.savePrompt(newPrompt)) {
                    Toast.makeText(getContext(), R.string.openai_saved_prompts_save_success, Toast.LENGTH_SHORT).show();
                    loadPrompts();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), R.string.openai_saved_prompts_error_save_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteConfirmationDialog(@NonNull OpenAISavedPrompt prompt) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.openai_saved_prompts_delete)
                .setMessage(R.string.openai_saved_prompts_delete_confirm)
                .setPositiveButton(R.string.openai_saved_prompts_delete_yes, (dialog, which) -> {
                    if (promptsManager.deletePrompt(prompt.getId())) {
                        Toast.makeText(getContext(), R.string.openai_saved_prompts_delete_success, Toast.LENGTH_SHORT).show();
                        loadPrompts();
                    } else {
                        Toast.makeText(getContext(), R.string.openai_saved_prompts_error_save_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.openai_saved_prompts_delete_no, null)
                .show();
    }

    private void insertPromptIntoMainField(@NonNull OpenAISavedPrompt prompt) {
        // Get the current prompt from SharedPreferences
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(
                "com.menny.android.anysoftkeyboard_preferences", Context.MODE_PRIVATE);
        String currentPrompt = prefs.getString(getString(R.string.settings_key_openai_prompt), "");
        
        // Append the selected prompt text
        String newPrompt = currentPrompt + (TextUtils.isEmpty(currentPrompt) ? "" : " ") + prompt.getText();
        
        // Save the updated prompt
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString(getString(R.string.settings_key_openai_prompt), newPrompt);
        editor.apply();
        
        Toast.makeText(getContext(), R.string.openai_saved_prompts_insert_success, Toast.LENGTH_SHORT).show();
        
        // Dismiss the dialog if we're in one
        if (getActivity() != null && getActivity().getSupportFragmentManager().findFragmentByTag("OpenAISavedPromptsDialog") != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private class PromptAdapter extends RecyclerView.Adapter<PromptAdapter.PromptViewHolder> {
        private List<OpenAISavedPrompt> prompts = new ArrayList<>();

        @NonNull
        @Override
        public PromptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.openai_saved_prompt_item, parent, false);
            return new PromptViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PromptViewHolder holder, int position) {
            OpenAISavedPrompt prompt = prompts.get(position);
            holder.previewText.setText(prompt.getText());
            
            holder.itemView.setOnClickListener(v -> insertPromptIntoMainField(prompt));
            holder.editButton.setOnClickListener(v -> showAddEditDialog(prompt));
            holder.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog(prompt));
        }

        @Override
        public int getItemCount() {
            return prompts.size();
        }

        public void setPrompts(List<OpenAISavedPrompt> prompts) {
            this.prompts = prompts;
            notifyDataSetChanged();
        }

        class PromptViewHolder extends RecyclerView.ViewHolder {
            TextView previewText;
            TextView editButton;
            TextView deleteButton;

            PromptViewHolder(@NonNull View itemView) {
                super(itemView);
                previewText = itemView.findViewById(R.id.prompt_preview);
                editButton = itemView.findViewById(R.id.edit_button);
                deleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
}