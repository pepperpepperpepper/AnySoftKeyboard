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

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.anysoftkeyboard.notification.NotificationIds;
import com.anysoftkeyboard.permissions.PermissionRequestHelper;
import com.anysoftkeyboard.prefs.DirectBootAwareSharedPreferences;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.Objects;
import net.evendanan.pixel.EdgeEffectHacker;
import pub.devrel.easypermissions.AfterPermissionGranted;

public class MainSettingsActivity extends AppCompatActivity {

  public static final String ACTION_REQUEST_PERMISSION_ACTIVITY =
      "ACTION_REQUEST_PERMISSION_ACTIVITY";
  public static final String ACTION_REVOKE_PERMISSION_ACTIVITY =
      "ACTION_REVOKE_PERMISSION_ACTIVITY";
  public static final String EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY =
      "EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY";

  private CharSequence mTitle;

  @Override
  protected void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    setContentView(R.layout.main_ui);

    mTitle = getTitle();

    final NavController navController =
        ((NavHostFragment)
                Objects.requireNonNull(
                    getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
            .getNavController();
    final BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
    NavigationUI.setupWithNavController(bottomNavigationView, navController);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // applying my very own Edge-Effect color
    EdgeEffectHacker.brandGlowEffect(this, ContextCompat.getColor(this, R.color.app_accent));
    handlePermissionRequest(getIntent());
    handleOpenAISettingsNavigation(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    handlePermissionRequest(intent);
    handleOpenAISettingsNavigation(intent);
  }

  private void handleOpenAISettingsNavigation(Intent intent) {
    if (intent == null) return;
    
    // Handle navigation to OpenAI settings
    if (intent.hasExtra("navigate_to_openai_settings")) {
      android.util.Log.d("MainSettingsActivity", "Found navigate_to_openai_settings extra, calling navigateToOpenAISettings");
      intent.removeExtra("navigate_to_openai_settings");
      navigateToOpenAISettings();
      return;
    }
  }
  
  private void handlePermissionRequest(Intent intent) {
    if (intent == null) return;
    
    if (ACTION_REQUEST_PERMISSION_ACTIVITY.equals(intent.getAction())
        && intent.hasExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY)) {
      final String permission = intent.getStringExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      intent.removeExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      if (Objects.equals(permission, Manifest.permission.READ_CONTACTS)) {
        startContactsPermissionRequest();
      } else {
        throw new IllegalArgumentException("Unknown permission request " + permission);
      }
    }

    if (ACTION_REVOKE_PERMISSION_ACTIVITY.equals(intent.getAction())
        && intent.hasExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY)) {
      final String permission = intent.getStringExtra(EXTRA_KEY_ACTION_REQUEST_PERMISSION_ACTIVITY);
      intent.removeExtra(ACTION_REVOKE_PERMISSION_ACTIVITY);
      if (Objects.equals(permission, Manifest.permission.READ_CONTACTS)) {
        AnyApplication.notifier(this).cancel(NotificationIds.RequestContactsPermission);
        DirectBootAwareSharedPreferences.create(getApplicationContext())
            .edit()
            .putBoolean(getString(R.string.settings_key_use_contacts_dictionary), false)
            .apply();
        finish();
      } else {
        throw new IllegalArgumentException("Unknown permission request " + permission);
      }
    }
  }

  @AfterPermissionGranted(PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE)
  public void startContactsPermissionRequest() {
    AnyApplication.notifier(this).cancel(NotificationIds.RequestContactsPermission);
    PermissionRequestHelper.check(this, PermissionRequestHelper.CONTACTS_PERMISSION_REQUEST_CODE);
  }

public void navigateToOpenAISettings() {
    navigateToOpenAISettings(null);
  }
  
  public void navigateToOpenAISettings(String promptText) {
    android.util.Log.d("MainSettingsActivity", "navigateToOpenAISettings called with prompt: " + promptText);
    
    // Navigate to OpenAI settings fragment
    final NavController navController =
        ((NavHostFragment)
                Objects.requireNonNull(
                    getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment)))
            .getNavController();
    
    // Set intent extra to open prompt dialog
    getIntent().putExtra("open_prompt_dialog", true);
    
    // If we have prompt text, store it for later use
    if (promptText != null) {
        getIntent().putExtra("prompt_text_to_load", promptText);
    }
    
    // Check if we're already at OpenAI settings fragment
    if (navController.getCurrentDestination() != null && 
        navController.getCurrentDestination().getId() == R.id.openAISpeechSettingsFragment) {
      android.util.Log.d("MainSettingsActivity", "Already at OpenAI settings, showing prompt dialog");
      // We're already at OpenAI settings, just trigger the prompt dialog
      OpenAISpeechSettingsFragment currentFragment = (OpenAISpeechSettingsFragment) 
          ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment))
              .getChildFragmentManager().getFragments().get(0);
      if (currentFragment != null) {
        if (promptText != null) {
            currentFragment.updatePromptPreference(promptText);
        }
        currentFragment.showPromptDialog();
      }
    } else {
      android.util.Log.d("MainSettingsActivity", "Navigating to OpenAI settings through navigation flow");
      android.util.Log.d("MainSettingsActivity", "Current destination: " + (navController.getCurrentDestination() != null ? navController.getCurrentDestination().getId() : "null"));
      
      // Navigate: Language Settings -> Additional Language Settings -> OpenAI Settings
      // First, select Language Settings tab in bottom navigation
      BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
      if (bottomNavigationView != null) {
        android.util.Log.d("MainSettingsActivity", "Setting selected item to languageSettingsFragment");
        bottomNavigationView.setSelectedItemId(R.id.languageSettingsFragment);
        
// Then navigate with proper navigation flow using NavController.OnDestinationChangedListener
      new Handler().postDelayed(() -> {
        try {
            android.util.Log.d("MainSettingsActivity", "Navigating to additional language settings");
            navController.navigate(R.id.action_languageSettingsFragment_to_additionalLanguageSettingsFragment);
            
            // Add listener to wait for navigation to complete
            navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                @Override
                public void onDestinationChanged(@NonNull NavController controller, @NonNull NavDestination destination, @Nullable Bundle arguments) {
                    if (destination.getId() == R.id.additionalLanguageSettingsFragment) {
                        android.util.Log.d("MainSettingsActivity", "Reached additional language settings, navigating to OpenAI settings");
                        // Remove the listener to avoid multiple calls
                        controller.removeOnDestinationChangedListener(this);
                        
                        // Navigate to OpenAI settings after a short delay
                        new Handler().postDelayed(() -> {
                            try {
                                navController.navigate(R.id.action_additionalLanguageSettingsFragment_to_openAISpeechSettingsFragment);
                            } catch (Exception e) {
                                android.util.Log.e("MainSettingsActivity", "Error navigating to OpenAI settings", e);
                            }
                        }, 300);
                    }
                }
            });
          } catch (Exception e) {
            android.util.Log.e("MainSettingsActivity", "Error navigating to additional language settings", e);
          }
        }, 200);      } else {
        android.util.Log.e("MainSettingsActivity", "BottomNavigationView is null!");
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionRequestHelper.onRequestPermissionsResult(
        requestCode, permissions, grantResults, this);
  }

  @Override
  public void setTitle(CharSequence title) {
    mTitle = title;
    getSupportActionBar().setTitle(mTitle);
  }
}
