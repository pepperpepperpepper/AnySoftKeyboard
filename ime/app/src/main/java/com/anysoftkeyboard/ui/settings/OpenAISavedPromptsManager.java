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
        android.util.Log.d("OpenAISavedPromptsManager", "Getting prompts, JSON: " + (json != null ? "found" : "null"));
        if (json == null) {
            android.util.Log.d("OpenAISavedPromptsManager", "No saved prompts found, returning empty list");
            return new ArrayList<>();
        }
        
        try {
            JSONArray jsonArray = new JSONArray(json);
            List<OpenAISavedPrompt> prompts = new ArrayList<>();
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                OpenAISavedPrompt prompt = new OpenAISavedPrompt(
                    jsonObject.getLong("id"),
                    jsonObject.getString("text"),
                    jsonObject.getLong("timestamp")
                );
                prompts.add(prompt);
                android.util.Log.d("OpenAISavedPromptsManager", "Loaded prompt: " + prompt.getText());
            }
            
            android.util.Log.d("OpenAISavedPromptsManager", "Successfully loaded " + prompts.size() + " prompts");
            return prompts;
        } catch (JSONException e) {
            android.util.Log.e("OpenAISavedPromptsManager", "Error parsing prompts JSON", e);
            // If there's an error parsing, return empty list
            return new ArrayList<>();
        }
    }

    /**
     * Save a new prompt.
     */
    public boolean savePrompt(@NonNull OpenAISavedPrompt prompt) {
        android.util.Log.d("OpenAISavedPromptsManager", "Saving prompt: " + prompt.getText());
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        
        // Generate auto-incrementing ID
        long newId = 1;
        for (OpenAISavedPrompt existingPrompt : prompts) {
            if (existingPrompt.getId() >= newId) {
                newId = existingPrompt.getId() + 1;
            }
        }
        prompt.setId(newId);
        android.util.Log.d("OpenAISavedPromptsManager", "Generated ID: " + newId);
        
        prompts.add(prompt);
        boolean result = savePromptsList(prompts);
        android.util.Log.d("OpenAISavedPromptsManager", "Save result: " + result);
        return result;
    }

    /**
     * Update an existing prompt.
     */
    public boolean updatePrompt(@NonNull OpenAISavedPrompt prompt) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId() == prompt.getId()) {
                prompts.set(i, prompt);
                return savePromptsList(prompts);
            }
        }
        return false; // Prompt not found
    }

    /**
     * Delete a prompt by ID.
     */
    public boolean deletePrompt(long promptId) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (int i = 0; i < prompts.size(); i++) {
            if (prompts.get(i).getId() == promptId) {
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
    public OpenAISavedPrompt getPromptById(long promptId) {
        List<OpenAISavedPrompt> prompts = getAllPrompts();
        for (OpenAISavedPrompt prompt : prompts) {
            if (prompt.getId() == promptId) {
                return prompt;
            }
        }
        return null;
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
            android.util.Log.d("OpenAISavedPromptsManager", "Saving " + prompts.size() + " prompts to JSON");
            JSONArray jsonArray = new JSONArray();
            
            for (OpenAISavedPrompt prompt : prompts) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", prompt.getId());
                jsonObject.put("text", prompt.getText());
                jsonObject.put("timestamp", prompt.getTimestamp());
                jsonArray.put(jsonObject);
                android.util.Log.d("OpenAISavedPromptsManager", "Added to JSON: " + prompt.getText());
            }
            
            String json = jsonArray.toString();
            android.util.Log.d("OpenAISavedPromptsManager", "JSON length: " + json.length());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SAVED_PROMPTS, json);
            boolean result = editor.commit();
            android.util.Log.d("OpenAISavedPromptsManager", "SharedPreferences commit result: " + result);
            return result;
        } catch (JSONException e) {
            android.util.Log.e("OpenAISavedPromptsManager", "JSON error while saving", e);
            return false;
        }
    }
}