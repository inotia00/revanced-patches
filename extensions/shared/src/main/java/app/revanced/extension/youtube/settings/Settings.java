package app.revanced.extension.youtube.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.revanced.extension.shared.patches.PatchStatus.PackageNameYouTubeMusic;
import static app.revanced.extension.shared.settings.Setting.migrateFromOldPreferences;
import static app.revanced.extension.shared.settings.Setting.parent;
import static app.revanced.extension.shared.settings.Setting.parentsAll;
import static app.revanced.extension.shared.settings.Setting.parentsAny;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.patches.player.MiniplayerPatch.MiniplayerType;
import static app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour.IGNORE;
import static app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour.MANUAL_SKIP;
import static app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour.SKIP_AUTOMATICALLY;
import static app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour.SKIP_AUTOMATICALLY_ONCE;
import static app.revanced.extension.youtube.utils.ExtendedUtils.IS_19_34_OR_GREATER;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.HideAudioFlyoutMenuAvailability;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.settings.LongSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.settings.preference.SharedPrefCategory;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.alternativethumbnails.AlternativeThumbnailsPatch.DeArrowAvailability;
import app.revanced.extension.youtube.patches.alternativethumbnails.AlternativeThumbnailsPatch.StillImagesAvailability;
import app.revanced.extension.youtube.patches.alternativethumbnails.AlternativeThumbnailsPatch.ThumbnailOption;
import app.revanced.extension.youtube.patches.alternativethumbnails.AlternativeThumbnailsPatch.ThumbnailStillTime;
import app.revanced.extension.youtube.patches.general.ChangeFormFactorPatch.FormFactor;
import app.revanced.extension.youtube.patches.general.ChangeStartPagePatch;
import app.revanced.extension.youtube.patches.general.ChangeStartPagePatch.StartPage;
import app.revanced.extension.youtube.patches.general.YouTubeMusicActionsPatch;
import app.revanced.extension.youtube.patches.player.ExitFullscreenPatch.FullscreenMode;
import app.revanced.extension.youtube.patches.player.MiniplayerPatch;
import app.revanced.extension.youtube.patches.shorts.AnimationFeedbackPatch.AnimationType;
import app.revanced.extension.youtube.patches.shorts.ShortsRepeatStatePatch.ShortsLoopBehavior;
import app.revanced.extension.youtube.patches.swipe.SwipeControlsPatch.SwipeOverlayBrightnessColorAvailability;
import app.revanced.extension.youtube.patches.swipe.SwipeControlsPatch.SwipeOverlayVolumeColorAvailability;
import app.revanced.extension.youtube.patches.utils.PatchStatus;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch.PlaybackSpeedMenuType;
import app.revanced.extension.youtube.shared.PlaylistIdPrefix;
import app.revanced.extension.youtube.sponsorblock.SegmentPlaybackController.SponsorBlockDuration;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockSettings;
import app.revanced.extension.youtube.swipecontrols.SwipeControlsConfigurationProvider.SwipeOverlayStyle;

@SuppressWarnings("unused")
public class Settings extends BaseSettings {
    // PreferenceScreen: Ads
    public static final BooleanSetting HIDE_CREATOR_STORE_SHELF = new BooleanSetting("revanced_hide_creator_store_shelf", TRUE);
    public static final BooleanSetting HIDE_END_SCREEN_STORE_BANNER = new BooleanSetting("revanced_hide_end_screen_store_banner", TRUE, true);
    public static final BooleanSetting HIDE_GENERAL_ADS = new BooleanSetting("revanced_hide_general_ads", TRUE);
    public static final BooleanSetting HIDE_GET_PREMIUM = new BooleanSetting("revanced_hide_get_premium", TRUE, true);
    public static final BooleanSetting HIDE_MERCHANDISE_SHELF = new BooleanSetting("revanced_hide_merchandise_shelf", TRUE);
    public static final BooleanSetting HIDE_PAID_PROMOTION_LABEL = new BooleanSetting("revanced_hide_paid_promotion_label", TRUE);
    public static final BooleanSetting HIDE_SELF_SPONSOR_CARDS = new BooleanSetting("revanced_hide_self_sponsor_cards", TRUE);
    public static final BooleanSetting HIDE_SHOPPING_LINKS = new BooleanSetting("revanced_hide_shopping_links", TRUE);
    public static final BooleanSetting HIDE_VIDEO_ADS = new BooleanSetting("revanced_hide_video_ads", TRUE, true);
    public static final BooleanSetting HIDE_VIEW_PRODUCTS = new BooleanSetting("revanced_hide_view_products", TRUE);
    public static final BooleanSetting HIDE_WEB_SEARCH_RESULTS = new BooleanSetting("revanced_hide_web_search_results", TRUE);


    // PreferenceScreen: Alternative Thumbnails
    public static final EnumSetting<ThumbnailOption> ALT_THUMBNAIL_HOME = new EnumSetting<>("revanced_alt_thumbnail_home", ThumbnailOption.ORIGINAL);
    public static final EnumSetting<ThumbnailOption> ALT_THUMBNAIL_PLAYER = new EnumSetting<>("revanced_alt_thumbnail_player", ThumbnailOption.ORIGINAL);
    public static final EnumSetting<ThumbnailOption> ALT_THUMBNAIL_SEARCH = new EnumSetting<>("revanced_alt_thumbnail_search", ThumbnailOption.ORIGINAL);
    public static final EnumSetting<ThumbnailOption> ALT_THUMBNAIL_SUBSCRIPTIONS = new EnumSetting<>("revanced_alt_thumbnail_subscriptions", ThumbnailOption.ORIGINAL);
    public static final EnumSetting<ThumbnailOption> ALT_THUMBNAIL_LIBRARY = new EnumSetting<>("revanced_alt_thumbnail_library", ThumbnailOption.ORIGINAL);
    public static final StringSetting ALT_THUMBNAIL_DEARROW_API_URL = new StringSetting("revanced_alt_thumbnail_dearrow_api_url",
            "https://dearrow-thumb.ajay.app/api/v1/getThumbnail", true, new DeArrowAvailability());
    public static final BooleanSetting ALT_THUMBNAIL_DEARROW_CONNECTION_TOAST = new BooleanSetting("revanced_alt_thumbnail_dearrow_connection_toast", FALSE, new DeArrowAvailability());
    public static final EnumSetting<ThumbnailStillTime> ALT_THUMBNAIL_STILLS_TIME = new EnumSetting<>("revanced_alt_thumbnail_stills_time", ThumbnailStillTime.MIDDLE, new StillImagesAvailability());
    public static final BooleanSetting ALT_THUMBNAIL_STILLS_FAST = new BooleanSetting("revanced_alt_thumbnail_stills_fast", FALSE, new StillImagesAvailability());


    // PreferenceScreen: Feed
    public static final BooleanSetting HIDE_ALBUM_CARDS = new BooleanSetting("revanced_hide_album_card", TRUE);
    public static final BooleanSetting HIDE_FEED_CAPTIONS_BUTTON = new BooleanSetting("revanced_hide_feed_captions_button", FALSE, true);
    public static final BooleanSetting HIDE_CHIPS_SHELF = new BooleanSetting("revanced_hide_chips_shelf", TRUE);
    public static final BooleanSetting HIDE_EXPANDABLE_CARD = new BooleanSetting("revanced_hide_expandable_card", TRUE);
    public static final BooleanSetting HIDE_EXPANDABLE_SHELF = new BooleanSetting("revanced_hide_expandable_shelf", TRUE);
    public static final BooleanSetting HIDE_FLOATING_BUTTON = new BooleanSetting("revanced_hide_floating_button", FALSE, true);
    public static final BooleanSetting HIDE_IMAGE_SHELF = new BooleanSetting("revanced_hide_image_shelf", TRUE);
    public static final BooleanSetting HIDE_LATEST_POSTS = new BooleanSetting("revanced_hide_latest_posts", TRUE);
    public static final BooleanSetting HIDE_LATEST_VIDEOS_BUTTON = new BooleanSetting("revanced_hide_latest_videos_button", TRUE);
    public static final BooleanSetting HIDE_MIX_PLAYLISTS = new BooleanSetting("revanced_hide_mix_playlists", FALSE);
    public static final BooleanSetting HIDE_MOVIE_SHELF = new BooleanSetting("revanced_hide_movie_shelf", FALSE);
    public static final BooleanSetting HIDE_NOTIFY_ME_BUTTON = new BooleanSetting("revanced_hide_notify_me_button", FALSE);
    public static final BooleanSetting HIDE_PLAYABLES = new BooleanSetting("revanced_hide_playables", FALSE);
    public static final BooleanSetting HIDE_FEED_SEARCH_BAR = new BooleanSetting("revanced_hide_feed_search_bar", FALSE);
    public static final BooleanSetting HIDE_SHOW_MORE_BUTTON = new BooleanSetting("revanced_hide_show_more_button", TRUE, true);
    public static final BooleanSetting HIDE_SUBSCRIBED_CHANNELS_BAR = new BooleanSetting("revanced_hide_subscribed_channels_bar", FALSE, true);
    public static final BooleanSetting HIDE_SURVEYS = new BooleanSetting("revanced_hide_surveys", TRUE);
    public static final BooleanSetting HIDE_TICKET_SHELF = new BooleanSetting("revanced_hide_ticket_shelf", TRUE);
    public static final BooleanSetting HIDE_VIDEO_RECOMMENDATION_LABELS = new BooleanSetting("revanced_hide_video_recommendation_labels", TRUE);

    // PreferenceScreen: Feed - Carousel shelf
    public static final BooleanSetting HIDE_CAROUSEL_SHELF_HOME = new BooleanSetting("revanced_hide_carousel_shelf_home", FALSE);
    public static final BooleanSetting HIDE_CAROUSEL_SHELF_SEARCH = new BooleanSetting("revanced_hide_carousel_shelf_search", FALSE);
    public static final BooleanSetting HIDE_CAROUSEL_SHELF_SUBSCRIPTIONS = new BooleanSetting("revanced_hide_carousel_shelf_subscriptions", FALSE);

    // PreferenceScreen: Feed - Category bar
    public static final BooleanSetting HIDE_CATEGORY_BAR_IN_FEED = new BooleanSetting("revanced_hide_category_bar_in_feed", FALSE, true);
    public static final BooleanSetting HIDE_CATEGORY_BAR_IN_HISTORY = new BooleanSetting("revanced_hide_category_bar_in_history", FALSE);
    public static final BooleanSetting HIDE_CATEGORY_BAR_IN_PLAYLIST = new BooleanSetting("revanced_hide_category_bar_in_playlist", FALSE);
    public static final BooleanSetting HIDE_CATEGORY_BAR_IN_SEARCH = new BooleanSetting("revanced_hide_category_bar_in_search", FALSE, true);
    public static final BooleanSetting HIDE_CATEGORY_BAR_IN_RELATED_VIDEOS = new BooleanSetting("revanced_hide_category_bar_in_related_videos", FALSE, true);

    // PreferenceScreen: Feed - Channel page
    public static final BooleanSetting HIDE_CHANNEL_TAB = new BooleanSetting("revanced_hide_channel_tab", FALSE);
    public static final StringSetting HIDE_CHANNEL_TAB_FILTER_STRINGS = new StringSetting("revanced_hide_channel_tab_filter_strings", "", true, parent(HIDE_CHANNEL_TAB));
    public static final BooleanSetting HIDE_LINKS_PREVIEW = new BooleanSetting("revanced_hide_links_preview", TRUE);
    public static final BooleanSetting HIDE_MEMBERS_SHELF = new BooleanSetting("revanced_hide_members_shelf", TRUE);
    public static final BooleanSetting HIDE_VISIT_COMMUNITY_BUTTON = new BooleanSetting("revanced_hide_visit_community_button", TRUE);
    public static final BooleanSetting HIDE_VISIT_STORE_BUTTON = new BooleanSetting("revanced_hide_visit_store_button", TRUE);

