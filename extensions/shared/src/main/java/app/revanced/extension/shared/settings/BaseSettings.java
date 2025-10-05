package app.revanced.extension.shared.settings;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.revanced.extension.shared.patches.PatchStatus.SpoofStreamingDataYouTube;
import static app.revanced.extension.shared.settings.Setting.parent;

import app.revanced.extension.shared.innertube.client.YouTubeClient.ClientType;
import app.revanced.extension.shared.patches.ReturnYouTubeUsernamePatch.DisplayFormat;
import app.revanced.extension.shared.patches.WatchHistoryPatch.WatchHistoryType;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.ClientAndroidVRAvailability;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.ClientJSAvailability;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.ClientSingleAudioTrackAvailability;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.J2V8Availability;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch.ShowReloadVideoButtonAvailability;

/**
 * Settings shared across multiple apps.
 * <p>
 * To ensure this class is loaded when the UI is created, app specific setting bundles should extend
 * or reference this class.
 */
public class BaseSettings {
    public static final BooleanSetting DEBUG = new BooleanSetting("revanced_debug", FALSE);
    public static final BooleanSetting DEBUG_PROTOBUFFER = new BooleanSetting("revanced_debug_protobuffer", FALSE, parent(DEBUG));
    public static final BooleanSetting DEBUG_SPANNABLE = new BooleanSetting("revanced_debug_spannable", FALSE, parent(DEBUG));
    public static final BooleanSetting DEBUG_TOAST_ON_ERROR = new BooleanSetting("revanced_debug_toast_on_error", FALSE);
    public static final BooleanSetting SETTINGS_INITIALIZED = new BooleanSetting("revanced_settings_initialized", FALSE, false, false);

    public static final EnumSetting<AppLanguage> REVANCED_LANGUAGE = new EnumSetting<>("revanced_language", AppLanguage.DEFAULT, true);

    /**
     * These settings are used by YouTube.
     * Some patches are in a shared path, so they are declared here.
     */
    public static final BooleanSetting SPOOF_STREAMING_DATA = new BooleanSetting("revanced_spoof_streaming_data", TRUE, true, "revanced_spoof_streaming_data_user_dialog_message");
    public static final BooleanSetting SPOOF_STREAMING_DATA_PRIORITIZE_VIDEO_QUALITY = new BooleanSetting("revanced_spoof_streaming_data_prioritize_video_quality", FALSE, true,
            "revanced_spoof_streaming_data_prioritize_video_quality_user_dialog_message", parent(SPOOF_STREAMING_DATA));
    public static final BooleanSetting SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON = new BooleanSetting("revanced_spoof_streaming_data_reload_video_button", TRUE, true, parent(SPOOF_STREAMING_DATA));
    public static final BooleanSetting SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON_ALWAYS_SHOW = new BooleanSetting("revanced_spoof_streaming_data_reload_video_button_always_show", TRUE, true, new ShowReloadVideoButtonAvailability());
    public static final BooleanSetting SPOOF_STREAMING_DATA_STATS_FOR_NERDS = new BooleanSetting("revanced_spoof_streaming_data_stats_for_nerds", TRUE, parent(SPOOF_STREAMING_DATA));

    public static final BooleanSetting SPOOF_STREAMING_DATA_VR_ENABLE_AV1 = new BooleanSetting("revanced_spoof_streaming_data_vr_enable_av1", FALSE, true,
            "revanced_spoof_streaming_data_vr_enable_av1_user_dialog_message", new ClientAndroidVRAvailability());

    public static final EnumSetting<AppLanguage> SPOOF_STREAMING_DATA_NO_AUTH_LANGUAGE = new EnumSetting<>("revanced_spoof_streaming_data_no_auth_language", AppLanguage.DEFAULT, new ClientSingleAudioTrackAvailability());
    public static final BooleanSetting SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON = new BooleanSetting("revanced_spoof_streaming_data_audio_track_button", FALSE, true,
            "revanced_spoof_streaming_data_audio_track_button_user_dialog_message", new ClientSingleAudioTrackAvailability());

