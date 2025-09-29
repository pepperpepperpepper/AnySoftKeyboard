package com.anysoftkeyboard.quicktextkeys.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.anysoftkeyboard.ime.InputViewActionsProvider;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.quicktextkeys.EmojiSearchManager;
import com.anysoftkeyboard.quicktextkeys.HistoryQuickTextKey;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.quicktextkeys.SearchableQuickTextKey;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.astuetz.PagerSlidingTabStrip;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.evendanan.pixel.ViewPagerWithDisable;

public class QuickTextPagerView extends LinearLayout implements InputViewActionsProvider, EmojiSearchManager.SearchListener {

  private KeyboardTheme mKeyboardTheme;
  private float mTabTitleTextSize;
  private ColorStateList mTabTitleTextColor;
  private Drawable mCloseKeyboardIcon;
  private Drawable mBackspaceIcon;
  private Drawable mSettingsIcon;
  private Drawable mMediaInsertionDrawable;
  private Drawable mDeleteRecentlyUsedDrawable;
  private int mBottomPadding;
  private QuickKeyHistoryRecords mQuickKeyHistoryRecords;
  private DefaultSkinTonePrefTracker mDefaultSkinTonePrefTracker;
  private DefaultGenderPrefTracker mDefaultGenderPrefTracker;
  
  // Search-related fields
  private EmojiSearchManager mEmojiSearchManager;
  private List<QuickTextKey> mOriginalQuickTextKeys;
  private QuickKeysKeyboardPagerAdapter mAdapter;
  private ViewPagerWithDisable mPager;
  private View mSearchIndicator;
  private LinearLayout mSearchContainer;
  private EditText mSearchInput;
  private ImageView mSearchBackspaceButton;
  private ImageView mSearchClearButton;
  private ImageView mSearchDoneButton;

  public QuickTextPagerView(Context context) {
    super(context);
    initSearchManager();
  }

