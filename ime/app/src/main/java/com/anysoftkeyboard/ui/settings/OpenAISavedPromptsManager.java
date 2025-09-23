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
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages storage and retrieval of saved OpenAI prompts using SharedPreferences.
 */
public class OpenAISavedPromptsManager {
    private static final String PREFS_NAME = "openai_saved_prompts";
    private static final String KEY_SAVED_PROMPTS = "saved_prompts_list";
    
    private final SharedPreferences sharedPreferences;

    public OpenAISavedPromptsManager(@NonNull Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get all saved prompts.
     */
    @NonNull
    public List<OpenAISavedPrompt> getAllPrompts() {
        String json = sharedPreferences.getString(KEY_SAVED_PROMPTS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<OpenAISavedPrompt> prompts = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                OpenAISavedPrompt prompt = new OpenAISavedPrompt(
                    jsonObject.getString("id"),
                    jsonObject.getString("name"),
                    jsonObject.getString("text"),
                    jsonObject.getLong("timestamp")
                );
                prompts.add(prompt);
            }
            
            return prompts;
        } catch (JSONException e) {
            // If there's an error parsing, return empty list
            return new ArrayList<>();
        }
    }

    /**
     * Save a new prompt.
     */
    public boolean savePrompt(@NonNull OpenAISavedPrompt prompt) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        prompts.add(prompt);
        return savePromptsList(prompts);
    }

    /**
     * Update an existing prompt.
     */
    public boolean updatePrompt(@NonNull OpenAISavedPrompt prompt) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId().equals(prompt.getId())) {
                prompts.set(i, prompt);
                return savePromptsList(prompts);
            }
        }
        return false; // Prompt not found
    }

    /**
     * Delete a prompt by ID.
     */
    public boolean deletePrompt(@NonNull String promptId) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId().equals(promptId)) {
                prompts.remove(i);
                return savePromptsList(prompts);
            }
        }
        return false; // Prompt not found
    }

    /**
     * Get a prompt by ID.
     */
    @Nullable
    public OpenAISavedPrompt getPromptById(@NonNull String promptId) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (OpenAISavedPrompt prompt : prompts) {
            if (prompt.getId().equals(promptId)) {
                return prompt;
            }
        }
        return null;
    }

    /**
     * Check if a prompt with the given name exists.
     */
    public boolean promptNameExists(@NonNull String name) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (OpenAISavedPrompt prompt : prompts) {
            if (prompt.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the number of saved prompts.
     */
    public int getPromptCount() {
        return getAllPrompts().size();
    }

    /**
     * Clear all saved prompts.
     */
    public boolean clearAllPrompts() {
        return savePromptsList(new ArrayList<>());
    }

    /**
     * Save the list of prompts to SharedPreferences.
     */
    private boolean savePromptsList(@NonNull List<OpenAISavedPrompt> prompts) {
        try {
            JSONArray jsonArray = new JSONArray();
            
            for (OpenAISavedPrompt prompt : prompts) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", prompt.getId());
                jsonObject.put("name", prompt.getName());
                jsonObject.put("text", prompt.getText());
                jsonObject.put("timestamp", prompt.getTimestamp());
                jsonArray.put(jsonObject);
            }
            
            String json = jsonArray.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SAVED_PROMPTS, json);
            return editor.commit();
        } catch (JSONException e) {
            return false;
        }
    }
}