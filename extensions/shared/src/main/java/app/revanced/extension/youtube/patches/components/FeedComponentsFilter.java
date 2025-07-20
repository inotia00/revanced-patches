package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.patches.components.StringFilterGroupList;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;

@SuppressWarnings("unused")
public final class FeedComponentsFilter extends Filter {
    private static final String CONVERSATION_CONTEXT_FEED_IDENTIFIER =
            "horizontalCollectionSwipeProtector=null";
    private static final String CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER =
            "heightConstraint=null";
    private static final String INLINE_EXPANSION_PATH = "inline_expansion";
    private static final String FEED_VIDEO_PATH = "video_lockup_with_attachment";

    private static final ByteArrayFilterGroup mixPlaylists =
            new ByteArrayFilterGroup(
                    null,
                    "&list="
            );
    private static final ByteArrayFilterGroup mixPlaylistsBufferExceptions =
            new ByteArrayFilterGroup(
                    null,
                    "cell_description_body",
                    "channel_profile"
            );
    private static final StringTrieSearch mixPlaylistsContextExceptions = new StringTrieSearch();
    private static final StringTrieSearch communityPostsFeedGroupSearch = new StringTrieSearch();
    private final StringFilterGroup channelProfile;
    private final ByteArrayFilterGroupList channelProfileGroupList = new ByteArrayFilterGroupList();
    private final StringFilterGroup chipBar;
    private final StringFilterGroup communityPosts;
    private final StringFilterGroup expandableCard;
    private final StringFilterGroup horizontalShelves;
    private final ByteArrayFilterGroup ticketShelf;
    private final StringFilterGroupList communityPostsFeedGroup = new StringFilterGroupList();


    public FeedComponentsFilter() {
        communityPostsFeedGroupSearch.addPatterns(
                CONVERSATION_CONTEXT_FEED_IDENTIFIER,
                CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER
        );
        mixPlaylistsContextExceptions.addPatterns(
                "V.ED", // playlist browse id
                "java.lang.ref.WeakReference"
        );

        // Identifiers.

        final StringFilterGroup chipsShelf = new StringFilterGroup(
                Settings.HIDE_CHIPS_SHELF,
                "chips_shelf"
        );

        communityPosts = new StringFilterGroup(
                null,
                "post_base_wrapper",
                "images_post_root",
                "images_post_slim",
                "poll_post_root",
                "post_responsive_root",
                "post_shelf_slim",
                "text_post_root",
                "videos_post_root"
        );

        final StringFilterGroup expandableShelf = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_SHELF,
                "expandable_section"
        );

        final StringFilterGroup feedSearchBar = new StringFilterGroup(
                Settings.HIDE_FEED_SEARCH_BAR,
                "search_bar_entry_point"
        );

        final StringFilterGroup tasteBuilder = new StringFilterGroup(
                Settings.HIDE_SURVEYS,
                "selectable_item.eml",
                "cell_button.eml"
        );

        addIdentifierCallbacks(
                chipsShelf,
                communityPosts,
                expandableShelf,
                feedSearchBar,
                tasteBuilder
        );

        // Paths.

        final StringFilterGroup albumCard = new StringFilterGroup(
                Settings.HIDE_ALBUM_CARDS,
                "browsy_bar",
                "official_card"
        );

        channelProfile = new StringFilterGroup(
                null,
                "channel_profile.eml",
                "page_header.eml" // new layout
        );

        channelProfileGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_VISIT_COMMUNITY_BUTTON,
                        "community_button"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_VISIT_STORE_BUTTON,
                        "header_store_button"
                )
        );

        final StringFilterGroup membersShelf = new StringFilterGroup(
                Settings.HIDE_MEMBERS_SHELF,
                "member_recognition_shelf"
        );

        final StringFilterGroup linksPreview = new StringFilterGroup(
                Settings.HIDE_LINKS_PREVIEW,
                "channel_header_links",
                "attribution.eml" // new layout
        );

        expandableCard = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_CARD,
                INLINE_EXPANSION_PATH,
                "inline_expander",
                "expandable_metadata.eml"
        );

        final StringFilterGroup surveys = new StringFilterGroup(
                Settings.HIDE_SURVEYS,
                "feed_nudge",
                "_survey"
        );

        final StringFilterGroup forYouShelf = new StringFilterGroup(
                Settings.HIDE_FOR_YOU_SHELF,
                "mixed_content_shelf"
        );

        final StringFilterGroup imageShelf = new StringFilterGroup(
                Settings.HIDE_IMAGE_SHELF,
                "image_shelf"
        );

        final StringFilterGroup latestPosts = new StringFilterGroup(
                Settings.HIDE_LATEST_POSTS,
                "post_shelf"
        );

        final StringFilterGroup movieShelf = new StringFilterGroup(
                Settings.HIDE_MOVIE_SHELF,
                "compact_movie",
                "horizontal_movie_shelf",
                "movie_and_show_upsell_card",
                "compact_tvfilm_item",
                "offer_module"
        );

        final StringFilterGroup notifyMe = new StringFilterGroup(
                Settings.HIDE_NOTIFY_ME_BUTTON,
                "set_reminder_button"
        );

        final StringFilterGroup playables = new StringFilterGroup(
                Settings.HIDE_PLAYABLES,
                "horizontal_gaming_shelf",
                "mini_game_card.eml"
        );

        final StringFilterGroup subscribedChannelsBar = new StringFilterGroup(
                Settings.HIDE_SUBSCRIBED_CHANNELS_BAR,
                "subscriptions_channel_bar"
        );

        final StringFilterGroup subscriptionsCategoryBar = new StringFilterGroup(
                Settings.HIDE_CATEGORY_BAR_IN_FEED,
                "subscriptions_chip_bar"
        );

        chipBar = new StringFilterGroup(
                Settings.HIDE_CATEGORY_BAR_IN_HISTORY,
                "chip_bar"
        );

        final StringFilterGroup ticketShelfLegacy = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_horizontal_shelf",
                "ticket_shelf"
        );

        horizontalShelves = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "horizontal_video_shelf.eml",
                "horizontal_shelf.eml",
                "horizontal_shelf_inline.eml",
                "horizontal_tile_shelf.eml"
        );

        ticketShelf = new ByteArrayFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket.eml"
        );

        addPathCallbacks(
                albumCard,
                channelProfile,
                chipBar,
                expandableCard,
                forYouShelf,
                horizontalShelves,
                imageShelf,
                latestPosts,
                linksPreview,
                membersShelf,
                movieShelf,
                notifyMe,
                playables,
                subscribedChannelsBar,
                subscriptionsCategoryBar,
                surveys,
                ticketShelfLegacy
        );

        final StringFilterGroup communityPostsHomeAndRelatedVideos =
                new StringFilterGroup(
                        Settings.HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS,
                        CONVERSATION_CONTEXT_FEED_IDENTIFIER
                );

        final StringFilterGroup communityPostsSubscriptions =
                new StringFilterGroup(
                        Settings.HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS,
                        CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER
                );

        communityPostsFeedGroup.addAll(communityPostsHomeAndRelatedVideos, communityPostsSubscriptions);
    }

    /**
     * Injection point.
     * <p>
     * Called from a different place then the other filters.
     */
    public static boolean filterMixPlaylists(final Object conversionContext, @Nullable final byte[] bytes) {
        try {
            if (!Settings.HIDE_MIX_PLAYLISTS.get()) {
                return false;
            }
            return bytes != null
                    && mixPlaylists.check(bytes).isFiltered()
                    && !mixPlaylistsBufferExceptions.check(bytes).isFiltered()
                    && !mixPlaylistsContextExceptions.matches(conversionContext.toString());
        } catch (Exception ex) {
            Logger.printException(() -> "filterMixPlaylists failure", ex);
        }

        return false;
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == channelProfile) {
            if (contentIndex == 0 && channelProfileGroupList.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == chipBar) {
            if (contentIndex == 0 && NavigationButton.getSelectedNavigationButton() == NavigationButton.LIBRARY) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == communityPosts) {
            if (!communityPostsFeedGroupSearch.matches(allValue) && Settings.HIDE_COMMUNITY_POSTS_CHANNEL.get()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            if (communityPostsFeedGroup.check(allValue).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == expandableCard) {
            if (path.startsWith(FEED_VIDEO_PATH)) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        } else if (matchedGroup == horizontalShelves) {
            if (contentIndex == 0 && ticketShelf.check(protobufBufferArray).isFiltered()) {
                return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
            }
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