  public QuickTextPagerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initSearchManager();
  }

  public QuickTextPagerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initSearchManager();
  }
  
  private void initSearchManager() {
    mEmojiSearchManager = new EmojiSearchManager(getContext());
    mEmojiSearchManager.setSearchListener(this);
  }

  private static void setupSlidingTab(
      View rootView,
      float tabTitleTextSize,
      ColorStateList tabTitleTextColor,
      ViewPager pager,
      PagerAdapter adapter,
      ViewPager.OnPageChangeListener onPageChangeListener,
      int startIndex) {
    PagerSlidingTabStrip pagerTabStrip = rootView.findViewById(R.id.pager_tabs);
    pagerTabStrip.setTextSize((int) tabTitleTextSize);
    pagerTabStrip.setTextColor(tabTitleTextColor.getDefaultColor());
    pagerTabStrip.setIndicatorColor(tabTitleTextColor.getDefaultColor());
    pager.setAdapter(adapter);
    pager.setCurrentItem(startIndex);
    pagerTabStrip.setViewPager(pager);
    pagerTabStrip.setOnPageChangeListener(onPageChangeListener);
  }

  public void setThemeValues(
      @NonNull KeyboardTheme keyboardTheme,
      float tabTextSize,
      ColorStateList tabTextColor,
      Drawable closeKeyboardIcon,
      Drawable backspaceIcon,
      Drawable settingsIcon,
      Drawable keyboardDrawable,
      Drawable mediaInsertionDrawable,
      Drawable deleteRecentlyUsedDrawable,
      int bottomPadding,
      Set<MediaType> supportedMediaTypes) {
    mKeyboardTheme = keyboardTheme;
    mTabTitleTextSize = tabTextSize;
    mTabTitleTextColor = tabTextColor;
    mCloseKeyboardIcon = closeKeyboardIcon;
    mBackspaceIcon = backspaceIcon;
    mSettingsIcon = settingsIcon;
    mMediaInsertionDrawable = mediaInsertionDrawable;
    mDeleteRecentlyUsedDrawable = deleteRecentlyUsedDrawable;
    mBottomPadding = bottomPadding;
    findViewById(R.id.quick_keys_popup_quick_keys_insert_media)
        .setVisibility(supportedMediaTypes.isEmpty() ? View.GONE : VISIBLE);
    setBackground(keyboardDrawable);
  }

  @Override
  public void setOnKeyboardActionListener(OnKeyboardActionListener keyboardActionListener) {
    FrameKeyboardViewClickListener frameKeyboardViewClickListener =
        new FrameKeyboardViewClickListener(keyboardActionListener);
    frameKeyboardViewClickListener.registerOnViews(this);

    final Context context = getContext();
    mOriginalQuickTextKeys = new ArrayList<>();
    // always starting with Recent
    final HistoryQuickTextKey historyQuickTextKey =
        new HistoryQuickTextKey(context, mQuickKeyHistoryRecords);
    mOriginalQuickTextKeys.add(historyQuickTextKey);
    // then all the rest
    mOriginalQuickTextKeys.addAll(AnyApplication.getQuickTextKeyFactory(context).getEnabledAddOns());

    final QuickTextUserPrefs quickTextUserPrefs = new QuickTextUserPrefs(context);

    mPager = findViewById(R.id.quick_text_keyboards_pager);
    mAdapter =
        new QuickKeysKeyboardPagerAdapter(
            context,
            mPager,
            getCurrentFilteredKeys(),
            new RecordHistoryKeyboardActionListener(historyQuickTextKey, keyboardActionListener),
            mDefaultSkinTonePrefTracker,
            mDefaultGenderPrefTracker,
            mKeyboardTheme,
            mBottomPadding);

    final ImageView clearEmojiHistoryIcon =
        findViewById(R.id.quick_keys_popup_delete_recently_used_smileys);
    ViewPager.SimpleOnPageChangeListener onPageChangeListener =
        new ViewPager.SimpleOnPageChangeListener() {
          @Override
          public void onPageSelected(int position) {
            super.onPageSelected(position);
            List<QuickTextKey> currentKeys = getCurrentFilteredKeys();
            if (position < currentKeys.size()) {
              QuickTextKey selectedKey = currentKeys.get(position);
              quickTextUserPrefs.setLastSelectedAddOnId(selectedKey.getId());
              // if this is History, we need to show clear icon
              // else, hide the clear icon
              clearEmojiHistoryIcon.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }
          }
        };
    int startPageIndex = quickTextUserPrefs.getStartPageIndex(getCurrentFilteredKeys());
    setupSlidingTab(
        this,
        mTabTitleTextSize,
        mTabTitleTextColor,
        mPager,
        mAdapter,
        onPageChangeListener,
        startPageIndex);

    // setting up icons from theme
    ((ImageView) findViewById(R.id.quick_keys_popup_close)).setImageDrawable(mCloseKeyboardIcon);
    ((ImageView) findViewById(R.id.quick_keys_popup_backspace)).setImageDrawable(mBackspaceIcon);
    ((ImageView) findViewById(R.id.quick_keys_popup_quick_keys_insert_media))
        .setImageDrawable(mMediaInsertionDrawable);
    clearEmojiHistoryIcon.setImageDrawable(mDeleteRecentlyUsedDrawable);
    ((ImageView) findViewById(R.id.quick_keys_popup_quick_keys_settings))
        .setImageDrawable(mSettingsIcon);
    
    // Setup search button
    ImageView searchButton = findViewById(R.id.quick_keys_popup_search);
    if (searchButton != null) {
      searchButton.setOnClickListener(v -> {
        if (isEmojiSearchActive()) {
          endEmojiSearch();
        } else {
          startEmojiSearch();
        }
      });
    }
    
    final View actionsLayout = findViewById(R.id.quick_text_actions_layout);
    actionsLayout.setPadding(
        actionsLayout.getPaddingLeft(),
        actionsLayout.getPaddingTop(),
        actionsLayout.getPaddingRight(),
        // this will support the case were we have navigation-bar offset
        actionsLayout.getPaddingBottom() + mBottomPadding);
    
    // Initialize search UI elements
    setupSearchUI();
  }

  public void setQuickKeyHistoryRecords(QuickKeyHistoryRecords quickKeyHistoryRecords) {
    mQuickKeyHistoryRecords = quickKeyHistoryRecords;
  }

  public void setEmojiVariantsPrefTrackers(
      DefaultSkinTonePrefTracker defaultSkinTonePrefTracker,
      DefaultGenderPrefTracker defaultGenderPrefTracker) {
    mDefaultSkinTonePrefTracker = defaultSkinTonePrefTracker;
    mDefaultGenderPrefTracker = defaultGenderPrefTracker;
  }
  
  // Search-related methods
  
  private void setupSearchUI() {
    // Initialize search UI elements
    mSearchIndicator = findViewById(R.id.quick_keys_search_indicator);
    mSearchContainer = findViewById(R.id.quick_keys_search_container);
    mSearchInput = findViewById(R.id.quick_keys_search_input);
    mSearchBackspaceButton = findViewById(R.id.quick_keys_search_backspace);
    mSearchClearButton = findViewById(R.id.quick_keys_search_clear);
    mSearchDoneButton = findViewById(R.id.quick_keys_search_done);
    
    if (mSearchInput != null) {
      // Add text watcher for real-time search
      mSearchInput.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        
        @Override
        public void afterTextChanged(Editable s) {
          String query = s.toString();
          mEmojiSearchManager.updateSearchQuery(query);
        }
      });
      
      // Handle done action
      mSearchInput.setOnEditorActionListener((v, actionId, event) -> {
        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
          endEmojiSearch();
          return true;
        }
        return false;
      });
    }
    
    // Setup search action buttons
    if (mSearchBackspaceButton != null) {
      mSearchBackspaceButton.setOnClickListener(v -> {
        if (mSearchInput != null && mSearchInput.getText().length() > 0) {
          Editable text = mSearchInput.getText();
          text.delete(text.length() - 1, text.length());
        }
      });
    }
    
    if (mSearchClearButton != null) {
      mSearchClearButton.setOnClickListener(v -> {
        if (mSearchInput != null) {
          mSearchInput.setText("");
        }
      });
    }
    
    if (mSearchDoneButton != null) {
      mSearchDoneButton.setOnClickListener(v -> {
        endEmojiSearch();
      });
    }
    
    updateSearchUI();
  }
  
  private void updateSearchUI() {
    boolean searchActive = mEmojiSearchManager.isSearchActive();
    
    if (mSearchIndicator != null) {
      mSearchIndicator.setVisibility(searchActive ? View.VISIBLE : View.GONE);
    }
    
    if (mSearchContainer != null) {
      mSearchContainer.setVisibility(searchActive ? View.VISIBLE : View.GONE);
      if (searchActive && mSearchInput != null) {
        mSearchInput.requestFocus();
        // Show soft keyboard for the EditText
        mSearchInput.post(() -> {
          android.view.inputmethod.InputMethodManager imm = 
              (android.view.inputmethod.InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
          if (imm != null) {
            imm.showSoftInput(mSearchInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
          }
        });
      }
    }
  }
  
  private List<QuickTextKey> getCurrentFilteredKeys() {
    if (mEmojiSearchManager.isSearchActive()) {
      return createSearchableKeys(mEmojiSearchManager.filterEmojiKeys(mOriginalQuickTextKeys));
    } else {
      return mOriginalQuickTextKeys;
    }
  }
  
  private List<QuickTextKey> createSearchableKeys(List<QuickTextKey> filteredKeys) {
    List<QuickTextKey> searchableKeys = new ArrayList<>();
    String query = mEmojiSearchManager.getCurrentQuery();
    
    for (QuickTextKey key : filteredKeys) {
      if (key instanceof HistoryQuickTextKey) {
        // Don't wrap the history key
        searchableKeys.add(key);
      } else {
        searchableKeys.add(new SearchableQuickTextKey(key, query));
      }
    }
    
    return searchableKeys;
  }
  
  private void refreshAdapter() {
    if (mAdapter != null && mPager != null) {
      List<QuickTextKey> filteredKeys = getCurrentFilteredKeys();
      
      // Create a new adapter with the filtered keys
      QuickKeysKeyboardPagerAdapter newAdapter =
          new QuickKeysKeyboardPagerAdapter(
              getContext(),
              mPager,
              filteredKeys,
              mAdapter.getKeyboardActionListener(),
              mDefaultSkinTonePrefTracker,
              mDefaultGenderPrefTracker,
              mKeyboardTheme,
              mBottomPadding);
      
      // Update the adapter
      mAdapter = newAdapter;
      mPager.setAdapter(mAdapter);
      
      // Update the tab strip
      PagerSlidingTabStrip pagerTabStrip = findViewById(R.id.pager_tabs);
      if (pagerTabStrip != null) {
        pagerTabStrip.setViewPager(mPager);
      }
    }
  }
  
  // Public methods for search integration
  
  public void updateSearchQuery(@NonNull String query) {
    mEmojiSearchManager.updateSearchQuery(query);
    // Update search input field if it exists
    if (mSearchInput != null && !mSearchInput.getText().toString().equals(query)) {
      mSearchInput.setText(query);
    }
  }
  
  public void startEmojiSearch() {
    mEmojiSearchManager.startSearch();
  }
  
  public void endEmojiSearch() {
    mEmojiSearchManager.endSearch();
    // Hide soft keyboard when ending search
    if (mSearchInput != null) {
      android.view.inputmethod.InputMethodManager imm = 
          (android.view.inputmethod.InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.hideSoftInputFromWindow(mSearchInput.getWindowToken(), 0);
      }
    }
  }
  
  public boolean isEmojiSearchActive() {
    return mEmojiSearchManager.isSearchActive();
  }
  
  // EmojiSearchManager.SearchListener implementation
  
  @Override
  public void onSearchResultsChanged(@NonNull String query, @NonNull List<QuickTextKey> results) {
    // Refresh the adapter with filtered results
    refreshAdapter();
    updateSearchUI();
  }
  
  @Override
  public void onSearchStateChanged(boolean isActive) {
    updateSearchUI();
    if (!isActive) {
      // Search ended, refresh to show all keys
      refreshAdapter();
    }
  }
}
