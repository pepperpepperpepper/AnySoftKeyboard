package com.anysoftkeyboard.emoji;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.anysoftkeyboard.keyboards.views.OnKeyboardActionListener;
import com.anysoftkeyboard.quicktextkeys.EmojiSearchManager;
import com.anysoftkeyboard.quicktextkeys.QuickTextKey;
import com.anysoftkeyboard.quicktextkeys.SearchableQuickTextKey;
import com.menny.android.anysoftkeyboard.R;
import java.util.List;

public class EmojiSearchOverlay implements EmojiSearchManager.SearchListener {

    private final OnKeyboardActionListener mKeyboard;
    private final Context mContext;
    private final View mOverlayView;
    private final TextView mSearchQueryText;
    private final LinearLayout mResultsContainer;
    private final EmojiSearchManager mSearchManager;
    private final StringBuilder mSearchQuery = new StringBuilder();

    public EmojiSearchOverlay(@NonNull Context context, @NonNull OnKeyboardActionListener keyboard, @NonNull View overlayView) {
        mContext = context;
        mKeyboard = keyboard;
        mOverlayView = overlayView;
        mSearchQueryText = overlayView.findViewById(R.id.emoji_search_query);
        mResultsContainer = overlayView.findViewById(R.id.emoji_search_results);
        
        // Initialize search manager
        mSearchManager = new EmojiSearchManager(mContext);
        mSearchManager.setSearchListener(this);
        
        // Setup close button
        ImageView closeButton = overlayView.findViewById(R.id.emoji_search_close);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> hideSearch());
        }
        
        // Initially hidden
        mOverlayView.setVisibility(View.GONE);
    }

    public void showSearch() {
        mOverlayView.setVisibility(View.VISIBLE);
        mSearchQuery.setLength(0);
        updateSearchDisplay();
        mSearchManager.startSearch();
    }

    public void hideSearch() {
        mOverlayView.setVisibility(View.GONE);
        mSearchQuery.setLength(0);
        mSearchManager.endSearch();
    }

    public boolean isVisible() {
        return mOverlayView.getVisibility() == View.VISIBLE;
    }

    public void handleCharacterInput(char character) {
        if (!isVisible()) return;
        
        mSearchQuery.append(character);
        updateSearchDisplay();
        mSearchManager.updateSearchQuery(mSearchQuery.toString());
    }

    public void handleBackspace() {
        if (!isVisible()) return;
        
        if (mSearchQuery.length() > 0) {
            mSearchQuery.deleteCharAt(mSearchQuery.length() - 1);
            updateSearchDisplay();
            mSearchManager.updateSearchQuery(mSearchQuery.toString());
        } else {
            // If query is empty, hide search
            hideSearch();
        }
    }

    private void updateSearchDisplay() {
        if (mSearchQueryText != null) {
            if (mSearchQuery.length() > 0) {
                mSearchQueryText.setText(mSearchQuery.toString());
                mSearchQueryText.setHint("");
            } else {
                mSearchQueryText.setText("");
                mSearchQueryText.setHint("Type to search emojis...");
            }
        }
    }

    @Override
    public void onSearchResultsChanged(@NonNull String query, @NonNull List<QuickTextKey> results) {
        updateSearchResults(results);
    }

    @Override
    public void onSearchStateChanged(boolean isActive) {
        if (!isActive) {
            hideSearch();
        }
    }

    private void updateSearchResults(List<QuickTextKey> results) {
        if (mResultsContainer == null) return;

        // Clear previous results
        mResultsContainer.removeAllViews();

        // Add emoji results
        for (QuickTextKey key : results) {
            if (key instanceof SearchableQuickTextKey) {
                SearchableQuickTextKey searchableKey = (SearchableQuickTextKey) key;
                addEmojiResult(searchableKey);
            }
        }

        // Scroll to beginning - handled by HorizontalScrollView in layout
    }

    private void addEmojiResult(SearchableQuickTextKey searchableKey) {
        TextView emojiView = new TextView(mContext);
        
        // Set emoji text
        CharSequence emojiText = searchableKey.getKeyOutputText();
        emojiView.setText(emojiText);
        emojiView.setTextSize(24f);
        emojiView.setPadding(8, 4, 8, 4);
        emojiView.setBackground(mContext.getDrawable(R.drawable.dark_background));
        
        // Set click listener to insert emoji
        emojiView.setOnClickListener(v -> {
            mKeyboard.onText(null, emojiText);
            hideSearch();
        });
        
        mResultsContainer.addView(emojiView);
    }
}