package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.function.Supplier;
import java.util.stream.Stream;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.shared.patches.components.StringFilterGroupList;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.StringTrieSearch;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.EngagementPanel;
import app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings({"deprecation", "unused", "FieldCanBeLocal"})
public final class FeedComponentsFilter extends Filter {
    private final String BROWSE_ID_CLIP = "FEclips";
    private final String BROWSE_ID_COURSES = "FEcourses_destination";
    private final String BROWSE_ID_HISTORY = "FEhistory";
    private final String BROWSE_ID_HOME = "FEwhat_to_watch";
    private final String BROWSE_ID_LIBRARY = "FElibrary";
    private final String BROWSE_ID_LIBRARY_PLAYLIST = "FEplaylist_aggregation";
    private final String BROWSE_ID_MOVIE = "FEstorefront";
    private final String BROWSE_ID_NEWS = "FEnews_destination";
    private final String BROWSE_ID_NOTIFICATION = "FEactivity";
    private final String BROWSE_ID_NOTIFICATION_INBOX = "FEnotifications_inbox";
    private final String BROWSE_ID_PLAYLIST = "VLPL";
    private final String BROWSE_ID_PODCASTS = "FEpodcasts_destination";
    private final String BROWSE_ID_PREMIUM = "SPunlimited";
    private final String BROWSE_ID_SUBSCRIPTION = "FEsubscriptions";

    private final String CONVERSATION_CONTEXT_FEED_IDENTIFIER =
            "horizontalCollectionSwipeProtector=null";
    private final String CONVERSATION_CONTEXT_SUBSCRIPTIONS_IDENTIFIER =
            "heightConstraint=null";
    private final String INLINE_EXPANSION_PATH = "inline_expansion";
    private final String FEED_VIDEO_PATH = "video_lockup_with_attachment";

    private final StringTrieSearch communityPostStringSearchGroup = new StringTrieSearch();
    private final StringFilterGroup channelProfile;
    private final ByteArrayFilterGroupList channelProfileBufferFilterGroup = new ByteArrayFilterGroupList();
    private final StringFilterGroupList channelProfileStringFilterGroup = new StringFilterGroupList();
    private final StringFilterGroup carouselShelves;
    private final StringFilterGroup chipBar;
    private final StringFilterGroup communityPosts;
    private final StringFilterGroup expandableCard;
    private final ByteArrayFilterGroup playablesBuffer;
    private final ByteArrayFilterGroup ticketShelfBuffer;
    private final StringFilterGroupList communityPostStringFilterGroup = new StringFilterGroupList();

    private final Supplier<Stream<String>> knownBrowseId = () -> Stream.of(
            BROWSE_ID_HOME,
            BROWSE_ID_NOTIFICATION,
            BROWSE_ID_PLAYLIST
    );

    private final Supplier<Stream<String>> whitelistBrowseId = () -> Stream.of(
            BROWSE_ID_CLIP,
            BROWSE_ID_COURSES,
            BROWSE_ID_LIBRARY,
            BROWSE_ID_LIBRARY_PLAYLIST,
            BROWSE_ID_MOVIE,
            BROWSE_ID_NEWS,
            BROWSE_ID_NOTIFICATION_INBOX,
            BROWSE_ID_PODCASTS,
            BROWSE_ID_PREMIUM
    );

    private final StringTrieSearch carouselShelfExceptions = new StringTrieSearch();

    private static final ByteArrayFilterGroup mixPlaylists = new ByteArrayFilterGroup(null, "&list=");
    private static final ByteArrayFilterGroup mixPlaylistsBufferExceptions = new ByteArrayFilterGroup(
            null,
            "cell_description_body",
            "channel_profile"
    );
    private static final StringTrieSearch mixPlaylistsContextExceptions = new StringTrieSearch();

