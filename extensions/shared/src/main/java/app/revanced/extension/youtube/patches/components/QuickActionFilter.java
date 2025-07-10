package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class QuickActionFilter extends Filter {
    private static final String QUICK_ACTION_PATH = "quick_actions.eml";
    private final StringFilterGroup quickActionRule;

    private final StringFilterGroup bufferFilterPathRule;
    private final ByteArrayFilterGroupList bufferButtonsGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup liveChatReplay;

    public QuickActionFilter() {
        quickActionRule = new StringFilterGroup(null, QUICK_ACTION_PATH);
        addIdentifierCallbacks(quickActionRule);
        bufferFilterPathRule = new StringFilterGroup(
                null,
                "|ContainerType|button.eml",
                "|fullscreen_video_action_button.eml"
        );

        liveChatReplay = new StringFilterGroup(
                Settings.HIDE_LIVE_CHAT_REPLAY_BUTTON,
                "live_chat_ep_entrypoint.eml"
        );

        addIdentifierCallbacks(liveChatReplay);

        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_LIKE_BUTTON,
                        "|like_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_DISLIKE_BUTTON,
                        "dislike_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                        "comments_entry_point_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_SAVE_TO_PLAYLIST_BUTTON,
                        "|save_to_playlist_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_MORE_BUTTON,
                        "|overflow_menu_button"
                ),
                new StringFilterGroup(
                        Settings.HIDE_RELATED_VIDEOS_OVERLAY,
                        "fullscreen_related_videos"
                ),
                bufferFilterPathRule
        );

        bufferButtonsGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_COMMENT_BUTTON,
                        "yt_outline_message_bubble_right"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_LIVE_CHAT_BUTTON,
                        "yt_outline_message_bubble_overlap"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_OPEN_MIX_PLAYLIST_BUTTON,
                        "yt_outline_youtube_mix"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_OPEN_PLAYLIST_BUTTON,
                        "yt_outline_list_play_arrow"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_QUICK_ACTIONS_SHARE_BUTTON,
                        "yt_outline_share"
                )
        );
    }

    private boolean isEveryFilterGroupEnabled() {
        for (StringFilterGroup group : pathCallbacks)
            if (!group.isEnabled()) return false;

        for (ByteArrayFilterGroup group : bufferButtonsGroupList)
            if (!group.isEnabled()) return false;

        return true;
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == liveChatReplay) {
            return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
        }
        if (!path.startsWith(QUICK_ACTION_PATH)) {
            return false;
        }
        if (matchedGroup == quickActionRule && !isEveryFilterGroupEnabled()) {
            return false;
        }
        if (matchedGroup == bufferFilterPathRule) {
            return bufferButtonsGroupList.check(protobufBufferArray).isFiltered();
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
