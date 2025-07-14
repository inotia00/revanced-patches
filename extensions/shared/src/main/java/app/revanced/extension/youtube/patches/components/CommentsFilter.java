package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;

@SuppressWarnings("unused")
public final class CommentsFilter extends Filter {
    private static final String COMMENT_COMPOSER_PATH = "comment_composer";
    private static final String COMMENT_ENTRY_POINT_TEASER_PATH = "comments_entry_point_teaser";
    private static final Pattern COMMENT_PREVIEW_TEXT_PATTERN = Pattern.compile("comments_entry_point_teaser.+ContainerType");
    private static final String FEED_VIDEO_PATH = "video_lockup_with_attachment";
    private static final String VIDEO_METADATA_CAROUSEL_PATH = "video_metadata_carousel.eml";

    private final StringFilterGroup chipBar;
    private final ByteArrayFilterGroup aiCommentsSummary;
    private final StringFilterGroup comments;
    private final StringFilterGroup commentsPreviewDots;
    private final StringFilterGroup createAShort;
    private final StringFilterGroup emojiPickerAndTimestamp;
    private final StringFilterGroup previewCommentText;
    private final StringFilterGroup thanks;
    private final StringTrieSearch exceptions = new StringTrieSearch();

    public CommentsFilter() {
        exceptions.addPatterns("macro_markers_list_item");

        final StringFilterGroup aiChatSummary = new StringFilterGroup(
                Settings.HIDE_AI_CHAT_SUMMARY,
                "live_chat_summary_banner"
        );

        chipBar = new StringFilterGroup(
                Settings.HIDE_AI_COMMENTS_SUMMARY,
                "chip_bar.eml"
        );

        aiCommentsSummary = new ByteArrayFilterGroup(
                null,
                "yt_fill_spark"
        );

        final StringFilterGroup channelGuidelines = new StringFilterGroup(
                Settings.HIDE_CHANNEL_GUIDELINES,
                "channel_guidelines_entry_banner",
                "community_guidelines",
                "sponsorships_comments_upsell"
        );

        comments = new StringFilterGroup(
                null,
                VIDEO_METADATA_CAROUSEL_PATH,
                "comments_"
        );

        final StringFilterGroup commentsByMembers = new StringFilterGroup(
                Settings.HIDE_COMMENTS_BY_MEMBERS,
                "sponsorships_comments_header.eml",
                "sponsorships_comments_footer.eml"
        );

        createAShort = new StringFilterGroup(
                Settings.HIDE_COMMENTS_CREATE_A_SHORT_BUTTON,
                "composer_short_creation_button"
        );

        emojiPickerAndTimestamp = new StringFilterGroup(
                Settings.HIDE_COMMENTS_EMOJI_AND_TIMESTAMP_BUTTONS,
                "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|"
        );

        final StringFilterGroup liveChatMessages = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_MESSAGES,
                "live_chat_text_message",
                "viewer_engagement_message" // message about poll, not poll itself
        );

        final StringFilterGroup previewComment = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD,
                "|carousel_item.",
                "|carousel_listener",
                COMMENT_ENTRY_POINT_TEASER_PATH,
                "comments_entry_point_simplebox"
        );

        commentsPreviewDots = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD,
                "|ContainerType|ContainerType|ContainerType|"
        );

        previewCommentText = new StringFilterGroup(
                Settings.HIDE_PREVIEW_COMMENT_NEW_METHOD,
                COMMENT_ENTRY_POINT_TEASER_PATH
        );

        thanks = new StringFilterGroup(
                Settings.HIDE_COMMENTS_THANKS_BUTTON,
                "|super_thanks_button.eml"
        );


        addIdentifierCallbacks(channelGuidelines);

        addPathCallbacks(
                aiChatSummary,
                chipBar,
                comments,
                commentsByMembers,
                commentsPreviewDots,
                createAShort,
                emojiPickerAndTimestamp,
                liveChatMessages,
                previewComment,
                previewCommentText,
                thanks
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (exceptions.matches(path))
            return false;

        if (matchedGroup == createAShort || matchedGroup == thanks || matchedGroup == emojiPickerAndTimestamp) {
            if (path.startsWith(COMMENT_COMPOSER_PATH)) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == chipBar) {
            // Playlist sort button uses same components and must only filter if the player is opened.
            return PlayerType.getCurrent().isMaximizedOrFullscreen()
                    && aiCommentsSummary.check(protobufBufferArray).isFiltered();
        } else if (matchedGroup == comments) {
            if (path.startsWith(FEED_VIDEO_PATH)) {
                if (Settings.HIDE_COMMENTS_SECTION_IN_HOME_FEED.get()) {
                    return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
                }
                return false;
            } else if (Settings.HIDE_COMMENTS_SECTION.get()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == commentsPreviewDots) {
            if (path.startsWith(VIDEO_METADATA_CAROUSEL_PATH)) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == previewCommentText) {
            if (COMMENT_PREVIEW_TEXT_PATTERN.matcher(path).find()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
