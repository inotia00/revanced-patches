package app.revanced.extension.youtube.patches.spans;

import android.text.SpannableString;

import app.revanced.extension.shared.patches.spans.Filter;
import app.revanced.extension.shared.patches.spans.SpanType;
import app.revanced.extension.shared.patches.spans.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "ConstantValue", "FieldCanBeLocal"})
public final class SearchLinksFilter extends Filter {
    /**
     * Located in front of the search icon.
     */
    private final String WORD_JOINER_CHARACTER = "\u2060";

    public SearchLinksFilter() {
        addCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_COMMENTS_HIGHLIGHTED_SEARCH_LINKS,
                        "|comment."
                )
        );
    }

    /**
     * @return Whether the word contains a search icon or not.
     */
    private boolean isSearchLinks(SpannableString original, int end) {
        String originalString = original.toString();
        int wordJoinerIndex = originalString.indexOf(WORD_JOINER_CHARACTER);
        // There may be more than one highlight keyword in the comments.
        // Check the index of all highlight keywords.
        while (wordJoinerIndex != -1) {
            if (end - wordJoinerIndex == 2) return true;
            wordJoinerIndex = originalString.indexOf(WORD_JOINER_CHARACTER, wordJoinerIndex + 1);
        }
        return false;
    }

    @Override
    public boolean skip(String conversionContext, SpannableString spannableString, Object span,
                        int start, int end, int flags, boolean isWord, SpanType spanType, StringFilterGroup matchedGroup) {
        if (isSearchLinks(spannableString, end)) {
            if (spanType == SpanType.IMAGE) {
                hideSpan(spannableString, start, end, flags);
            }
            return super.skip(conversionContext, spannableString, span, start, end, flags, isWord, spanType, matchedGroup);
        }
        return false;
    }
}