    // PreferenceScreen: Feed - Community posts
    public static final BooleanSetting HIDE_COMMUNITY_POSTS_CHANNEL = new BooleanSetting("revanced_hide_community_posts_channel", FALSE);
    public static final BooleanSetting HIDE_COMMUNITY_POSTS_HOME_RELATED_VIDEOS = new BooleanSetting("revanced_hide_community_posts_home_related_videos", TRUE);
    public static final BooleanSetting HIDE_COMMUNITY_POSTS_SUBSCRIPTIONS = new BooleanSetting("revanced_hide_community_posts_subscriptions", FALSE);

    // PreferenceScreen: Feed - Flyout menu
    public static final BooleanSetting HIDE_FEED_FLYOUT_MENU = new BooleanSetting("revanced_hide_feed_flyout_menu", FALSE);
    public static final BooleanSetting HIDE_FEED_FLYOUT_MENU_FILTER_TYPE = new BooleanSetting("revanced_hide_feed_flyout_menu_filter_type", FALSE, true, parent(HIDE_FEED_FLYOUT_MENU));
    public static final StringSetting HIDE_FEED_FLYOUT_MENU_FILTER_STRINGS = new StringSetting("revanced_hide_feed_flyout_menu_filter_strings", "", true, parent(HIDE_FEED_FLYOUT_MENU));

    // PreferenceScreen: Feed - Video filter
    public static final BooleanSetting HIDE_KEYWORD_CONTENT_COMMENTS = new BooleanSetting("revanced_hide_keyword_content_comments", FALSE);
    public static final BooleanSetting HIDE_KEYWORD_CONTENT_HOME = new BooleanSetting("revanced_hide_keyword_content_home", FALSE);
    public static final BooleanSetting HIDE_KEYWORD_CONTENT_SEARCH = new BooleanSetting("revanced_hide_keyword_content_search", FALSE);
    public static final BooleanSetting HIDE_KEYWORD_CONTENT_SUBSCRIPTIONS = new BooleanSetting("revanced_hide_keyword_content_subscriptions", FALSE);
    public static final StringSetting HIDE_KEYWORD_CONTENT_PHRASES = new StringSetting("revanced_hide_keyword_content_phrases", "",
            parentsAny(HIDE_KEYWORD_CONTENT_HOME, HIDE_KEYWORD_CONTENT_SEARCH, HIDE_KEYWORD_CONTENT_SUBSCRIPTIONS, HIDE_KEYWORD_CONTENT_COMMENTS));
    public static final BooleanSetting HIDE_KEYWORD_CONTENT_USE_AND_OPERATOR = new BooleanSetting("revanced_hide_keyword_content_use_and_operator", FALSE, true,
            parentsAny(HIDE_KEYWORD_CONTENT_HOME, HIDE_KEYWORD_CONTENT_SEARCH, HIDE_KEYWORD_CONTENT_SUBSCRIPTIONS, HIDE_KEYWORD_CONTENT_COMMENTS));

    public static final BooleanSetting HIDE_VIDEO_BY_VIEW_COUNTS_HOME = new BooleanSetting("revanced_hide_video_by_view_counts_home", FALSE);
    public static final BooleanSetting HIDE_VIDEO_BY_VIEW_COUNTS_SEARCH = new BooleanSetting("revanced_hide_video_by_view_counts_search", FALSE);
    public static final BooleanSetting HIDE_VIDEO_BY_VIEW_COUNTS_SUBSCRIPTIONS = new BooleanSetting("revanced_hide_video_by_view_counts_subscriptions", FALSE);
    public static final LongSetting HIDE_VIDEO_VIEW_COUNTS_LESS_THAN = new LongSetting("revanced_hide_video_view_counts_less_than", 1000L,
            parentsAny(HIDE_VIDEO_BY_VIEW_COUNTS_HOME, HIDE_VIDEO_BY_VIEW_COUNTS_SEARCH, HIDE_VIDEO_BY_VIEW_COUNTS_SUBSCRIPTIONS));
    public static final LongSetting HIDE_VIDEO_VIEW_COUNTS_GREATER_THAN = new LongSetting("revanced_hide_video_view_counts_greater_than", 1_000_000_000_000L,
            parentsAny(HIDE_VIDEO_BY_VIEW_COUNTS_HOME, HIDE_VIDEO_BY_VIEW_COUNTS_SEARCH, HIDE_VIDEO_BY_VIEW_COUNTS_SUBSCRIPTIONS));
    public static final StringSetting HIDE_VIDEO_VIEW_COUNTS_MULTIPLIER = new StringSetting("revanced_hide_video_view_counts_multiplier", str("revanced_hide_video_view_counts_multiplier_default_value"), true,
            parentsAny(HIDE_VIDEO_BY_VIEW_COUNTS_HOME, HIDE_VIDEO_BY_VIEW_COUNTS_SEARCH, HIDE_VIDEO_BY_VIEW_COUNTS_SUBSCRIPTIONS));


    // PreferenceScreen: General
    public static final EnumSetting<StartPage> CHANGE_START_PAGE = new EnumSetting<>("revanced_change_start_page", StartPage.ORIGINAL, true);
    public static final BooleanSetting CHANGE_START_PAGE_TYPE = new BooleanSetting("revanced_change_start_page_type", FALSE, true,
            new ChangeStartPagePatch.ChangeStartPageTypeAvailability());
    public static final BooleanSetting DISABLE_SIGNIN_TO_TV_POPUP = new BooleanSetting("revanced_disable_signin_to_tv_popup", FALSE);
    public static final BooleanSetting DISABLE_SPLASH_ANIMATION = new BooleanSetting("revanced_disable_splash_animation", PatchStatus.SplashAnimation(), true);
    public static final BooleanSetting ENABLE_GRADIENT_LOADING_SCREEN = new BooleanSetting("revanced_enable_gradient_loading_screen", FALSE, true);
    public static final BooleanSetting HIDE_FLOATING_MICROPHONE = new BooleanSetting("revanced_hide_floating_microphone", TRUE, true);
    public static final BooleanSetting HIDE_VISUAL_SPACERS = new BooleanSetting("revanced_hide_visual_spacers", TRUE);
    public static final BooleanSetting REMOVE_VIEWER_DISCRETION_DIALOG = new BooleanSetting("revanced_remove_viewer_discretion_dialog", FALSE);
    public static final BooleanSetting SET_TRANSCRIPT_COOKIES = new BooleanSetting("revanced_set_transcript_cookies", FALSE, true, "revanced_set_transcript_cookies_user_dialog_message");
    public static final BooleanSetting SET_TRANSCRIPT_COOKIES_ALL = new BooleanSetting("revanced_set_transcript_cookies_all", FALSE, true, parent(SET_TRANSCRIPT_COOKIES));
    public static final StringSetting TRANSCRIPT_COOKIES = new StringSetting("revanced_transcript_cookies", "", true, parent(SET_TRANSCRIPT_COOKIES));

    public static final EnumSetting<FormFactor> CHANGE_FORM_FACTOR = new EnumSetting<>("revanced_change_form_factor", FormFactor.DEFAULT, true, "revanced_change_form_factor_user_dialog_message");
    public static final BooleanSetting CHANGE_LIVE_RING_CLICK_ACTION = new BooleanSetting("revanced_change_live_ring_click_action", FALSE, true);
    public static final BooleanSetting DISABLE_LAYOUT_UPDATES = new BooleanSetting("revanced_disable_layout_updates", FALSE, true, "revanced_disable_layout_updates_user_dialog_message");
    public static final BooleanSetting DISABLE_TRANSLUCENT_STATUS_BAR = new BooleanSetting("revanced_disable_translucent_status_bar", FALSE, true, "revanced_disable_translucent_status_bar_user_dialog_message");
    public static final BooleanSetting FIX_HYPE_BUTTON_ICON = new BooleanSetting("revanced_fix_hype_button_icon", TRUE, true, "revanced_fix_hype_button_icon_user_dialog_message");
    public static final BooleanSetting SPOOF_APP_VERSION = new BooleanSetting("revanced_spoof_app_version", FALSE, true, "revanced_spoof_app_version_user_dialog_message");
    public static final StringSetting SPOOF_APP_VERSION_TARGET = new StringSetting("revanced_spoof_app_version_target", PatchStatus.SpoofAppVersionDefaultString(), true, parent(SPOOF_APP_VERSION));
    public static final BooleanSetting FIX_SPOOF_APP_VERSION_SIDE_EFFECT = new BooleanSetting("revanced_fix_spoof_app_version_side_effect", FALSE, true, "revanced_fix_spoof_app_version_side_effect_user_dialog_message");

    // PreferenceScreen: General - Account menu
    public static final BooleanSetting HIDE_ACCOUNT_MENU = new BooleanSetting("revanced_hide_account_menu", FALSE);
    public static final BooleanSetting HIDE_ACCOUNT_MENU_FILTER_TYPE = new BooleanSetting("revanced_hide_account_menu_filter_type", FALSE, true, parent(HIDE_ACCOUNT_MENU));
    public static final StringSetting HIDE_ACCOUNT_MENU_FILTER_STRINGS = new StringSetting("revanced_hide_account_menu_filter_strings", "", true, parent(HIDE_ACCOUNT_MENU));
    public static final BooleanSetting HIDE_HANDLE = new BooleanSetting("revanced_hide_handle", TRUE, true);

    // PreferenceScreen: General - Custom filter
    public static final BooleanSetting CUSTOM_FILTER = new BooleanSetting("revanced_custom_filter", FALSE);
    public static final StringSetting CUSTOM_FILTER_STRINGS = new StringSetting("revanced_custom_filter_strings", "", true, parent(CUSTOM_FILTER));

