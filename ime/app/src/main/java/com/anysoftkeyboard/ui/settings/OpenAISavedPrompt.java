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

import java.util.Objects;

/**
 * Data model for a saved OpenAI prompt.
 */
public class OpenAISavedPrompt {
    private long id;
    private String text;
    private long timestamp;

    public OpenAISavedPrompt() {
        // Default constructor for JSON deserialization
    }

    public OpenAISavedPrompt(String text) {
        this.id = -1; // Will be set by manager
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public OpenAISavedPrompt(long id, String text, long timestamp) {
        this.id = id;
        this.text = text;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpenAISavedPrompt that = (OpenAISavedPrompt) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OpenAISavedPrompt{" +
                "id=" + id +
                ", text='" + text + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}