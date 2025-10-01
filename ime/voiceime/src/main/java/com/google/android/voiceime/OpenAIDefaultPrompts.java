/*
 * Copyright (C) 2025 AnySoftKeyboard
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

/**
 * Utility class for managing default OpenAI prompts that provide better transcription quality.
 * These prompts are based on the voicepipe project's proven prompt templates.
 */
public class OpenAIDefaultPrompts {

    /**
     * Default prompt for Whisper-1 model.
     * Provides example of proper capitalization and dialogue formatting.
     */
    public static final String WHISPER_PROMPT = 
        "She said, \"Hello, how are you?\" Then she asked, \"What's your name?\" I replied, \"My name is John.\"";
    
    /**
     * Default prompt for GPT-4o model in dictation mode.
     * Provides comprehensive instructions for punctuation conversion and formatting.
     */
    public static final String GPT4_DICTATION_PROMPT = 
        "Please transcribe in dictation mode. When the speaker says punctuation commands, convert them to actual punctuation:\n" +
        "- \"open quote\" or \"quotation mark\" → \"\n" +
        "- \"close quote\" or \"end quote\" → \"\n" +
        "- \"comma\" → ,\n" +
        "- \"period\" → .\n" +
        "- \"question mark\" → ?\n" +
        "- \"exclamation mark\" → !\n\n" +
        "Example: If speaker says \"open quote hello close quote\", transcribe as: \"hello\"";
    
    /**
     * Prompt type enumeration.
     */
    public enum PromptType {
        WHISPER("whisper", WHISPER_PROMPT),
        GPT4_DICTATION("gpt4_dictation", GPT4_DICTATION_PROMPT),
        NONE("none", "");
        
        private final String value;
        private final String prompt;
        
        PromptType(String value, String prompt) {
            this.value = value;
            this.prompt = prompt;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getPrompt() {
            return prompt;
        }
        
        public static PromptType fromValue(String value) {
            for (PromptType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return WHISPER; // Default fallback
        }
    }
    
    /**
     * Gets the appropriate default prompt based on the model and prompt type.
     * 
     * @param model The OpenAI model being used
     * @param promptType The selected prompt type
     * @return The default prompt string, or empty string if none
     */
    public static String getDefaultPrompt(String model, PromptType promptType) {
        if (promptType == PromptType.NONE) {
            return "";
        }
        
        // If the prompt type matches the model, use it directly
        if ((promptType == PromptType.WHISPER && "whisper-1".equals(model)) ||
            (promptType == PromptType.GPT4_DICTATION && model != null && model.startsWith("gpt-4"))) {
            return promptType.getPrompt();
        }
        
        // Auto-select based on model if the chosen type doesn't match
        if (model != null && model.startsWith("gpt-4")) {
            return GPT4_DICTATION_PROMPT;
        } else {
            return WHISPER_PROMPT;
        }
    }
    
    /**
     * Combines the default prompt with a custom prompt based on the append setting.
     * 
     * @param defaultPrompt The default prompt to use
     * @param customPrompt The user's custom prompt
     * @param appendCustom Whether to append the custom prompt or replace the default
     * @return The combined prompt string
     */
    public static String combinePrompts(String defaultPrompt, String customPrompt, boolean appendCustom) {
        if (defaultPrompt == null) defaultPrompt = "";
        if (customPrompt == null) customPrompt = "";
        
        if (defaultPrompt.isEmpty()) {
            return customPrompt;
        }
        
        if (customPrompt.isEmpty()) {
            return defaultPrompt;
        }
        
        if (appendCustom) {
            return defaultPrompt + "\n\n" + customPrompt;
        } else {
            return customPrompt;
        }
    }
    
    /**
     * Gets the recommended prompt type for a given model.
     * 
     * @param model The OpenAI model
     * @return The recommended PromptType
     */
    public static PromptType getRecommendedPromptType(String model) {
        if (model != null && model.startsWith("gpt-4")) {
            return PromptType.GPT4_DICTATION;
        } else {
            return PromptType.WHISPER;
        }
    }
}