    // PreferenceScreen: General - Navigation bar
    public static final BooleanSetting ENABLE_NARROW_NAVIGATION_BUTTONS = new BooleanSetting("revanced_enable_narrow_navigation_buttons", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_CREATE_BUTTON = new BooleanSetting("revanced_hide_navigation_create_button", TRUE, true);
    public static final BooleanSetting HIDE_NAVIGATION_HOME_BUTTON = new BooleanSetting("revanced_hide_navigation_home_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LIBRARY_BUTTON = new BooleanSetting("revanced_hide_navigation_library_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_NOTIFICATIONS_BUTTON = new BooleanSetting("revanced_hide_navigation_notifications_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_SHORTS_BUTTON = new BooleanSetting("revanced_hide_navigation_shorts_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_SUBSCRIPTIONS_BUTTON = new BooleanSetting("revanced_hide_navigation_subscriptions_button", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_LABEL = new BooleanSetting("revanced_hide_navigation_label", FALSE, true);
    public static final BooleanSetting SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON = new BooleanSetting("revanced_switch_create_with_notifications_button", TRUE, true,
            "revanced_switch_create_with_notifications_button_user_dialog_message");
    public static final BooleanSetting ENABLE_TRANSLUCENT_NAVIGATION_BAR = new BooleanSetting("revanced_enable_translucent_navigation_bar", FALSE, true);
    public static final BooleanSetting HIDE_NAVIGATION_BAR = new BooleanSetting("revanced_hide_navigation_bar", FALSE, true);

    // PreferenceScreen: General - Override buttons
    public static final BooleanSetting OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON = new BooleanSetting("revanced_override_playlist_download_button", FALSE, true);
    public static final BooleanSetting OVERRIDE_VIDEO_DOWNLOAD_BUTTON = new BooleanSetting("revanced_override_video_download_button", FALSE, true);
    public static final BooleanSetting OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER = new BooleanSetting("revanced_override_video_download_button_queue_manager", FALSE, true,
            "revanced_queue_manager_user_dialog_message", parent(OVERRIDE_VIDEO_DOWNLOAD_BUTTON));
    public static final StringSetting EXTERNAL_DOWNLOADER_PACKAGE_NAME_PLAYLIST = new StringSetting("revanced_external_downloader_package_name_playlist", "com.deniscerri.ytdl");
    public static final StringSetting EXTERNAL_DOWNLOADER_PACKAGE_NAME_VIDEO = new StringSetting("revanced_external_downloader_package_name_video", "com.deniscerri.ytdl");
    public static final BooleanSetting OVERRIDE_YOUTUBE_MUSIC_BUTTON = new BooleanSetting("revanced_override_youtube_music_button", FALSE, true);
    public static final StringSetting THIRD_PARTY_YOUTUBE_MUSIC_PACKAGE_NAME = new StringSetting("revanced_third_party_youtube_music_package_name", PackageNameYouTubeMusic(), true
            , new YouTubeMusicActionsPatch.HookYouTubeMusicPackageNameAvailability());

    // PreferenceScreen: General - Settings menu
    public static final BooleanSetting HIDE_SETTINGS_MENU_PARENT_TOOLS = new BooleanSetting("revanced_hide_settings_menu_parent_tools", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_GENERAL = new BooleanSetting("revanced_hide_settings_menu_general", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_ACCOUNT = new BooleanSetting("revanced_hide_settings_menu_account", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_DATA_SAVING = new BooleanSetting("revanced_hide_settings_menu_data_saving", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_AUTOPLAY_PLAYBACK = new BooleanSetting("revanced_hide_settings_menu_autoplay_playback", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_VIDEO_QUALITY_PREFERENCES = new BooleanSetting("revanced_hide_settings_menu_video_quality", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_OFFLINE = new BooleanSetting("revanced_hide_settings_menu_offline", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_WATCH_ON_TV = new BooleanSetting("revanced_hide_settings_menu_pair_with_tv", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_MANAGE_ALL_HISTORY = new BooleanSetting("revanced_hide_settings_menu_history", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_YOUR_DATA_IN_YOUTUBE = new BooleanSetting("revanced_hide_settings_menu_your_data", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PRIVACY = new BooleanSetting("revanced_hide_settings_menu_privacy", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_TRY_EXPERIMENTAL_NEW_FEATURES = new BooleanSetting("revanced_hide_settings_menu_premium_early_access", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PURCHASES_AND_MEMBERSHIPS = new BooleanSetting("revanced_hide_settings_menu_subscription_product", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_BILLING_AND_PAYMENTS = new BooleanSetting("revanced_hide_settings_menu_billing_and_payment", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_NOTIFICATIONS = new BooleanSetting("revanced_hide_settings_menu_notification", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_CONNECTED_APPS = new BooleanSetting("revanced_hide_settings_menu_connected_accounts", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_LIVE_CHAT = new BooleanSetting("revanced_hide_settings_menu_live_chat", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_CAPTIONS = new BooleanSetting("revanced_hide_settings_menu_captions", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_ACCESSIBILITY = new BooleanSetting("revanced_hide_settings_menu_accessibility", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_ABOUT = new BooleanSetting("revanced_hide_settings_menu_about", FALSE);
    // dummy data
    public static final BooleanSetting HIDE_SETTINGS_MENU_YOUTUBE_TV = new BooleanSetting("revanced_hide_settings_menu_youtube_tv", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_PRE_PURCHASE = new BooleanSetting("revanced_hide_settings_menu_pre_purchase", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_POST_PURCHASE = new BooleanSetting("revanced_hide_settings_menu_post_purchase", FALSE);
    public static final BooleanSetting HIDE_SETTINGS_MENU_THIRD_PARTY = new BooleanSetting("revanced_hide_settings_menu_third_party", FALSE);

    // PreferenceScreen: General - Snack bar
    public static final BooleanSetting HIDE_SNACK_BAR = new BooleanSetting("revanced_hide_snack_bar", FALSE, true);
    public static final BooleanSetting HIDE_SERVER_SIDE_SNACK_BAR = new BooleanSetting("revanced_hide_server_side_snack_bar", FALSE, true);
    public static final BooleanSetting CHANGE_SERVER_SIDE_SNACK_BAR_BACKGROUND = new BooleanSetting("revanced_change_server_side_snack_bar_background", FALSE, true, "revanced_change_server_side_snack_bar_background_user_dialog_message");
    public static final BooleanSetting INVERT_SNACK_BAR_THEME = new BooleanSetting("revanced_invert_snack_bar_theme", FALSE, true);

    // PreferenceScreen: General - Toolbar
    public static final BooleanSetting CHANGE_YOUTUBE_HEADER = new BooleanSetting("revanced_change_youtube_header", TRUE, true);
    public static final BooleanSetting ENABLE_WIDE_SEARCH_BAR = new BooleanSetting("revanced_enable_wide_search_bar", FALSE, true);
    public static final BooleanSetting ENABLE_WIDE_SEARCH_BAR_WITH_HEADER = new BooleanSetting("revanced_enable_wide_search_bar_with_header", TRUE, true,
            parent(ENABLE_WIDE_SEARCH_BAR));
    public static final BooleanSetting ENABLE_WIDE_SEARCH_BAR_IN_YOU_TAB = new BooleanSetting("revanced_enable_wide_search_bar_in_you_tab", FALSE, true,
            "revanced_enable_wide_search_bar_in_you_tab_user_dialog_message", parent(ENABLE_WIDE_SEARCH_BAR));
    public static final BooleanSetting HIDE_TOOLBAR_CAST_BUTTON = new BooleanSetting("revanced_hide_toolbar_cast_button", TRUE, true);
    public static final BooleanSetting HIDE_TOOLBAR_CREATE_BUTTON = new BooleanSetting("revanced_hide_toolbar_create_button", FALSE, true);
    public static final BooleanSetting HIDE_TOOLBAR_NOTIFICATION_BUTTON = new BooleanSetting("revanced_hide_toolbar_notification_button", FALSE, true);
    public static final BooleanSetting HIDE_SEARCH_TERM_THUMBNAIL = new BooleanSetting("revanced_hide_search_term_thumbnail", FALSE);
    public static final BooleanSetting HIDE_IMAGE_SEARCH_BUTTON = new BooleanSetting("revanced_hide_image_search_button", FALSE, true);
    public static final BooleanSetting HIDE_VOICE_SEARCH_BUTTON = new BooleanSetting("revanced_hide_voice_search_button", FALSE, true);
    public static final BooleanSetting HIDE_YOU_MAY_LIKE_SECTION = new BooleanSetting("revanced_hide_you_may_like_section", FALSE, true);
    public static final BooleanSetting HIDE_YOUTUBE_DOODLES = new BooleanSetting("revanced_hide_youtube_doodles", FALSE, true, "revanced_hide_youtube_doodles_user_dialog_message");
    public static final BooleanSetting REPLACE_TOOLBAR_CREATE_BUTTON = new BooleanSetting("revanced_replace_toolbar_create_button", FALSE, true);
    public static final BooleanSetting REPLACE_TOOLBAR_CREATE_BUTTON_TYPE = new BooleanSetting("revanced_replace_toolbar_create_button_type", FALSE, true,
            parent(REPLACE_TOOLBAR_CREATE_BUTTON));


    // PreferenceScreen: Player
    public static final IntegerSetting CUSTOM_PLAYER_OVERLAY_OPACITY = new IntegerSetting("revanced_custom_player_overlay_opacity", 100, true);
    public static final BooleanSetting DISABLE_AUTO_SWITCH_MIX_PLAYLISTS = new BooleanSetting("revanced_disable_auto_switch_mix_playlists", FALSE, true, "revanced_disable_auto_switch_mix_playlists_user_dialog_message");
    public static final BooleanSetting DISABLE_AUTO_PLAYER_POPUP_PANELS = new BooleanSetting("revanced_disable_auto_player_popup_panels", TRUE, true);
    public static final BooleanSetting DISABLE_CHAPTER_SKIP_DOUBLE_TAP = new BooleanSetting("revanced_disable_chapter_skip_double_tap", FALSE);
    public static final BooleanSetting DISABLE_SPEED_OVERLAY = new BooleanSetting("revanced_disable_speed_overlay", FALSE, true);
    public static final FloatSetting SPEED_OVERLAY_VALUE = new FloatSetting("revanced_speed_overlay_value", 2.0f, true);
    public static final BooleanSetting HIDE_CHANNEL_WATERMARK = new BooleanSetting("revanced_hide_channel_watermark", TRUE);
    public static final BooleanSetting HIDE_CROWDFUNDING_BOX = new BooleanSetting("revanced_hide_crowdfunding_box", TRUE, true);
    public static final BooleanSetting HIDE_DOUBLE_TAP_OVERLAY_FILTER = new BooleanSetting("revanced_hide_double_tap_overlay_filter", FALSE, true);
    public static final BooleanSetting HIDE_END_SCREEN_CARDS = new BooleanSetting("revanced_hide_end_screen_cards", FALSE, true);
    public static final BooleanSetting HIDE_END_SCREEN_SUGGESTED_VIDEO = new BooleanSetting("revanced_hide_end_screen_suggested_video", TRUE, true);
    public static final BooleanSetting SKIP_AUTOPLAY_COUNTDOWN = new BooleanSetting("revanced_skip_autoplay_countdown", FALSE, true, parent(HIDE_END_SCREEN_SUGGESTED_VIDEO));
    public static final BooleanSetting HIDE_FILMSTRIP_OVERLAY = new BooleanSetting("revanced_hide_filmstrip_overlay", FALSE, true);
    public static final BooleanSetting HIDE_INFO_CARDS = new BooleanSetting("revanced_hide_info_cards", FALSE, true);
    public static final BooleanSetting HIDE_INFO_PANEL = new BooleanSetting("revanced_hide_info_panel", TRUE);
    public static final BooleanSetting HIDE_MEDICAL_PANEL = new BooleanSetting("revanced_hide_medical_panel", TRUE);
    public static final BooleanSetting HIDE_SEEK_MESSAGE = new BooleanSetting("revanced_hide_seek_message", FALSE, true);
    public static final BooleanSetting HIDE_SEEK_UNDO_MESSAGE = new BooleanSetting("revanced_hide_seek_undo_message", FALSE, true);
    public static final BooleanSetting HIDE_SUGGESTED_ACTION = new BooleanSetting("revanced_hide_suggested_actions", TRUE, true);
    public static final BooleanSetting HIDE_TIMED_REACTIONS = new BooleanSetting("revanced_hide_timed_reactions", TRUE);
    public static final BooleanSetting HIDE_ZOOM_OVERLAY = new BooleanSetting("revanced_hide_zoom_overlay", FALSE, true);
    public static final BooleanSetting SANITIZE_VIDEO_SUBTITLE = new BooleanSetting("revanced_sanitize_video_subtitle", FALSE);

    // Experimental Flags
    public static final BooleanSetting HIDE_RELATED_VIDEOS = new BooleanSetting("revanced_hide_related_videos", FALSE, true, "revanced_hide_related_videos_user_dialog_message");
    public static final IntegerSetting RELATED_VIDEOS_OFFSET = new IntegerSetting("revanced_related_videos_offset", 2, true, parent(HIDE_RELATED_VIDEOS));


    // PreferenceScreen: Player - Action buttons
    public static final BooleanSetting DISABLE_LIKE_DISLIKE_GLOW = new BooleanSetting("revanced_disable_like_dislike_glow", FALSE);
    public static final BooleanSetting HIDE_ASK_BUTTON = new BooleanSetting("revanced_hide_ask_button", FALSE);
    public static final BooleanSetting HIDE_CLIP_BUTTON = new BooleanSetting("revanced_hide_clip_button", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_BUTTON = new BooleanSetting("revanced_hide_comments_button", FALSE);
    public static final BooleanSetting HIDE_DOWNLOAD_BUTTON = new BooleanSetting("revanced_hide_download_button", FALSE);
    public static final BooleanSetting HIDE_HYPE_BUTTON = new BooleanSetting("revanced_hide_hype_button", FALSE);
    public static final BooleanSetting HIDE_LIKE_DISLIKE_BUTTON = new BooleanSetting("revanced_hide_like_dislike_button", FALSE);
    public static final BooleanSetting HIDE_PLAYLIST_BUTTON = new BooleanSetting("revanced_hide_playlist_button", FALSE);
    public static final BooleanSetting HIDE_REMIX_BUTTON = new BooleanSetting("revanced_hide_remix_button", FALSE);
    public static final BooleanSetting HIDE_REWARDS_BUTTON = new BooleanSetting("revanced_hide_rewards_button", FALSE);
    public static final BooleanSetting HIDE_REPORT_BUTTON = new BooleanSetting("revanced_hide_report_button", FALSE);
    public static final BooleanSetting HIDE_SHARE_BUTTON = new BooleanSetting("revanced_hide_share_button", FALSE);
    public static final BooleanSetting HIDE_SHOP_BUTTON = new BooleanSetting("revanced_hide_shop_button", FALSE);
    public static final BooleanSetting HIDE_STOP_ADS_BUTTON = new BooleanSetting("revanced_hide_stop_ads_button", FALSE);
    public static final BooleanSetting HIDE_THANKS_BUTTON = new BooleanSetting("revanced_hide_thanks_button", FALSE);

    public static final BooleanSetting HIDE_ACTION_BUTTON_INDEX = new BooleanSetting("revanced_hide_action_button_index", FALSE, true);
    public static final IntegerSetting REMIX_BUTTON_INDEX = new IntegerSetting("revanced_remix_button_index", 3, true, parent(HIDE_ACTION_BUTTON_INDEX));

    // PreferenceScreen: Player - Ambient mode
    public static final BooleanSetting BYPASS_AMBIENT_MODE_RESTRICTIONS = new BooleanSetting("revanced_bypass_ambient_mode_restrictions", FALSE);
    public static final BooleanSetting DISABLE_AMBIENT_MODE = new BooleanSetting("revanced_disable_ambient_mode", FALSE, true);
    public static final BooleanSetting DISABLE_AMBIENT_MODE_IN_FULLSCREEN = new BooleanSetting("revanced_disable_ambient_mode_in_fullscreen", FALSE, true);

    // PreferenceScreen: Player - Channel bar
    public static final BooleanSetting HIDE_JOIN_BUTTON = new BooleanSetting("revanced_hide_join_button", TRUE);
    public static final BooleanSetting HIDE_START_TRIAL_BUTTON = new BooleanSetting("revanced_hide_start_trial_button", TRUE);

    // PreferenceScreen: Player - Comments
    public static final BooleanSetting HIDE_AI_CHAT_SUMMARY = new BooleanSetting("revanced_hide_ai_chat_summary", FALSE);
    public static final BooleanSetting HIDE_AI_COMMENTS_SUMMARY = new BooleanSetting("revanced_hide_ai_comments_summary", FALSE);
    public static final BooleanSetting HIDE_CHANNEL_GUIDELINES = new BooleanSetting("revanced_hide_channel_guidelines", TRUE);
    public static final BooleanSetting HIDE_COMMENTS_BY_MEMBERS = new BooleanSetting("revanced_hide_comments_by_members", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_SECTION = new BooleanSetting("revanced_hide_comments_section", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_SECTION_IN_HOME_FEED = new BooleanSetting("revanced_hide_comments_section_in_home_feed", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_CREATE_A_SHORT_BUTTON = new BooleanSetting("revanced_hide_comments_create_a_short_button", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_EMOJI_AND_TIMESTAMP_BUTTONS = new BooleanSetting("revanced_hide_comments_emoji_and_timestamp_buttons", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_HIGHLIGHTED_SEARCH_LINKS = new BooleanSetting("revanced_hide_comments_highlighted_search_links", FALSE, true);
    public static final BooleanSetting HIDE_LIVE_CHAT_MESSAGES = new BooleanSetting("revanced_hide_live_chat_messages", FALSE);
    public static final BooleanSetting HIDE_PREVIEW_COMMENT = new BooleanSetting("revanced_hide_preview_comment", FALSE);
    public static final BooleanSetting HIDE_PREVIEW_COMMENT_TYPE = new BooleanSetting("revanced_hide_preview_comment_type", FALSE, parent(HIDE_PREVIEW_COMMENT));
    public static final BooleanSetting HIDE_PREVIEW_COMMENT_OLD_METHOD = new BooleanSetting("revanced_hide_preview_comment_old_method", FALSE);
    public static final BooleanSetting HIDE_PREVIEW_COMMENT_NEW_METHOD = new BooleanSetting("revanced_hide_preview_comment_new_method", FALSE);
    public static final BooleanSetting HIDE_COMMENTS_THANKS_BUTTON = new BooleanSetting("revanced_hide_comments_thanks_button", FALSE, true);
    public static final BooleanSetting SANITIZE_COMMENTS_CATEGORY_BAR = new BooleanSetting("revanced_sanitize_comments_category_bar", FALSE);

    // PreferenceScreen: Player - Flyout menu
    public static final BooleanSetting CHANGE_PLAYER_FLYOUT_MENU_TOGGLE = new BooleanSetting("revanced_change_player_flyout_menu_toggle", FALSE, true);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_ENHANCED_BITRATE = new BooleanSetting("revanced_hide_player_flyout_menu_enhanced_bitrate", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_AUDIO_TRACK = new BooleanSetting("revanced_hide_player_flyout_menu_audio_track", FALSE, new HideAudioFlyoutMenuAvailability());
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_CAPTIONS = new BooleanSetting("revanced_hide_player_flyout_menu_captions", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_CAPTIONS_FOOTER = new BooleanSetting("revanced_hide_player_flyout_menu_captions_footer", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_LOCK_SCREEN = new BooleanSetting("revanced_hide_player_flyout_menu_lock_screen", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_MORE = new BooleanSetting("revanced_hide_player_flyout_menu_more_info", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_PLAYBACK_SPEED = new BooleanSetting("revanced_hide_player_flyout_menu_playback_speed", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_QUALITY_HEADER = new BooleanSetting("revanced_hide_player_flyout_menu_quality_header", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_QUALITY_FOOTER = new BooleanSetting("revanced_hide_player_flyout_menu_quality_footer", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_REPORT = new BooleanSetting("revanced_hide_player_flyout_menu_report", TRUE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_SLEEP_TIMER = new BooleanSetting("revanced_hide_player_flyout_menu_sleep_timer", FALSE);

    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS = new BooleanSetting("revanced_hide_player_flyout_menu_additional_settings", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_AMBIENT = new BooleanSetting("revanced_hide_player_flyout_menu_ambient_mode", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_HELP = new BooleanSetting("revanced_hide_player_flyout_menu_help", TRUE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_LOOP = new BooleanSetting("revanced_hide_player_flyout_menu_loop_video", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_PIP = new BooleanSetting("revanced_hide_player_flyout_menu_pip", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS = new BooleanSetting("revanced_hide_player_flyout_menu_premium_controls", TRUE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME = new BooleanSetting("revanced_hide_player_flyout_menu_stable_volume", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS = new BooleanSetting("revanced_hide_player_flyout_menu_stats_for_nerds", FALSE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR = new BooleanSetting("revanced_hide_player_flyout_menu_watch_in_vr", TRUE);
    public static final BooleanSetting HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC = new BooleanSetting("revanced_hide_player_flyout_menu_listen_with_youtube_music", TRUE);

    // PreferenceScreen: Player - Fullscreen
    public static final BooleanSetting DISABLE_ENGAGEMENT_PANEL = new BooleanSetting("revanced_disable_engagement_panel", FALSE, true);
    public static final BooleanSetting ENTER_FULLSCREEN = new BooleanSetting("revanced_enter_fullscreen", FALSE);
    public static final EnumSetting<FullscreenMode> EXIT_FULLSCREEN = new EnumSetting<>("revanced_exit_fullscreen", FullscreenMode.DISABLED);
    public static final BooleanSetting SHOW_VIDEO_TITLE_SECTION = new BooleanSetting("revanced_show_video_title_section", TRUE, true, parent(DISABLE_ENGAGEMENT_PANEL));
    public static final BooleanSetting HIDE_AUTOPLAY_PREVIEW = new BooleanSetting("revanced_hide_autoplay_preview", FALSE, true);
    public static final BooleanSetting HIDE_LIVE_CHAT_REPLAY_BUTTON = new BooleanSetting("revanced_hide_live_chat_replay_button", FALSE);
    public static final BooleanSetting HIDE_RELATED_VIDEOS_OVERLAY = new BooleanSetting("revanced_hide_related_videos_overlay", FALSE, true);

    public static final BooleanSetting HIDE_QUICK_ACTIONS = new BooleanSetting("revanced_hide_quick_actions", FALSE, true);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_COMMENT_BUTTON = new BooleanSetting("revanced_hide_quick_actions_comment_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_DISLIKE_BUTTON = new BooleanSetting("revanced_hide_quick_actions_dislike_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_LIKE_BUTTON = new BooleanSetting("revanced_hide_quick_actions_like_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_LIVE_CHAT_BUTTON = new BooleanSetting("revanced_hide_quick_actions_live_chat_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_MORE_BUTTON = new BooleanSetting("revanced_hide_quick_actions_more_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_OPEN_MIX_PLAYLIST_BUTTON = new BooleanSetting("revanced_hide_quick_actions_open_mix_playlist_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_OPEN_PLAYLIST_BUTTON = new BooleanSetting("revanced_hide_quick_actions_open_playlist_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_SAVE_TO_PLAYLIST_BUTTON = new BooleanSetting("revanced_hide_quick_actions_save_to_playlist_button", FALSE);
    public static final BooleanSetting HIDE_QUICK_ACTIONS_SHARE_BUTTON = new BooleanSetting("revanced_hide_quick_actions_share_button", FALSE);
    public static final IntegerSetting QUICK_ACTIONS_TOP_MARGIN = new IntegerSetting("revanced_quick_actions_top_margin", 0, true);

    public static final BooleanSetting DISABLE_LANDSCAPE_MODE = new BooleanSetting("revanced_disable_landscape_mode", FALSE, true);
    public static final BooleanSetting ENABLE_COMPACT_CONTROLS_OVERLAY = new BooleanSetting("revanced_enable_compact_controls_overlay", FALSE, true);
    public static final BooleanSetting KEEP_LANDSCAPE_MODE = new BooleanSetting("revanced_keep_landscape_mode", FALSE, true);
    public static final LongSetting KEEP_LANDSCAPE_MODE_TIMEOUT = new LongSetting("revanced_keep_landscape_mode_timeout", 3000L, true, parent(KEEP_LANDSCAPE_MODE));

    // PreferenceScreen: Player - Haptic feedback
    public static final BooleanSetting DISABLE_HAPTIC_FEEDBACK_CHAPTERS = new BooleanSetting("revanced_disable_haptic_feedback_chapters", FALSE);
    public static final BooleanSetting DISABLE_HAPTIC_FEEDBACK_SCRUBBING = new BooleanSetting("revanced_disable_haptic_feedback_scrubbing", FALSE);
    public static final BooleanSetting DISABLE_HAPTIC_FEEDBACK_SEEK = new BooleanSetting("revanced_disable_haptic_feedback_seek", FALSE);
    public static final BooleanSetting DISABLE_HAPTIC_FEEDBACK_SEEK_UNDO = new BooleanSetting("revanced_disable_haptic_feedback_seek_undo", FALSE);
    public static final BooleanSetting DISABLE_HAPTIC_FEEDBACK_ZOOM = new BooleanSetting("revanced_disable_haptic_feedback_zoom", FALSE);

    // PreferenceScreen: Player - Miniplayer
    public static final BooleanSetting DISABLE_RESUMING_MINIPLAYER = new BooleanSetting("revanced_disable_resuming_miniplayer", FALSE, true);
    public static final EnumSetting<MiniplayerType> MINIPLAYER_TYPE = new EnumSetting<>("revanced_miniplayer_type", MiniplayerType.DEFAULT, true);
    public static final BooleanSetting MINIPLAYER_DOUBLE_TAP_ACTION = new BooleanSetting("revanced_miniplayer_double_tap_action", TRUE, true, new MiniplayerPatch.MiniplayerAnyModernAvailability());
    public static final BooleanSetting MINIPLAYER_DRAG_AND_DROP = new BooleanSetting("revanced_miniplayer_drag_and_drop", TRUE, true, new MiniplayerPatch.MiniplayerAnyModernAvailability());
    public static final BooleanSetting MINIPLAYER_HORIZONTAL_DRAG = new BooleanSetting("revanced_miniplayer_horizontal_drag", FALSE, true, new MiniplayerPatch.MiniplayerHorizontalDragAvailability());
    public static final BooleanSetting MINIPLAYER_HIDE_OVERLAY_BUTTONS = new BooleanSetting("revanced_miniplayer_hide_overlay_buttons", FALSE, true, new MiniplayerPatch.MiniplayerHideOverlayButtonsAvailability());
    public static final BooleanSetting MINIPLAYER_HIDE_SUBTEXT = new BooleanSetting("revanced_miniplayer_hide_subtext", FALSE, true, new MiniplayerPatch.MiniplayerHideSubtextsAvailability());
    public static final BooleanSetting MINIPLAYER_HIDE_REWIND_FORWARD = new BooleanSetting("revanced_miniplayer_hide_rewind_forward", TRUE, true, new MiniplayerPatch.MiniplayerHideRewindOrOverlayOpacityAvailability());
    public static final BooleanSetting MINIPLAYER_ROUNDED_CORNERS = new BooleanSetting("revanced_miniplayer_rounded_corners", TRUE, true, new MiniplayerPatch.MiniplayerAnyModernAvailability());
    public static final IntegerSetting MINIPLAYER_WIDTH_DIP = new IntegerSetting("revanced_miniplayer_width_dip", 192, true, new MiniplayerPatch.MiniplayerAnyModernAvailability());
    public static final IntegerSetting MINIPLAYER_OPACITY = new IntegerSetting("revanced_miniplayer_opacity", 100, true, new MiniplayerPatch.MiniplayerHideRewindOrOverlayOpacityAvailability());

    // PreferenceScreen: Player - Player buttons
    public static final BooleanSetting HIDE_PLAYER_AUTOPLAY_BUTTON = new BooleanSetting("revanced_hide_player_autoplay_button", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_CAPTIONS_BUTTON = new BooleanSetting("revanced_hide_player_captions_button", FALSE, true);
    public static final BooleanSetting HIDE_PLAYER_CAST_BUTTON = new BooleanSetting("revanced_hide_player_cast_button", TRUE, true);
    public static final BooleanSetting HIDE_PLAYER_COLLAPSE_BUTTON = new BooleanSetting("revanced_hide_player_collapse_button", FALSE, true);
    public static final BooleanSetting HIDE_PLAYER_FULLSCREEN_BUTTON = new BooleanSetting("revanced_hide_player_fullscreen_button", FALSE, true);
    public static final BooleanSetting HIDE_PLAYER_PREVIOUS_NEXT_BUTTON = new BooleanSetting("revanced_hide_player_previous_next_button", FALSE, true);
    public static final BooleanSetting HIDE_PLAYER_YOUTUBE_MUSIC_BUTTON = new BooleanSetting("revanced_hide_player_youtube_music_button", FALSE);

    public static final BooleanSetting ALWAYS_REPEAT = new BooleanSetting("revanced_always_repeat", FALSE);
    public static final BooleanSetting ALWAYS_REPEAT_PAUSE = new BooleanSetting("revanced_always_repeat_pause", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_ALWAYS_REPEAT = new BooleanSetting("revanced_overlay_button_always_repeat", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_COPY_VIDEO_URL = new BooleanSetting("revanced_overlay_button_copy_video_url", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_COPY_VIDEO_URL_TIMESTAMP = new BooleanSetting("revanced_overlay_button_copy_video_url_timestamp", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_MUTE_VOLUME = new BooleanSetting("revanced_overlay_button_mute_volume", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_EXTERNAL_DOWNLOADER = new BooleanSetting("revanced_overlay_button_external_downloader", FALSE);
    public static final BooleanSetting OVERLAY_BUTTON_EXTERNAL_DOWNLOADER_QUEUE_MANAGER = new BooleanSetting("revanced_overlay_button_external_downloader_queue_manager", FALSE, true,
            "revanced_queue_manager_user_dialog_message", parent(OVERLAY_BUTTON_EXTERNAL_DOWNLOADER));
    public static final BooleanSetting OVERLAY_BUTTON_SPEED_DIALOG = new BooleanSetting("revanced_overlay_button_speed_dialog", FALSE);
    public static final EnumSetting<PlaybackSpeedMenuType> OVERLAY_BUTTON_SPEED_DIALOG_TYPE = new EnumSetting<>("revanced_overlay_button_speed_dialog_type", PlaybackSpeedMenuType.CUSTOM_MODERN, parent(OVERLAY_BUTTON_SPEED_DIALOG));
    public static final BooleanSetting OVERLAY_BUTTON_PLAY_ALL = new BooleanSetting("revanced_overlay_button_play_all", FALSE);
    public static final EnumSetting<PlaylistIdPrefix> OVERLAY_BUTTON_PLAY_ALL_TYPE = new EnumSetting<>("revanced_overlay_button_play_all_type", PlaylistIdPrefix.ALL_CONTENTS_WITH_TIME_DESCENDING, parent(OVERLAY_BUTTON_PLAY_ALL));
    public static final BooleanSetting OVERLAY_BUTTON_WHITELIST = new BooleanSetting("revanced_overlay_button_whitelist", FALSE);

    // PreferenceScreen: Player - Seekbar
    public static final BooleanSetting APPEND_TIME_STAMP_INFORMATION = new BooleanSetting("revanced_append_time_stamp_information", TRUE, true);
    public static final BooleanSetting APPEND_TIME_STAMP_INFORMATION_TYPE = new BooleanSetting("revanced_append_time_stamp_information_type", TRUE, parent(APPEND_TIME_STAMP_INFORMATION));
    public static final EnumSetting<PlaybackSpeedMenuType> APPEND_TIME_STAMP_INFORMATION_PLAYBACK_SPEED_MENU_TYPE = new EnumSetting<>("revanced_append_time_stamp_information_playback_speed_menu_type", PlaybackSpeedMenuType.CUSTOM_MODERN, parent(APPEND_TIME_STAMP_INFORMATION));
    public static final BooleanSetting APPEND_TIME_STAMP_INFORMATION_VIDEO_QUALITY_MENU_TYPE = new BooleanSetting("revanced_append_time_stamp_information_video_quality_menu_type", TRUE, parent(APPEND_TIME_STAMP_INFORMATION));

    public static final BooleanSetting REPLACE_TIME_STAMP_ACTION = new BooleanSetting("revanced_replace_time_stamp_action", TRUE, true, parent(APPEND_TIME_STAMP_INFORMATION));
    public static final BooleanSetting DISABLE_SEEKBAR_CHAPTERS = new BooleanSetting("revanced_disable_seekbar_chapters", FALSE, true);
    public static final BooleanSetting ENABLE_CUSTOM_SEEKBAR_COLOR = new BooleanSetting("revanced_enable_custom_seekbar_color", FALSE, true);
    public static final StringSetting CUSTOM_SEEKBAR_COLOR_PRIMARY = new StringSetting("revanced_custom_seekbar_color_primary", "#FF0033", true, parent(ENABLE_CUSTOM_SEEKBAR_COLOR));
    public static final StringSetting CUSTOM_SEEKBAR_COLOR_ACCENT = new StringSetting("revanced_custom_seekbar_color_accent", "#FF2791", true, parent(ENABLE_CUSTOM_SEEKBAR_COLOR));
    public static final BooleanSetting ENABLE_SEEKBAR_TAPPING = new BooleanSetting("revanced_enable_seekbar_tapping", TRUE);
    public static final BooleanSetting HIDE_SEEKBAR_CHAPTER_LABEL = new BooleanSetting("revanced_hide_seekbar_chapter_label", FALSE, true);
    public static final BooleanSetting HIDE_SEEKBAR = new BooleanSetting("revanced_hide_seekbar", FALSE, true);
    public static final BooleanSetting HIDE_SEEKBAR_THUMBNAIL = new BooleanSetting("revanced_hide_seekbar_thumbnail", FALSE, true);
    public static final BooleanSetting HIDE_TIME_STAMP = new BooleanSetting("revanced_hide_time_stamp", FALSE, true);
    public static final BooleanSetting RESTORE_OLD_SEEKBAR_THUMBNAILS = new BooleanSetting("revanced_restore_old_seekbar_thumbnails",
            PatchStatus.OldSeekbarThumbnailsDefaultBoolean(), true);
    public static final BooleanSetting ENABLE_SEEKBAR_THUMBNAILS_HIGH_QUALITY = new BooleanSetting("revanced_enable_seekbar_thumbnails_high_quality", FALSE, true, "revanced_enable_seekbar_thumbnails_high_quality_dialog_message");

    // PreferenceScreen: Player - Video description
    public static final BooleanSetting DISABLE_ROLLING_NUMBER_ANIMATIONS = new BooleanSetting("revanced_disable_rolling_number_animations", FALSE);
    public static final BooleanSetting HIDE_AI_GENERATED_VIDEO_SUMMARY_SECTION = new BooleanSetting("revanced_hide_ai_generated_video_summary_section", FALSE);
    public static final BooleanSetting HIDE_ATTRIBUTES_SECTION = new BooleanSetting("revanced_hide_attributes_section", FALSE);
    public static final BooleanSetting HIDE_CHAPTERS_SECTION = new BooleanSetting("revanced_hide_chapters_section", FALSE);
    public static final BooleanSetting HIDE_CONTENTS_SECTION = new BooleanSetting("revanced_hide_contents_section", FALSE);
    public static final BooleanSetting HIDE_HYPE_POINTS_SECTION = new BooleanSetting("revanced_hide_hype_points_section", FALSE);
    public static final BooleanSetting HIDE_PODCAST_SECTION = new BooleanSetting("revanced_hide_podcast_section", FALSE);
    public static final BooleanSetting HIDE_INFO_CARDS_SECTION = new BooleanSetting("revanced_hide_info_cards_section", FALSE);
    public static final BooleanSetting HIDE_KEY_CONCEPTS_SECTION = new BooleanSetting("revanced_hide_key_concepts_section", FALSE);
    public static final BooleanSetting HIDE_TRANSCRIPT_SECTION = new BooleanSetting("revanced_hide_transcript_section", FALSE);
    public static final BooleanSetting DISABLE_VIDEO_DESCRIPTION_INTERACTION = new BooleanSetting("revanced_disable_video_description_interaction", FALSE, true);
    public static final BooleanSetting EXPAND_VIDEO_DESCRIPTION = new BooleanSetting("revanced_expand_video_description", FALSE, true);


    // PreferenceScreen: Shorts
    public static final BooleanSetting DISABLE_RESUMING_SHORTS_PLAYER = new BooleanSetting("revanced_disable_resuming_shorts_player", TRUE);
    public static final BooleanSetting DISABLE_SHORTS_BACKGROUND_PLAYBACK = new BooleanSetting("revanced_disable_shorts_background_playback", FALSE);
    public static final BooleanSetting HIDE_SHORTS_FLOATING_BUTTON = new BooleanSetting("revanced_hide_shorts_floating_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SHELF = new BooleanSetting("revanced_hide_shorts_shelf", TRUE, true);
    public static final BooleanSetting HIDE_SHORTS_SHELF_CHANNEL = new BooleanSetting("revanced_hide_shorts_shelf_channel", FALSE, parent(HIDE_SHORTS_SHELF));
    public static final BooleanSetting HIDE_SHORTS_SHELF_HOME_RELATED_VIDEOS = new BooleanSetting("revanced_hide_shorts_shelf_home_related_videos", TRUE, parent(HIDE_SHORTS_SHELF));
    public static final BooleanSetting HIDE_SHORTS_SHELF_SEARCH = new BooleanSetting("revanced_hide_shorts_shelf_search", TRUE, parent(HIDE_SHORTS_SHELF));
    public static final BooleanSetting HIDE_SHORTS_SHELF_SUBSCRIPTIONS = new BooleanSetting("revanced_hide_shorts_shelf_subscriptions", TRUE, parent(HIDE_SHORTS_SHELF));
    public static final BooleanSetting HIDE_SHORTS_SHELF_VIDEO_DESCRIPTION = new BooleanSetting("revanced_hide_shorts_shelf_video_description", TRUE, parent(HIDE_SHORTS_SHELF));
    public static final BooleanSetting HIDE_SHORTS_SHELF_HISTORY = new BooleanSetting("revanced_hide_shorts_shelf_history", FALSE, parent(HIDE_SHORTS_SHELF));
    public static final EnumSetting<ShortsLoopBehavior> CHANGE_SHORTS_BACKGROUND_REPEAT_STATE = new EnumSetting<>("revanced_change_shorts_background_repeat_state", ShortsLoopBehavior.UNKNOWN);
    public static final EnumSetting<ShortsLoopBehavior> CHANGE_SHORTS_REPEAT_STATE = new EnumSetting<>("revanced_change_shorts_repeat_state", ShortsLoopBehavior.UNKNOWN);
    public static final BooleanSetting OPEN_SHORTS_IN_REGULAR_PLAYER = new BooleanSetting("revanced_open_shorts_in_regular_player", FALSE);

    // PreferenceScreen: Shorts - Shorts player components
    public static final BooleanSetting HIDE_SHORTS_CHANNEL_BAR = new BooleanSetting("revanced_hide_shorts_channel_bar", FALSE);
    public static final BooleanSetting HIDE_SHORTS_INFO_PANEL = new BooleanSetting("revanced_hide_shorts_info_panel", TRUE);
    public static final BooleanSetting HIDE_SHORTS_JOIN_BUTTON = new BooleanSetting("revanced_hide_shorts_join_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_LIVE_HEADER = new BooleanSetting("revanced_hide_shorts_live_header", FALSE);
    public static final BooleanSetting HIDE_SHORTS_PAID_PROMOTION_LABEL = new BooleanSetting("revanced_hide_shorts_paid_promotion_label", TRUE, true);
    public static final BooleanSetting HIDE_SHORTS_PAUSED_HEADER = new BooleanSetting("revanced_hide_shorts_paused_header", FALSE, true);
    public static final BooleanSetting HIDE_SHORTS_PAUSED_OVERLAY_BUTTONS = new BooleanSetting("revanced_hide_shorts_paused_overlay_buttons", FALSE);
    public static final BooleanSetting HIDE_SHORTS_SHOPPING_BUTTON = new BooleanSetting("revanced_hide_shorts_shopping_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SOUND_METADATA_LABEL = new BooleanSetting("revanced_hide_shorts_sound_metadata_label", TRUE);
    public static final BooleanSetting HIDE_SHORTS_STICKERS = new BooleanSetting("revanced_hide_shorts_stickers", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SUBSCRIBE_BUTTON = new BooleanSetting("revanced_hide_shorts_subscribe_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_TRENDS_BUTTON = new BooleanSetting("revanced_hide_shorts_trends_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_VIDEO_LINK_LABEL = new BooleanSetting("revanced_hide_shorts_video_link_label", TRUE);
    public static final BooleanSetting HIDE_SHORTS_VIDEO_TITLE = new BooleanSetting("revanced_hide_shorts_video_title", FALSE);

    // PreferenceScreen: Shorts - Shorts player components - Suggested actions
    public static final BooleanSetting HIDE_SHORTS_BUY_SUPER_THANKS_BUTTON = new BooleanSetting("revanced_hide_shorts_buy_super_thanks_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_GREEN_SCREEN_BUTTON = new BooleanSetting("revanced_hide_shorts_green_screen_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_LOCATION_BUTTON = new BooleanSetting("revanced_hide_shorts_location_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SAVE_MUSIC_BUTTON = new BooleanSetting("revanced_hide_shorts_save_music_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SEARCH_SUGGESTIONS_BUTTON = new BooleanSetting("revanced_hide_shorts_search_suggestions_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SHOP_BUTTON = new BooleanSetting("revanced_hide_shorts_shop_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_TAGGED_PRODUCTS = new BooleanSetting("revanced_hide_shorts_tagged_products", TRUE);
    public static final BooleanSetting HIDE_SHORTS_USE_THIS_SOUND_BUTTON = new BooleanSetting("revanced_hide_shorts_use_this_sound_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_USE_THIS_TEMPLATE_BUTTON = new BooleanSetting("revanced_hide_shorts_use_this_template_button", TRUE);

    // PreferenceScreen: Shorts - Shorts player components - Action buttons
    public static final BooleanSetting HIDE_SHORTS_LIKE_BUTTON = new BooleanSetting("revanced_hide_shorts_like_button", FALSE);
    public static final BooleanSetting HIDE_SHORTS_DISLIKE_BUTTON = new BooleanSetting("revanced_hide_shorts_dislike_button", FALSE);
    public static final BooleanSetting HIDE_SHORTS_COMMENTS_BUTTON = new BooleanSetting("revanced_hide_shorts_comments_button", FALSE);
    public static final BooleanSetting HIDE_SHORTS_REMIX_BUTTON = new BooleanSetting("revanced_hide_shorts_remix_button", TRUE);
    public static final BooleanSetting HIDE_SHORTS_SHARE_BUTTON = new BooleanSetting("revanced_hide_shorts_share_button", FALSE);
    public static final BooleanSetting HIDE_SHORTS_SOUND_BUTTON = new BooleanSetting("revanced_hide_shorts_sound_button", TRUE);

    // PreferenceScreen: Shorts - Shorts player components - Animation / Feedback
    public static final BooleanSetting DISABLE_SHORTS_LIKE_BUTTON_FOUNTAIN_ANIMATION = new BooleanSetting("revanced_disable_shorts_like_button_fountain_animation", FALSE);
    public static final EnumSetting<AnimationType> ANIMATION_TYPE = new EnumSetting<>("revanced_shorts_double_tap_to_like_animation", AnimationType.ORIGINAL, true);
    public static final BooleanSetting HIDE_SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND = new BooleanSetting("revanced_hide_shorts_play_pause_button_background", FALSE, true);

    // PreferenceScreen: Shorts - Shorts player components - Custom actions
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL_TIMESTAMP = new BooleanSetting("revanced_shorts_custom_actions_copy_video_url_timestamp", FALSE, true);
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL = new BooleanSetting("revanced_shorts_custom_actions_copy_video_url", FALSE, true);
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_EXTERNAL_DOWNLOADER = new BooleanSetting("revanced_shorts_custom_actions_external_downloader", FALSE, true);
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_OPEN_VIDEO = new BooleanSetting("revanced_shorts_custom_actions_open_video", FALSE, true);
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG = new BooleanSetting("revanced_shorts_custom_actions_speed_dialog", FALSE, true);
    public static final BooleanSetting SHORTS_CUSTOM_ACTIONS_REPEAT_STATE = new BooleanSetting("revanced_shorts_custom_actions_repeat_state", FALSE, true);
    public static final EnumSetting<PlaybackSpeedMenuType> SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG_TYPE = new EnumSetting<>("revanced_shorts_custom_actions_speed_dialog_type", IS_19_34_OR_GREATER ? PlaybackSpeedMenuType.CUSTOM_MODERN : PlaybackSpeedMenuType.CUSTOM_LEGACY,
            true, IS_19_34_OR_GREATER ? null : "revanced_shorts_custom_actions_speed_dialog_type_user_dialog_message", parent(SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG));

    public static final BooleanSetting ENABLE_SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU = new BooleanSetting("revanced_enable_shorts_custom_actions_flyout_menu", FALSE, true,
            parentsAny(SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL, SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL_TIMESTAMP, SHORTS_CUSTOM_ACTIONS_EXTERNAL_DOWNLOADER, SHORTS_CUSTOM_ACTIONS_OPEN_VIDEO, SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG, SHORTS_CUSTOM_ACTIONS_REPEAT_STATE));
    public static final BooleanSetting ENABLE_SHORTS_CUSTOM_ACTIONS_TOOLBAR = new BooleanSetting("revanced_enable_shorts_custom_actions_toolbar", FALSE, true,
            parentsAny(SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL, SHORTS_CUSTOM_ACTIONS_COPY_VIDEO_URL_TIMESTAMP, SHORTS_CUSTOM_ACTIONS_EXTERNAL_DOWNLOADER, SHORTS_CUSTOM_ACTIONS_OPEN_VIDEO, SHORTS_CUSTOM_ACTIONS_SPEED_DIALOG, SHORTS_CUSTOM_ACTIONS_REPEAT_STATE));

    // Experimental Flags
    public static final BooleanSetting ENABLE_SHORTS_TIME_STAMP = new BooleanSetting("revanced_enable_shorts_time_stamp", FALSE, true);
    public static final BooleanSetting ENABLE_SHORTS_CLEAR_MODE = new BooleanSetting("revanced_enable_shorts_clear_mode", FALSE, true, parent(ENABLE_SHORTS_TIME_STAMP));
    public static final BooleanSetting HIDE_SHORTS_NAVIGATION_BAR = new BooleanSetting("revanced_hide_shorts_navigation_bar", FALSE, true);
    public static final IntegerSetting SHORTS_NAVIGATION_BAR_HEIGHT_PERCENTAGE = new IntegerSetting("revanced_shorts_navigation_bar_height_percentage", 45, true, parent(HIDE_SHORTS_NAVIGATION_BAR));
    public static final BooleanSetting HIDE_SHORTS_TOOLBAR = new BooleanSetting("revanced_hide_shorts_toolbar", FALSE, true);
    public static final BooleanSetting REPLACE_CHANNEL_HANDLE = new BooleanSetting("revanced_replace_channel_handle", FALSE, true);
    public static final BooleanSetting RESTORE_SHORTS_OLD_PLAYER_LAYOUT = new BooleanSetting("revanced_restore_shorts_old_player_layout", FALSE, true);

    // PreferenceScreen: Swipe controls
    public static final BooleanSetting SWIPE_BRIGHTNESS = new BooleanSetting("revanced_swipe_brightness", TRUE, true);
    public static final BooleanSetting SWIPE_VOLUME = new BooleanSetting("revanced_swipe_volume", TRUE, true);
    public static final BooleanSetting SWIPE_LOWEST_VALUE_ENABLE_AUTO_BRIGHTNESS = new BooleanSetting("revanced_swipe_lowest_value_enable_auto_brightness", TRUE, true,
            parent(SWIPE_BRIGHTNESS));
    public static final BooleanSetting SWIPE_PRESS_TO_ENGAGE = new BooleanSetting("revanced_swipe_press_to_engage", FALSE, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final BooleanSetting SWIPE_HAPTIC_FEEDBACK = new BooleanSetting("revanced_swipe_haptic_feedback", TRUE, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final BooleanSetting SWIPE_SAVE_AND_RESTORE_BRIGHTNESS = new BooleanSetting("revanced_swipe_save_and_restore_brightness", TRUE, true,
            parent(SWIPE_BRIGHTNESS));
    public static final BooleanSetting SWIPE_LOCK_MODE = new BooleanSetting("revanced_swipe_gestures_lock_mode", FALSE, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final IntegerSetting SWIPE_MAGNITUDE_THRESHOLD = new IntegerSetting("revanced_swipe_threshold", 30, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final EnumSetting<SwipeOverlayStyle> SWIPE_OVERLAY_STYLE = new EnumSetting<>("revanced_swipe_overlay_style", SwipeOverlayStyle.HORIZONTAL, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final StringSetting SWIPE_OVERLAY_BRIGHTNESS_COLOR = new StringSetting("revanced_swipe_overlay_progress_brightness_color", "#BFFFFFFF", true,
            new SwipeOverlayBrightnessColorAvailability());
    public static final StringSetting SWIPE_OVERLAY_VOLUME_COLOR = new StringSetting("revanced_swipe_overlay_progress_volume_color", "#BFFFFFFF", true,
            new SwipeOverlayVolumeColorAvailability());
    public static final IntegerSetting SWIPE_OVERLAY_OPACITY = new IntegerSetting("revanced_swipe_overlay_background_opacity", 60, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final IntegerSetting SWIPE_OVERLAY_RECT_SIZE = new IntegerSetting("revanced_swipe_overlay_rect_size", 20, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final IntegerSetting SWIPE_OVERLAY_TEXT_SIZE = new IntegerSetting("revanced_swipe_text_overlay_size", 14, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final LongSetting SWIPE_OVERLAY_TIMEOUT = new LongSetting("revanced_swipe_overlay_timeout", 500L, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));
    public static final FloatSetting SWIPE_BRIGHTNESS_DISTANCE_DIP = new FloatSetting("revanced_swipe_brightness_distance_dip", 1f, true, parent(SWIPE_BRIGHTNESS));
    public static final IntegerSetting SWIPE_VOLUMES_SENSITIVITY = new IntegerSetting("revanced_swipe_volumes_sensitivity", 1, true, parent(SWIPE_VOLUME));
    public static final FloatSetting SWIPE_BRIGHTNESS_VALUE = new FloatSetting("revanced_swipe_brightness_value", -1f);

    public static final BooleanSetting ENABLE_SWIPE_TO_SWITCH_VIDEO = new BooleanSetting("revanced_enable_swipe_to_switch_video", FALSE, true);
    /**
     * @noinspection DeprecatedIsStillUsed
     */
    @Deprecated // Patch is obsolete and no longer works with 19.09+
    public static final BooleanSetting DISABLE_HDR_AUTO_BRIGHTNESS = new BooleanSetting("revanced_disable_hdr_auto_brightness", TRUE, true, parent(SWIPE_BRIGHTNESS));
    public static final BooleanSetting DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_BELOW_THE_PLAYER = new BooleanSetting("revanced_disable_swipe_to_enter_fullscreen_mode_below_the_player", FALSE, true);
    public static final BooleanSetting DISABLE_SWIPE_TO_ENTER_FULLSCREEN_MODE_IN_THE_PLAYER = new BooleanSetting("revanced_disable_swipe_to_enter_fullscreen_mode_in_the_player", FALSE, true);
    public static final BooleanSetting DISABLE_SWIPE_TO_EXIT_FULLSCREEN_MODE = new BooleanSetting("revanced_disable_swipe_to_exit_fullscreen_mode", FALSE, true);
    public static final BooleanSetting FIX_SWIPE_TAP_AND_HOLD_SPEED = new BooleanSetting("revanced_fix_swipe_tap_and_hold_speed", FALSE, true,
            parentsAny(SWIPE_BRIGHTNESS, SWIPE_VOLUME));


    // PreferenceScreen: Video - Codec
    public static final BooleanSetting DISABLE_HDR_VIDEO = new BooleanSetting("revanced_disable_hdr_video", FALSE, true);
    public static final BooleanSetting DISABLE_VP9_CODEC = new BooleanSetting("revanced_disable_vp9_codec", FALSE, true);

    // PreferenceScreen: Video - Playback speed
    public static final FloatSetting DEFAULT_PLAYBACK_SPEED = new FloatSetting("revanced_default_playback_speed", -2.0f);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_LAST_SELECTED = new BooleanSetting("revanced_remember_playback_speed_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_playback_speed_last_selected_toast", TRUE, parent(REMEMBER_PLAYBACK_SPEED_LAST_SELECTED));
    public static final FloatSetting DEFAULT_PLAYBACK_SPEED_SHORTS = new FloatSetting("revanced_default_playback_speed_shorts", -2.0f);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_SHORTS_LAST_SELECTED = new BooleanSetting("revanced_remember_playback_speed_shorts_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_PLAYBACK_SPEED_SHORTS_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_playback_speed_shorts_last_selected_toast", TRUE, parent(REMEMBER_PLAYBACK_SPEED_SHORTS_LAST_SELECTED));
    public static final BooleanSetting ENABLE_CUSTOM_PLAYBACK_SPEED = new BooleanSetting("revanced_enable_custom_playback_speed", FALSE, true);
    public static final EnumSetting<PlaybackSpeedMenuType> CUSTOM_PLAYBACK_SPEED_MENU_TYPE = new EnumSetting<>("revanced_custom_playback_speed_menu_type", PlaybackSpeedMenuType.CUSTOM_MODERN, parent(ENABLE_CUSTOM_PLAYBACK_SPEED));
    public static final StringSetting CUSTOM_PLAYBACK_SPEEDS = new StringSetting("revanced_custom_playback_speeds", "0.25\n0.5\n0.75\n1.0\n1.25\n1.5\n1.75\n2.0\n2.25\n2.5", true, parent(ENABLE_CUSTOM_PLAYBACK_SPEED));

    // PreferenceScreen: Video - Video quality
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_MOBILE = new IntegerSetting("revanced_default_video_quality_mobile", -2);
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_WIFI = new IntegerSetting("revanced_default_video_quality_wifi", -2);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_LAST_SELECTED = new BooleanSetting("revanced_remember_video_quality_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_video_quality_last_selected_toast", TRUE, parent(REMEMBER_VIDEO_QUALITY_LAST_SELECTED));
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_MOBILE_SHORTS = new IntegerSetting("revanced_default_video_quality_mobile_shorts", -2, true);
    public static final IntegerSetting DEFAULT_VIDEO_QUALITY_WIFI_SHORTS = new IntegerSetting("revanced_default_video_quality_wifi_shorts", -2, true);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED = new BooleanSetting("revanced_remember_video_quality_shorts_last_selected", TRUE);
    public static final BooleanSetting REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED_TOAST = new BooleanSetting("revanced_remember_video_quality_shorts_last_selected_toast", TRUE, parent(REMEMBER_VIDEO_QUALITY_SHORTS_LAST_SELECTED));
    public static final BooleanSetting ADVANCED_VIDEO_QUALITY_MENU = new BooleanSetting("revanced_advanced_video_quality_menu", TRUE, true);
    public static final BooleanSetting ADVANCED_VIDEO_QUALITY_MENU_TYPE = new BooleanSetting("revanced_advanced_video_quality_menu_type", TRUE, true, parent(ADVANCED_VIDEO_QUALITY_MENU));
    public static final BooleanSetting SPOOF_DEVICE_DIMENSIONS = new BooleanSetting("revanced_spoof_device_dimensions", FALSE, true);

    public static final BooleanSetting DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC = new BooleanSetting("revanced_disable_default_playback_speed_music", FALSE, true);
    public static final BooleanSetting DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC_TYPE = new BooleanSetting("revanced_disable_default_playback_speed_music_type", FALSE, true, parent(DISABLE_DEFAULT_PLAYBACK_SPEED_MUSIC));

    // PreferenceScreen: Miscellaneous
    public static final BooleanSetting BYPASS_URL_REDIRECTS = new BooleanSetting("revanced_bypass_url_redirects", TRUE);
    public static final BooleanSetting OPEN_LINKS_EXTERNALLY = new BooleanSetting("revanced_open_links_externally", TRUE, true);
    public static final BooleanSetting ENABLE_WATCH_NEXT_PROCESSING_DELAY = new BooleanSetting("revanced_enable_watch_next_processing_delay", FALSE,
            "revanced_enable_watch_next_processing_delay_user_dialog_message");
    public static final IntegerSetting WATCH_NEXT_PROCESSING_DELAY = new IntegerSetting("revanced_watch_next_processing_delay", 10000,
            parent(ENABLE_WATCH_NEXT_PROCESSING_DELAY));

    // Experimental Flags
    public static final BooleanSetting CHANGE_SHARE_SHEET = new BooleanSetting("revanced_change_share_sheet", FALSE, true);

    /**
     * @noinspection DeprecatedIsStillUsed
     */
    @Deprecated
    public static final LongSetting DOUBLE_BACK_TO_CLOSE_TIMEOUT = new LongSetting("revanced_double_back_to_close_timeout", 2000L);

    // PreferenceScreen: Miscellaneous - Watch history

    // PreferenceScreen: Miscellaneous - Spoof streaming data

    // PreferenceScreen: Return YouTube Dislike
    public static final BooleanSetting RYD_ENABLED = new BooleanSetting("ryd_enabled", TRUE);
    public static final StringSetting RYD_USER_ID = new StringSetting("ryd_user_id", "");
    public static final BooleanSetting RYD_SHORTS = new BooleanSetting("ryd_shorts", TRUE, parent(RYD_ENABLED));
    public static final BooleanSetting RYD_DISLIKE_PERCENTAGE = new BooleanSetting("ryd_dislike_percentage", FALSE, parent(RYD_ENABLED));
    public static final BooleanSetting RYD_COMPACT_LAYOUT = new BooleanSetting("ryd_compact_layout", FALSE, parent(RYD_ENABLED));
    public static final BooleanSetting RYD_ESTIMATED_LIKE = new BooleanSetting("ryd_estimated_like", FALSE, true, parent(RYD_ENABLED));
    public static final BooleanSetting RYD_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("ryd_toast_on_connection_error", TRUE, parent(RYD_ENABLED));


    // PreferenceScreen: SponsorBlock
    public static final BooleanSetting SB_ENABLED = new BooleanSetting("sb_enabled", TRUE);
    /**
     * Do not use directly, instead use {@link SponsorBlockSettings}
     */
    public static final StringSetting SB_PRIVATE_USER_ID = new StringSetting("sb_private_user_id_Do_Not_Share", "", parent(SB_ENABLED));
    public static final IntegerSetting SB_CREATE_NEW_SEGMENT_STEP = new IntegerSetting("sb_create_new_segment_step", 150, parent(SB_ENABLED));
    public static final BooleanSetting SB_VOTING_BUTTON = new BooleanSetting("sb_voting_button", FALSE, parent(SB_ENABLED));
    public static final BooleanSetting SB_CREATE_NEW_SEGMENT = new BooleanSetting("sb_create_new_segment", FALSE, parent(SB_ENABLED));
    public static final BooleanSetting SB_SQUARE_LAYOUT = new BooleanSetting("sb_square_layout", FALSE, parent(SB_ENABLED));
    public static final BooleanSetting SB_COMPACT_SKIP_BUTTON = new BooleanSetting("sb_compact_skip_button", FALSE, parent(SB_ENABLED));
    public static final BooleanSetting SB_AUTO_HIDE_SKIP_BUTTON = new BooleanSetting("sb_auto_hide_skip_button", TRUE, parent(SB_ENABLED));
    public static final EnumSetting<SponsorBlockDuration> SB_AUTO_HIDE_SKIP_BUTTON_DURATION = new EnumSetting<>("sb_auto_hide_skip_button_duration",
            SponsorBlockDuration.FOUR_SECONDS, parent(SB_ENABLED));
    public static final BooleanSetting SB_TOAST_ON_SKIP = new BooleanSetting("sb_toast_on_skip", TRUE, parent(SB_ENABLED));
    public static final EnumSetting<SponsorBlockDuration> SB_TOAST_ON_SKIP_DURATION = new EnumSetting<>("sb_toast_on_skip_duration",
            SponsorBlockDuration.FOUR_SECONDS, parentsAll(SB_ENABLED, SB_TOAST_ON_SKIP));
    public static final BooleanSetting SB_TOAST_ON_CONNECTION_ERROR = new BooleanSetting("sb_toast_on_connection_error", TRUE, parent(SB_ENABLED));
    public static final BooleanSetting SB_TRACK_SKIP_COUNT = new BooleanSetting("sb_track_skip_count", TRUE, parent(SB_ENABLED));
    public static final FloatSetting SB_SEGMENT_MIN_DURATION = new FloatSetting("sb_min_segment_duration", 0F, parent(SB_ENABLED));
    public static final BooleanSetting SB_VIDEO_LENGTH_WITHOUT_SEGMENTS = new BooleanSetting("sb_video_length_without_segments", FALSE, parent(SB_ENABLED));
    public static final StringSetting SB_API_URL = new StringSetting("sb_api_url", "https://sponsor.ajay.app", parent(SB_ENABLED));
    public static final BooleanSetting SB_USER_IS_VIP = new BooleanSetting("sb_user_is_vip", FALSE);
    public static final IntegerSetting SB_LOCAL_TIME_SAVED_NUMBER_SEGMENTS = new IntegerSetting("sb_local_time_saved_number_segments", 0, parent(SB_ENABLED));
    public static final LongSetting SB_LOCAL_TIME_SAVED_MILLISECONDS = new LongSetting("sb_local_time_saved_milliseconds", 0L, parent(SB_ENABLED));

    public static final StringSetting SB_CATEGORY_SPONSOR = new StringSetting("sb_sponsor", SKIP_AUTOMATICALLY_ONCE.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_SPONSOR_COLOR = new StringSetting("sb_sponsor_color", "#CC00D400");
    public static final StringSetting SB_CATEGORY_SELF_PROMO = new StringSetting("sb_selfpromo", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_SELF_PROMO_COLOR = new StringSetting("sb_selfpromo_color", "#CCFFFF00");
    public static final StringSetting SB_CATEGORY_INTERACTION = new StringSetting("sb_interaction", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_INTERACTION_COLOR = new StringSetting("sb_interaction_color", "#CCCC00FF");
    public static final StringSetting SB_CATEGORY_HIGHLIGHT = new StringSetting("sb_highlight", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_HIGHLIGHT_COLOR = new StringSetting("sb_highlight_color", "#CCFF1684");
    public static final StringSetting SB_CATEGORY_HOOK = new StringSetting("sb_hook", IGNORE.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_HOOK_COLOR = new StringSetting("sb_hook_color", "#CC395699");
    public static final StringSetting SB_CATEGORY_INTRO = new StringSetting("sb_intro", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_INTRO_COLOR = new StringSetting("sb_intro_color", "#CC00FFFF");
    public static final StringSetting SB_CATEGORY_OUTRO = new StringSetting("sb_outro", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_OUTRO_COLOR = new StringSetting("sb_outro_color", "#CC0202ED");
    public static final StringSetting SB_CATEGORY_PREVIEW = new StringSetting("sb_preview", IGNORE.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_PREVIEW_COLOR = new StringSetting("sb_preview_color", "#CC008FD6");
    public static final StringSetting SB_CATEGORY_FILLER = new StringSetting("sb_filler", IGNORE.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_FILLER_COLOR = new StringSetting("sb_filler_color", "#CC7300FF");
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC = new StringSetting("sb_music_offtopic", MANUAL_SKIP.reVancedKeyValue, parent(SB_ENABLED));
    public static final StringSetting SB_CATEGORY_MUSIC_OFFTOPIC_COLOR = new StringSetting("sb_music_offtopic_color", "#CCFF9900");
    // Dummy setting. Category is not exposed in the UI nor does it ever change.
    public static final StringSetting SB_CATEGORY_UNSUBMITTED = new StringSetting("sb_unsubmitted", SKIP_AUTOMATICALLY.reVancedKeyValue, false, false);
    public static final StringSetting SB_CATEGORY_UNSUBMITTED_COLOR = new StringSetting("sb_unsubmitted_color", "#FFFFFFFF", false, false);

    // SB Setting not exported
    public static final LongSetting SB_LAST_VIP_CHECK = new LongSetting("sb_last_vip_check", 0L, false, false);
    public static final BooleanSetting SB_HIDE_EXPORT_WARNING = new BooleanSetting("sb_hide_export_warning", FALSE, false, false);
    public static final BooleanSetting SB_SEEN_GUIDELINES = new BooleanSetting("sb_seen_guidelines", FALSE, false, false);

    // Deprecated migrations
    private static final FloatSetting DEPRECATED_SB_CATEGORY_SPONSOR_OPACITY = new FloatSetting("sb_sponsor_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_SELF_PROMO_OPACITY = new FloatSetting("sb_selfpromo_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_INTERACTION_OPACITY = new FloatSetting("sb_interaction_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_HIGHLIGHT_OPACITY = new FloatSetting("sb_highlight_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_HOOK_OPACITY = new FloatSetting("sb_hook_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_INTRO_OPACITY = new FloatSetting("sb_intro_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_OUTRO_OPACITY = new FloatSetting("sb_outro_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_PREVIEW_OPACITY = new FloatSetting("sb_preview_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_FILLER_OPACITY = new FloatSetting("sb_filler_opacity", 0.8f, false, false);
    private static final FloatSetting DEPRECATED_SB_CATEGORY_MUSIC_OFFTOPIC_OPACITY = new FloatSetting("sb_music_offtopic_opacity", 0.8f, false, false);

    static {
        // region Migration initialized

        // Old spoof versions that no longer work reliably.
        boolean spoofAppVersionIncluded = PatchStatus.SpoofAppVersion();
        if (!spoofAppVersionIncluded && SPOOF_APP_VERSION.get()) {
            Logger.printInfo(() -> "Resetting spoof app version");
            SPOOF_APP_VERSION.resetToDefault();
            SPOOF_APP_VERSION_TARGET.resetToDefault();
        }
        String spoofAppVersionTarget = SPOOF_APP_VERSION_TARGET.get();
        if (spoofAppVersionIncluded && spoofAppVersionTarget.compareTo(SPOOF_APP_VERSION_TARGET.defaultValue) < 0) {
            Utils.showToastShort(str("revanced_spoof_app_version_target_invalid_toast", spoofAppVersionTarget));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            Logger.printInfo(() -> "Resetting spoof app version target");
            SPOOF_APP_VERSION_TARGET.resetToDefault();
        }

        // Categories were previously saved without a 'sb_' key prefix, so they need an additional adjustment.
        Set<Setting<?>> sbCategories = new HashSet<>(Arrays.asList(
                SB_CATEGORY_SPONSOR,
                SB_CATEGORY_SPONSOR_COLOR,
                SB_CATEGORY_SELF_PROMO,
                SB_CATEGORY_SELF_PROMO_COLOR,
                SB_CATEGORY_INTERACTION,
                SB_CATEGORY_INTERACTION_COLOR,
                SB_CATEGORY_HIGHLIGHT,
                SB_CATEGORY_HIGHLIGHT_COLOR,
                SB_CATEGORY_INTRO,
                SB_CATEGORY_INTRO_COLOR,
                SB_CATEGORY_OUTRO,
                SB_CATEGORY_OUTRO_COLOR,
                SB_CATEGORY_PREVIEW,
                SB_CATEGORY_PREVIEW_COLOR,
                SB_CATEGORY_FILLER,
                SB_CATEGORY_FILLER_COLOR,
                SB_CATEGORY_MUSIC_OFFTOPIC,
                SB_CATEGORY_MUSIC_OFFTOPIC_COLOR,
                SB_CATEGORY_UNSUBMITTED,
                SB_CATEGORY_UNSUBMITTED_COLOR));

        SharedPrefCategory ytPrefs = new SharedPrefCategory("youtube");
        SharedPrefCategory rydPrefs = new SharedPrefCategory("ryd");
        SharedPrefCategory sbPrefs = new SharedPrefCategory("sponsor-block");
        for (Setting<?> setting : Setting.allLoadedSettings()) {
            String key = setting.key;
            if (setting.key.startsWith("sb_")) {
                if (sbCategories.contains(setting)) {
                    key = key.substring(3); // Remove the "sb_" prefix, as old categories are saved without it.
                }
                migrateFromOldPreferences(sbPrefs, setting, key);
            } else if (setting.key.startsWith("ryd_")) {
                migrateFromOldPreferences(rydPrefs, setting, key);
            } else {
                migrateFromOldPreferences(ytPrefs, setting, key);
            }
        }

        // Migrate old saved data. Must be done here before the settings can be used by any other code.
        applyOldSbOpacityToColor(SB_CATEGORY_SPONSOR_COLOR, DEPRECATED_SB_CATEGORY_SPONSOR_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_SELF_PROMO_COLOR, DEPRECATED_SB_CATEGORY_SELF_PROMO_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_INTERACTION_COLOR, DEPRECATED_SB_CATEGORY_INTERACTION_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_HIGHLIGHT_COLOR, DEPRECATED_SB_CATEGORY_HIGHLIGHT_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_HOOK_COLOR, DEPRECATED_SB_CATEGORY_HOOK_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_INTRO_COLOR, DEPRECATED_SB_CATEGORY_INTRO_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_OUTRO_COLOR, DEPRECATED_SB_CATEGORY_OUTRO_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_PREVIEW_COLOR, DEPRECATED_SB_CATEGORY_PREVIEW_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_FILLER_COLOR, DEPRECATED_SB_CATEGORY_FILLER_OPACITY);
        applyOldSbOpacityToColor(SB_CATEGORY_MUSIC_OFFTOPIC_COLOR, DEPRECATED_SB_CATEGORY_MUSIC_OFFTOPIC_OPACITY);

        // endregion

        // region SB import/export callbacks

        Setting.addImportExportCallback(SponsorBlockSettings.SB_IMPORT_EXPORT_CALLBACK);

        // endregion
    }

    private static void applyOldSbOpacityToColor(StringSetting colorSetting, FloatSetting opacitySetting) {
        String colorString = colorSetting.get();
        if (colorString.length() >= 8) {
            return; // Color is already #ARGB
        }

        colorSetting.save(SponsorBlockSettings.migrateOldColorString(colorString, opacitySetting.get()));
    }
}
