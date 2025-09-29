package com.anysoftkeyboard.ime;

import android.text.TextUtils;
import android.view.View;
import com.anysoftkeyboard.api.KeyCodes;
import com.anysoftkeyboard.keyboards.Keyboard;
import com.anysoftkeyboard.keyboards.views.AnyKeyboardView;
import com.anysoftkeyboard.keyboards.views.KeyboardViewContainerView;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.quicktextkeys.ui.DefaultGenderPrefTracker;
import com.anysoftkeyboard.quicktextkeys.ui.DefaultSkinTonePrefTracker;
import com.anysoftkeyboard.quicktextkeys.ui.QuickTextPagerView;
import com.anysoftkeyboard.quicktextkeys.ui.QuickTextViewFactory;
import com.anysoftkeyboard.rx.GenericOnError;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;

public abstract class AnySoftKeyboardWithQuickText extends AnySoftKeyboardMediaInsertion {

  private boolean mDoNotFlipQuickTextKeyAndPopupFunctionality;
  private String mOverrideQuickTextText = "";
  private DefaultSkinTonePrefTracker mDefaultSkinTonePrefTracker;
  private DefaultGenderPrefTracker mDefaultGenderPrefTracker;
  private QuickTextPagerView mQuickTextPagerView;
  private StringBuilder mSearchQueryBuilder = new StringBuilder();

  @Override
  public void onCreate() {
    super.onCreate();
    addDisposable(
        prefs()
            .getBoolean(
                R.string.settings_key_do_not_flip_quick_key_codes_functionality,
                R.bool.settings_default_do_not_flip_quick_keys_functionality)
            .asObservable()
            .subscribe(
                value -> mDoNotFlipQuickTextKeyAndPopupFunctionality = value,
                GenericOnError.onError("settings_key_do_not_flip_quick_key_codes_functionality")));

    addDisposable(
        prefs()
            .getString(R.string.settings_key_emoticon_default_text, R.string.settings_default_empty)
            .asObservable()
            .subscribe(
                value -> mOverrideQuickTextText = value,
                GenericOnError.onError("settings_key_emoticon_default_text")));

    mDefaultSkinTonePrefTracker = new DefaultSkinTonePrefTracker(prefs());
    addDisposable(mDefaultSkinTonePrefTracker);
    mDefaultGenderPrefTracker = new DefaultGenderPrefTracker(prefs());
    addDisposable(mDefaultGenderPrefTracker);
  }

  protected void onQuickTextRequested(Keyboard.Key key) {
    if (mDoNotFlipQuickTextKeyAndPopupFunctionality) {
      outputCurrentQuickTextKey(key);
    } else {
      switchToQuickTextKeyboard();
    }
  }

  protected void onQuickTextKeyboardRequested(Keyboard.Key key) {
    if (mDoNotFlipQuickTextKeyAndPopupFunctionality) {
      switchToQuickTextKeyboard();
    } else {
      outputCurrentQuickTextKey(key);
    }
  }

  private void outputCurrentQuickTextKey(Keyboard.Key key) {
    QuickTextKey quickTextKey = AnyApplication.getQuickTextKeyFactory(this).getEnabledAddOn();
    if (TextUtils.isEmpty(mOverrideQuickTextText)) {
      final CharSequence keyOutputText = quickTextKey.getKeyOutputText();
      onText(key, keyOutputText);
    } else {
      onText(key, mOverrideQuickTextText);
    }
  }

  @Override
  public void onFinishInputView(boolean finishingInput) {
    super.onFinishInputView(finishingInput);
    cleanUpQuickTextKeyboard(true);
  }

  private void switchToQuickTextKeyboard() {
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    abortCorrectionAndResetPredictionState(false);

    cleanUpQuickTextKeyboard(false);

    final AnyKeyboardView actualInputView = (AnyKeyboardView) getInputView();
    mQuickTextPagerView =
        QuickTextViewFactory.createQuickTextView(
            getApplicationContext(),
            inputViewContainer,
            getQuickKeyHistoryRecords(),
            mDefaultSkinTonePrefTracker,
            mDefaultGenderPrefTracker);
    actualInputView.resetInputView();
    mQuickTextPagerView.setThemeValues(
        mCurrentTheme,
        actualInputView.getLabelTextSize(),
        actualInputView.getCurrentResourcesHolder().getKeyTextColor(),
        actualInputView.getDrawableForKeyCode(KeyCodes.CANCEL),
        actualInputView.getDrawableForKeyCode(KeyCodes.DELETE),
        actualInputView.getDrawableForKeyCode(KeyCodes.SETTINGS),
        actualInputView.getBackground(),
        actualInputView.getDrawableForKeyCode(KeyCodes.IMAGE_MEDIA_POPUP),
        actualInputView.getDrawableForKeyCode(KeyCodes.CLEAR_QUICK_TEXT_HISTORY),
        actualInputView.getPaddingBottom(),
        getSupportedMediaTypesForInput());

    actualInputView.setVisibility(View.GONE);
    inputViewContainer.addView(mQuickTextPagerView);
  }

