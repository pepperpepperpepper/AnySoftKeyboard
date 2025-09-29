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
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.anysoftkeyboard.ime.InputViewActionsProvider;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.quicktextkeys.HistoryQuickTextKey;
import com.anysoftkeyboard.quicktextkeys.QuickKeyHistoryRecords;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.remote.MediaType;
import com.anysoftkeyboard.theme.KeyboardTheme;
import com.astuetz.PagerSlidingTabStrip;
import com.menny.android.anysoftkeyboard.AnyApplication;
import com.menny.android.anysoftkeyboard.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.evendanan.pixel.ViewPagerWithDisable;

public class QuickTextPagerView extends LinearLayout implements InputViewActionsProvider {

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
  private List<QuickTextKey> mOriginalQuickTextKeys;
  private QuickKeysKeyboardPagerAdapter mAdapter;
  private ViewPagerWithDisable mPager;

  public QuickTextPagerView(Context context) {
    super(context);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public QuickTextPagerView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
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
            mOriginalQuickTextKeys,
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
            if (position < mOriginalQuickTextKeys.size()) {
              QuickTextKey selectedKey = mOriginalQuickTextKeys.get(position);
              quickTextUserPrefs.setLastSelectedAddOnId(selectedKey.getId());
              // if this is History, we need to show clear icon
              // else, hide the clear icon
              clearEmojiHistoryIcon.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }
          }
        };
    int startPageIndex = quickTextUserPrefs.getStartPageIndex(mOriginalQuickTextKeys);
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
    
    final View actionsLayout = findViewById(R.id.quick_text_actions_layout);
    actionsLayout.setPadding(
        actionsLayout.getPaddingLeft(),
        actionsLayout.getPaddingTop(),
        actionsLayout.getPaddingRight(),
        // this will support the case were we have navigation-bar offset
        actionsLayout.getPaddingBottom() + mBottomPadding);
  }

  public void setQuickKeyHistoryRecords(QuickKeyHistoryRecords quickKeyHistoryRecords) {
    mQuickKeyHistoryRecords = quickKeyHistoryRecords;
  }

  public void setEmojiVariantsPrefTrackers(
      DefaultSkinTonePrefTracker defaultSkinTonePrefTracker,
      DefaultGenderPrefTracker defaultGenderPrefTracker) {
    mDefaultSkinTonePrefTracker = defaultSkinTonePrefTracker;
    mDefaultGenderPrefTracker = defaultGenderPrefTracker;
}}
