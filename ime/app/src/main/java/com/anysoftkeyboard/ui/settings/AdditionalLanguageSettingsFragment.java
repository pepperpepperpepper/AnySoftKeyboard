/*
 * Copyright (c) 2013 Menny Even-Danan
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

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.menny.android.anysoftkeyboard.R;
import net.evendanan.pixel.UiUtils;

public class AdditionalLanguageSettingsFragment extends PreferenceFragmentCompat
    implements Preference.OnPreferenceClickListener {

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    addPreferencesFromResource(R.xml.prefs_addtional_language_prefs);
    
    // Debug: Check if the OpenAI preference exists
    Preference openaiPref = findPreference(getString(R.string.settings_key_openai_speech_settings));
    if (openaiPref != null) {
      android.util.Log.d("AdditionalLanguageSettings", "OpenAI preference found: " + openaiPref.getTitle());
    } else {
      android.util.Log.d("AdditionalLanguageSettings", "OpenAI preference NOT found!");
    }
    
    // Debug: Check if the tweaks preference exists
    Preference tweaksPref = findPreference(getString(R.string.tweaks_group_key));
    if (tweaksPref != null) {
      android.util.Log.d("AdditionalLanguageSettings", "Tweaks preference found: " + tweaksPref.getTitle());
    } else {
      android.util.Log.d("AdditionalLanguageSettings", "Tweaks preference NOT found!");
    }
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    findPreference(getString(R.string.tweaks_group_key)).setOnPreferenceClickListener(this);
    findPreference(getString(R.string.settings_key_openai_speech_settings)).setOnPreferenceClickListener(this);
  }

  @Override
  public void onStart() {
    super.onStart();
    UiUtils.setActivityTitle(this, R.string.language_tweaks_settings_tile);
  }

  @Override
  public boolean onPreferenceClick(Preference preference) {
    if (preference.getKey().equals(getString(R.string.tweaks_group_key))) {
      Navigation.findNavController(requireView())
          .navigate(
              AdditionalLanguageSettingsFragmentDirections
                  .actionAdditionalLanguageSettingsFragmentToLanguageTweaksFragment());
      return true;
    } else if (preference.getKey().equals(getString(R.string.settings_key_openai_speech_settings))) {
      Navigation.findNavController(requireView())
          .navigate(
              AdditionalLanguageSettingsFragmentDirections
                  .actionAdditionalLanguageSettingsFragmentToOpenAISpeechSettingsFragment());
      return true;
    }
    return false;
  }
}