  private boolean cleanUpQuickTextKeyboard(boolean reshowStandardKeyboard) {
    final KeyboardViewContainerView inputViewContainer = getInputViewContainer();
    if (inputViewContainer == null) return false;

    if (reshowStandardKeyboard) {
      View standardKeyboardView = (View) getInputView();
      if (standardKeyboardView != null) {
        standardKeyboardView.setVisibility(View.VISIBLE);
      }
    }

    QuickTextPagerView quickTextsLayout =
        inputViewContainer.findViewById(R.id.quick_text_pager_root);
    if (quickTextsLayout != null) {
      inputViewContainer.removeView(quickTextsLayout);
      if (quickTextsLayout == mQuickTextPagerView) {
        mQuickTextPagerView = null;
        mSearchQueryBuilder.setLength(0);
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean handleCloseRequest() {
    return super.handleCloseRequest() || cleanUpQuickTextKeyboard(true);
  }
  
  // Emoji search integration methods
  
  protected void onEmojiSearchCharacter(int primaryCode) {
    if (mQuickTextPagerView != null && mQuickTextPagerView.isEmojiSearchActive()) {
      if (primaryCode == KeyCodes.DELETE) {
        // Handle backspace in search mode
        if (mSearchQueryBuilder.length() > 0) {
          mSearchQueryBuilder.deleteCharAt(mSearchQueryBuilder.length() - 1);
          updateEmojiSearch();
        } else {
          // No more characters to delete, end search
          endEmojiSearch();
        }
      } else if (primaryCode == KeyCodes.CANCEL || primaryCode == KeyCodes.ESCAPE) {
        // Cancel search mode
        endEmojiSearch();
      } else if (primaryCode >= 32 && primaryCode < 127) {
        // Add printable character to search query
        mSearchQueryBuilder.append((char) primaryCode);
        updateEmojiSearch();
      }
    }
  }
  
  protected void startEmojiSearch() {
    if (mQuickTextPagerView != null) {
      mSearchQueryBuilder.setLength(0);
      mQuickTextPagerView.startEmojiSearch();
    }
  }
  
  protected void endEmojiSearch() {
    if (mQuickTextPagerView != null) {
      mSearchQueryBuilder.setLength(0);
      mQuickTextPagerView.endEmojiSearch();
    }
  }
  
  protected void updateEmojiSearch() {
    if (mQuickTextPagerView != null) {
      String query = mSearchQueryBuilder.toString();
      mQuickTextPagerView.updateSearchQuery(query);
    }
  }
  
  protected boolean isEmojiSearchActive() {
    return mQuickTextPagerView != null && mQuickTextPagerView.isEmojiSearchActive();
  }
  
  @Override
  protected void handleCharacter(int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes) {
    // Check if we're in emoji search mode
    if (isEmojiSearchActive()) {
      onEmojiSearchCharacter(primaryCode);
      return;
    }
    
    // Normal character handling
    super.handleCharacter(primaryCode, key, multiTapIndex, nearByKeyCodes);
  }
  
  @Override
  protected void handleSeparator(int primaryCode) {
    // Check if we're in emoji search mode
    if (isEmojiSearchActive()) {
      if (primaryCode == KeyCodes.SPACE) {
        // Space could end search or be part of search, let's end search for now
        endEmojiSearch();
        return;
      } else if (primaryCode == KeyCodes.ENTER) {
        // Enter ends search
        endEmojiSearch();
        return;
      }
      onEmojiSearchCharacter(primaryCode);
      return;
    }
    
    // Normal separator handling
    super.handleSeparator(primaryCode);
  }
  
  @Override
  public void onKey(
      int primaryCode, Keyboard.Key key, int multiTapIndex, int[] nearByKeyCodes, boolean fromUI) {
    if (primaryCode == KeyCodes.EMOJI_SEARCH) {
      if (isEmojiSearchActive()) {
        endEmojiSearch();
      } else {
        startEmojiSearch();
      }
    } else {
      super.onKey(primaryCode, key, multiTapIndex, nearByKeyCodes, fromUI);
    }
  }
}
