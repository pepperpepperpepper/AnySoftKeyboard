package com.anysoftkeyboard.quicktextkeys;

import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper around QuickTextKey that provides search functionality.
 * This class filters the popup list items based on a search query
 * while maintaining the original QuickTextKey's functionality.
 */
public class SearchableQuickTextKey extends QuickTextKey {
    
    @NonNull private final QuickTextKey mOriginalKey;
    @NonNull private final String mSearchQuery;
    @NonNull private List<String> mFilteredNames;
    @NonNull private List<String> mFilteredValues;
    @Nullable private int[] mFilteredIconResIds;
    
public SearchableQuickTextKey(@NonNull QuickTextKey originalKey, @NonNull String searchQuery) {
        super(
            originalKey.getPackageContext(),
            originalKey.getPackageContext(),
            originalKey.getApiVersion(),
            originalKey.getId(),
            createSearchName(originalKey, searchQuery),
            originalKey.getPopupKeyboardResId(),
            0, // We'll handle popup list resources ourselves
            0, // We'll handle popup list resources ourselves
            0, // We'll handle popup list resources ourselves
            0, // keyIconResId - using default
            originalKey.getKeyLabel(),
            originalKey.getKeyOutputText(),
            0, // iconPreviewResId - using default
            false, // isHidden - using default
            originalKey.getDescription(),
            originalKey.getSortIndex());
        
        mOriginalKey = originalKey;
        mSearchQuery = searchQuery.toLowerCase();
        filterPopupItems();
    }
    
    private static CharSequence createSearchName(@NonNull QuickTextKey originalKey, @NonNull String searchQuery) {
        if (searchQuery.isEmpty()) {
            return originalKey.getName();
        }
        return originalKey.getName() + " (🔍)";
    }
    
    private void filterPopupItems() {
        List<String> originalNames = mOriginalKey.getPopupListNames();
        List<String> originalValues = mOriginalKey.getPopupListValues();
        // Note: QuickTextKey doesn't have getPopupListIconResIds() method, so we'll handle icons differently
        int[] originalIconResIds = null; // mOriginalKey.getPopupListIconResIds();
        
        mFilteredNames = new ArrayList<>();
        mFilteredValues = new ArrayList<>();
        
        if (mSearchQuery.isEmpty()) {
            // No search query, return all items
            mFilteredNames.addAll(originalNames);
            mFilteredValues.addAll(originalValues);
            mFilteredIconResIds = originalIconResIds;
        } else {
            // Filter items based on search query
            for (int i = 0; i < originalNames.size(); i++) {
                String name = originalNames.get(i).toLowerCase();
                String value = originalValues.get(i).toLowerCase();
                
                if (name.contains(mSearchQuery) || value.contains(mSearchQuery)) {
                    mFilteredNames.add(originalNames.get(i));
                    mFilteredValues.add(originalValues.get(i));
                }
            }
            
            // Create filtered icon array if needed
            if (originalIconResIds != null && !mFilteredNames.isEmpty()) {
                mFilteredIconResIds = new int[mFilteredNames.size()];
                int filteredIndex = 0;
                for (int i = 0; i < originalNames.size(); i++) {
                    String name = originalNames.get(i).toLowerCase();
                    String value = originalValues.get(i).toLowerCase();
                    
                    if (name.contains(mSearchQuery) || value.contains(mSearchQuery)) {
                        mFilteredIconResIds[filteredIndex++] = originalIconResIds[i];
                    }
                }
            } else {
                mFilteredIconResIds = null;
            }
        }
    }
    
    @Override
    protected String[] getStringArrayFromNamesResId(int popupListNamesResId, Resources resources) {
        return mFilteredNames.toArray(new String[0]);
    }
    
    @Override
    protected String[] getStringArrayFromValuesResId(int popupListValuesResId, Resources resources) {
        return mFilteredValues.toArray(new String[0]);
    }
    
    @Override
    public List<String> getPopupListNames() {
        return mFilteredNames;
    }
    
    @Override
    public List<String> getPopupListValues() {
        return mFilteredValues;
    }
    
    public int[] getPopupListIconResIds() {
        return mFilteredIconResIds;
    }
    
    @Override
    public boolean isPopupKeyboardUsed() {
        return mOriginalKey.isPopupKeyboardUsed();
    }
    
    @Override
    public int getPopupKeyboardResId() {
        return mOriginalKey.getPopupKeyboardResId();
    }
    
    @Override
    public CharSequence getKeyOutputText() {
        return mOriginalKey.getKeyOutputText();
    }
    
    @Nullable
    @Override
    public CharSequence getKeyLabel() {
        return mOriginalKey.getKeyLabel();
    }
    
    public boolean hasFilteredResults() {
        return !mFilteredNames.isEmpty();
    }
    
    public int getOriginalItemCount() {
        return mOriginalKey.getPopupListNames().size();
    }
    
    public int getFilteredItemCount() {
        return mFilteredNames.size();
    }
}