package com.anysoftkeyboard.quicktextkeys;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages emoji search functionality for QWERTY-based emoji search.
 * This class handles the search logic, maintains search state, and filters
 * emoji categories based on the search query.
 */
public class EmojiSearchManager {
    
    public interface SearchListener {
        void onSearchResultsChanged(@NonNull String query, @NonNull List<QuickTextKey> results);
        void onSearchStateChanged(boolean isActive);
    }
    
    @NonNull private final Context mContext;
    @Nullable private SearchListener mSearchListener;
    @NonNull private String mCurrentQuery = "";
    private boolean mSearchActive = false;
    
    public EmojiSearchManager(@NonNull Context context) {
        mContext = context;
    }
    
    public void setSearchListener(@Nullable SearchListener listener) {
        mSearchListener = listener;
    }
    
    public void startSearch() {
        if (!mSearchActive) {
            mSearchActive = true;
            notifySearchStateChanged();
        }
    }
    
    public void endSearch() {
        if (mSearchActive) {
            mSearchActive = false;
            mCurrentQuery = "";
            notifySearchStateChanged();
            notifySearchResultsChanged();
        }
    }
    
    public void updateSearchQuery(@NonNull String query) {
        if (!mSearchActive) {
            startSearch();
        }
        
        if (!mCurrentQuery.equals(query)) {
            mCurrentQuery = query.toLowerCase(Locale.ROOT).trim();
            notifySearchResultsChanged();
        }
    }
    
    public void clearSearch() {
        if (mSearchActive) {
            mCurrentQuery = "";
            notifySearchResultsChanged();
        }
    }
    
    @NonNull
    public List<QuickTextKey> filterEmojiKeys(@NonNull List<QuickTextKey> originalKeys) {
        if (!mSearchActive || mCurrentQuery.isEmpty()) {
            return originalKeys;
        }
        
        List<QuickTextKey> filteredKeys = new ArrayList<>();
        
        // Always include the history key (first item) if it exists
        if (!originalKeys.isEmpty() && originalKeys.get(0) instanceof HistoryQuickTextKey) {
            filteredKeys.add(originalKeys.get(0));
        }
        
        // Filter other keys based on search query
        for (int i = (filteredKeys.isEmpty() ? 0 : 1); i < originalKeys.size(); i++) {
            QuickTextKey key = originalKeys.get(i);
            if (matchesSearchQuery(key)) {
                filteredKeys.add(key);
            }
        }
        
        return filteredKeys;
    }
    
    private boolean matchesSearchQuery(@NonNull QuickTextKey key) {
        if (mCurrentQuery.isEmpty()) {
            return true;
        }
        
        // Check key name
        String keyName = key.getName().toString().toLowerCase(Locale.ROOT);
        if (keyName.contains(mCurrentQuery)) {
            return true;
        }
        
        // Check key output text (emoji)
        CharSequence keyOutput = key.getKeyOutputText();
        if (keyOutput != null) {
            String outputText = keyOutput.toString().toLowerCase(Locale.ROOT);
            if (outputText.contains(mCurrentQuery)) {
                return true;
            }
        }
        
        // Check popup list names and values
        List<String> popupNames = key.getPopupListNames();
        List<String> popupValues = key.getPopupListValues();
        
        for (int i = 0; i < popupNames.size(); i++) {
            String name = popupNames.get(i).toLowerCase(Locale.ROOT);
            String value = popupValues.get(i).toLowerCase(Locale.ROOT);
            
            if (name.contains(mCurrentQuery) || value.contains(mCurrentQuery)) {
                return true;
            }
        }
        
        return false;
    }
    
    public boolean isSearchActive() {
        return mSearchActive;
    }
    
    @NonNull
    public String getCurrentQuery() {
        return mCurrentQuery;
    }
    
    private void notifySearchStateChanged() {
        if (mSearchListener != null) {
            mSearchListener.onSearchStateChanged(mSearchActive);
        }
    }
    
    private void notifySearchResultsChanged() {
        if (mSearchListener != null) {
            // We'll let the listener handle the actual filtering with the original keys
            // This is just a notification that the query has changed
            mSearchListener.onSearchResultsChanged(mCurrentQuery, new ArrayList<>());
        }
    }
}