    public FeedComponentsFilter() {
        carouselShelfExceptions.addPattern("library_recent_shelf.");
        communityPostStringSearchGroup.addPatterns(
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
                "images_post_responsive",
                "images_post_root",
                "images_post_slim",
                "poll_post_root",
                "post_responsive_root",
                "post_shelf_slim",
                "shared_post_root",
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

        final StringFilterGroup movieShelfIdentifier = new StringFilterGroup(
                Settings.HIDE_MOVIE_SHELF,
                "tvfilm_attachment"
        );

        final StringFilterGroup tasteBuilder = new StringFilterGroup(
                Settings.HIDE_SURVEYS,
                "selectable_item.",
                "cell_button."
        );

        final StringFilterGroup ticketShelfIdentifier = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_"
        );

        addIdentifierCallbacks(
                chipsShelf,
                communityPosts,
                expandableShelf,
                feedSearchBar,
                movieShelfIdentifier,
                tasteBuilder,
                ticketShelfIdentifier
        );

        // Paths.

        final StringFilterGroup albumCard = new StringFilterGroup(
                Settings.HIDE_ALBUM_CARDS,
                "browsy_bar",
                "official_card"
        );

        channelProfile = new StringFilterGroup(
                null,
                "channel_profile.",
                "page_header." // new layout
        );

        channelProfileBufferFilterGroup.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_COMMUNITY_BUTTON,
                        "community_button"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_STORE_BUTTON,
                        "header_store_button"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_JOIN_BUTTON_IN_CHANNEL_PAGE,
                        "sponsor_button"
                )
        );

        channelProfileStringFilterGroup.addAll(
                new StringFilterGroup(
                        Settings.HIDE_SUBSCRIBE_BUTTON_IN_CHANNEL_PAGE,
                        "subscribe_button"
                )
        );

        final StringFilterGroup membersShelf = new StringFilterGroup(
                Settings.HIDE_MEMBERS_SHELF,
                "member_recognition_shelf"
        );

        final StringFilterGroup linksPreview = new StringFilterGroup(
                Settings.HIDE_LINKS_PREVIEW,
                "channel_header_links",
                "attribution." // new layout
        );

        expandableCard = new StringFilterGroup(
                Settings.HIDE_EXPANDABLE_CARD,
                INLINE_EXPANSION_PATH,
                "inline_expander",
                "expandable_metadata."
        );

        final StringFilterGroup surveys = new StringFilterGroup(
                Settings.HIDE_SURVEYS,
                "feed_nudge",
                "_survey"
        );

        // It appears YouTube no longer uses this keyword.
        // Just in case, I won't remove this filter until 2025.
        final StringFilterGroup forYouShelf = new StringFilterGroup(
                Settings.HIDE_CAROUSEL_SHELF_HOME,
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
                "mini_game_card."
        );

        final StringFilterGroup subscribedChannelsBar = new StringFilterGroup(
                Settings.HIDE_SUBSCRIBED_CHANNELS_BAR,
                "subscriptions_channel_bar"
        );

        final StringFilterGroup subscriptionsCategoryBar = new StringFilterGroup(
                Settings.HIDE_CATEGORY_BAR_IN_FEED,
                "subscriptions_chip_bar"
        );

        final var videoRecommendationLabels = new StringFilterGroup(
                Settings.HIDE_VIDEO_RECOMMENDATION_LABELS,
                "endorsement_header_footer."
        );

        carouselShelves = new StringFilterGroup(
                null,
                "horizontal_video_shelf.",
                "horizontal_shelf.",
                "horizontal_shelf_inline.",
                "horizontal_tile_shelf."
        );

        chipBar = new StringFilterGroup(
                null,
                "chip_bar"
        );

        final StringFilterGroup ticketShelfPath = new StringFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_horizontal_shelf",
                "ticket_shelf"
        );

        playablesBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_PLAYABLES,
                "mini_game"
        );

        ticketShelfBuffer = new ByteArrayFilterGroup(
                Settings.HIDE_TICKET_SHELF,
                "ticket_item"
        );

        addPathCallbacks(
                albumCard,
                carouselShelves,
                channelProfile,
                chipBar,
                expandableCard,
                forYouShelf,
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
                ticketShelfPath,
                videoRecommendationLabels
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

        communityPostStringFilterGroup.addAll(communityPostsHomeAndRelatedVideos, communityPostsSubscriptions);
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

    private boolean hideCategoryBar(int contentIndex) {
        if (contentIndex != 0) {
            return false;
        }

        final boolean hideHistory = Settings.HIDE_CATEGORY_BAR_IN_HISTORY.get();
        final boolean hidePlaylist = Settings.HIDE_CATEGORY_BAR_IN_PLAYLIST.get();

        if (hideHistory && hidePlaylist) {
            return true;
        }

        if (!hideHistory && !hidePlaylist) {
            return false;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (RootView.isPlayerActive()) {
            return false;
        }

        // Must check second, as search can be from any tab.
        if (RootView.isSearchBarActive()) {
            return false;
        }

        String browseId = RootView.getBrowseId();
        return (browseId.equals(BROWSE_ID_HISTORY) && hideHistory) || hidePlaylist;
    }

    private boolean hideShelves() {
        final boolean hideHomeAndOthers = Settings.HIDE_CAROUSEL_SHELF_HOME.get();
        final boolean hideSearch = Settings.HIDE_CAROUSEL_SHELF_SEARCH.get();
        final boolean hideSubscriptions = Settings.HIDE_CAROUSEL_SHELF_SUBSCRIPTIONS.get();

        if (!hideHomeAndOthers && !hideSearch && !hideSubscriptions) {
            return false;
        }

        // Must check player type first, as search bar can be active behind the player.
        if (RootView.isPlayerActive()) {
            return hideHomeAndOthers
                    && !EngagementPanel.isDescription()
                    && NavigationButton.getSelectedNavigationButton() != NavigationButton.LIBRARY;
        }

        // Must check second, as search can be from any tab.
        if (RootView.isSearchBarActive()) {
            return hideSearch;
        }

        NavigationButton selectedNavButton = NavigationButton.getSelectedNavigationButton();
        // Unknown tab, treat the same as home.
        if (selectedNavButton == null) {
            return hideHomeAndOthers;
        }

        String browseId = RootView.getBrowseId();
        // Fixes a very rare bug in home.
        if (selectedNavButton == NavigationButton.HOME
                && StringUtils.equalsAny(browseId, BROWSE_ID_LIBRARY, BROWSE_ID_NOTIFICATION_INBOX)) {
            return hideHomeAndOthers;
        }
        boolean isNotWhiteListBrowseId = whitelistBrowseId.get().noneMatch(browseId::equals);
        // Fixes a very rare bug in library.
        if (selectedNavButton == NavigationButton.LIBRARY) {
            return hideHomeAndOthers && isNotWhiteListBrowseId;
        }
        if (BROWSE_ID_SUBSCRIPTION.equals(browseId)) {
            return hideSubscriptions && isNotWhiteListBrowseId;
        }

        return hideHomeAndOthers && (knownBrowseId.get().anyMatch(browseId::equals) || isNotWhiteListBrowseId);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == channelProfile) {
            if (contentIndex != 0) {
                return false;
            }
            return channelProfileBufferFilterGroup.check(buffer).isFiltered()
                    || channelProfileStringFilterGroup.check(path).isFiltered();
        } else if (matchedGroup == chipBar) {
            return hideCategoryBar(contentIndex);
        } else if (matchedGroup == communityPosts) {
            if (!communityPostStringSearchGroup.matches(allValue) && Settings.HIDE_COMMUNITY_POSTS_CHANNEL.get()) {
                return true;
            }
            return communityPostStringFilterGroup.check(allValue).isFiltered();
        } else if (matchedGroup == expandableCard) {
            return path.startsWith(FEED_VIDEO_PATH);
        } else if (matchedGroup == carouselShelves) {
            if (contentIndex == 0) {
                return playablesBuffer.check(buffer).isFiltered()
                        || ticketShelfBuffer.check(buffer).isFiltered()
                        || (!carouselShelfExceptions.matches(path) && hideShelves());
            }
            return false;
        }

        return true;
    }
}