    public static final BooleanSetting SPOOF_STREAMING_DATA_USE_JS = new BooleanSetting("revanced_spoof_streaming_data_use_js", !SpoofStreamingDataYouTube(), true,
            "revanced_spoof_streaming_data_use_js_user_dialog_message", new J2V8Availability());
    public static final BooleanSetting SPOOF_STREAMING_DATA_USE_JS_ALL = new BooleanSetting("revanced_spoof_streaming_data_use_js_all", FALSE, true, new ClientJSAvailability());
    public static final BooleanSetting SPOOF_STREAMING_DATA_USE_YT_DLP_EJS = new BooleanSetting("revanced_spoof_streaming_data_use_yt_dlp_ejs", FALSE, true,
            "revanced_spoof_streaming_data_use_yt_dlp_ejs_user_dialog_message", new ClientJSAvailability());
    public static final BooleanSetting SPOOF_STREAMING_DATA_USE_JS_BYPASS_FAKE_BUFFERING = new BooleanSetting("revanced_spoof_streaming_data_use_js_bypass_fake_buffering", FALSE, true, new ClientJSAvailability());

    // Client type must be last spoof setting due to cyclic references.
    public static final EnumSetting<ClientType> SPOOF_STREAMING_DATA_DEFAULT_CLIENT = new EnumSetting<>("revanced_spoof_streaming_data_default_client",
            ClientType.ANDROID_VR, true, parent(SPOOF_STREAMING_DATA));

    public static final BooleanSetting DISABLE_AUTO_AUDIO_TRACKS = new BooleanSetting("revanced_disable_auto_audio_tracks", TRUE);

    /**
     * These settings are used by YouTube and YouTube Music.
     */
    public static final BooleanSetting ENABLE_COMMENTS_SCROLL_TOP = new BooleanSetting("revanced_enable_comments_scroll_top", FALSE, true);
    public static final BooleanSetting HIDE_FULLSCREEN_ADS = new BooleanSetting("revanced_hide_fullscreen_ads", TRUE, true);
    public static final BooleanSetting HIDE_PROMOTION_ALERT_BANNER = new BooleanSetting("revanced_hide_promotion_alert_banner", TRUE);

    public static final BooleanSetting DISABLE_AUTO_CAPTIONS = new BooleanSetting("revanced_disable_auto_captions", FALSE, true);
    public static final BooleanSetting DISABLE_DRC_AUDIO = new BooleanSetting("revanced_disable_drc_audio", FALSE, true);
    public static final BooleanSetting DISABLE_QUIC_PROTOCOL = new BooleanSetting("revanced_disable_quic_protocol", FALSE, true);
    public static final BooleanSetting ENABLE_OPUS_CODEC = new BooleanSetting("revanced_enable_opus_codec", TRUE, true);

    public static final BooleanSetting BYPASS_IMAGE_REGION_RESTRICTIONS = new BooleanSetting("revanced_bypass_image_region_restrictions", FALSE, true);
    public static final EnumSetting<WatchHistoryType> WATCH_HISTORY_TYPE = new EnumSetting<>("revanced_watch_history_type", WatchHistoryType.REPLACE);

    public static final BooleanSetting RETURN_YOUTUBE_USERNAME_ENABLED = new BooleanSetting("revanced_return_youtube_username_enabled", FALSE, true);
    public static final EnumSetting<DisplayFormat> RETURN_YOUTUBE_USERNAME_DISPLAY_FORMAT = new EnumSetting<>("revanced_return_youtube_username_display_format", DisplayFormat.USERNAME_ONLY,
            true, parent(RETURN_YOUTUBE_USERNAME_ENABLED));
    public static final StringSetting RETURN_YOUTUBE_USERNAME_YOUTUBE_DATA_API_V3_DEVELOPER_KEY = new StringSetting("revanced_return_youtube_username_youtube_data_api_v3_developer_key", "",
            true, false, null, parent(RETURN_YOUTUBE_USERNAME_ENABLED));

    public static final BooleanSetting SETTINGS_SEARCH_HISTORY = new BooleanSetting("revanced_settings_search_history", TRUE, true);
    public static final StringSetting SETTINGS_SEARCH_ENTRIES = new StringSetting("revanced_settings_search_entries", "", true);

    /**
     * @noinspection DeprecatedIsStillUsed
     */
    @Deprecated
    // The official ReVanced does not offer this, so it has been removed from the settings only. Users can still access settings through import / export settings.
    public static final StringSetting BYPASS_IMAGE_REGION_RESTRICTIONS_DOMAIN = new StringSetting("revanced_bypass_image_region_restrictions_domain", "yt4.ggpht.com", true);

    public static final BooleanSetting SANITIZE_SHARING_LINKS = new BooleanSetting("revanced_sanitize_sharing_links", TRUE, true);
}
