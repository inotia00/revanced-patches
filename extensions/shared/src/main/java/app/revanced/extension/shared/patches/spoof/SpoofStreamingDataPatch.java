package app.revanced.extension.shared.patches.spoof;

import static app.revanced.extension.shared.innertube.utils.J2V8Support.supportJ2V8;
import static app.revanced.extension.shared.innertube.utils.StreamingDataOuterClassUtils.prioritizeResolution;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protos.youtube.api.innertube.StreamingDataOuterClass.StreamingData;
import com.liskovsoft.googlecommon.common.helpers.YouTubeHelper;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import app.revanced.extension.shared.innertube.client.YouTubeClient.ClientType;
import app.revanced.extension.shared.innertube.utils.ThrottlingParameterUtils;
import app.revanced.extension.shared.patches.PatchStatus;
import app.revanced.extension.shared.patches.auth.AuthPatch;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.shared.VideoInformation;

@SuppressWarnings("unused")
public class SpoofStreamingDataPatch {
    public static final boolean SPOOF_STREAMING_DATA =
            BaseSettings.SPOOF_STREAMING_DATA.get() && PatchStatus.SpoofStreamingData();
    private static final boolean J2V8_LIBRARY_AVAILABILITY = supportJ2V8();
    private static final boolean SPOOF_STREAMING_DATA_PRIORITIZE_VIDEO_QUALITY =
            SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_PRIORITIZE_VIDEO_QUALITY.get();
    private static final boolean SPOOF_STREAMING_DATA_USE_JS =
            SPOOF_STREAMING_DATA && J2V8_LIBRARY_AVAILABILITY && BaseSettings.SPOOF_STREAMING_DATA_USE_JS.get();
    private static final boolean SPOOF_STREAMING_DATA_USE_JS_ALL =
            SPOOF_STREAMING_DATA_USE_JS && BaseSettings.SPOOF_STREAMING_DATA_USE_JS_ALL.get();

    /**
     * Domain used for internet connectivity verification.
     * It has an empty response body and is only used to check for a 204 response code.
     * <p>
     * If an unreachable IP address (127.0.0.1) is used, no response code is provided.
     * <p>
     * YouTube handles unreachable IP addresses without issue.
     * YouTube Music has an issue with waiting for the Cronet connect timeout of 30_000L on mobile networks.
     * <p>
     * Using a VPN or DNS can temporarily resolve this issue,
     * But the ideal workaround is to avoid using unreachable IP addresses.
     */
    private static final String INTERNET_CONNECTION_CHECK_URI_STRING =
            "https://www.google.com/gen_204";
    private static final Uri INTERNET_CONNECTION_CHECK_URI =
            Uri.parse(INTERNET_CONNECTION_CHECK_URI_STRING);

    /**
     * Parameters used when playing scrim.
     */
    private static final String SCRIM_PARAMETERS = "SAFgAXgB";
    /**
     * Parameters used when playing clips.
     */
    private static final String CLIPS_PARAMETERS = "kAIB";
    /**
     * Prefix present in all Short player parameters signature.
     */
    private static final String SHORTS_PLAYER_PARAMETERS = "8AEB";
    @NonNull
    private static volatile String playerResponseCpn = "";
    @Nullable
    private static volatile String playerResponseParameter = null;

    /**
     * Injection point.
     * /att/get requests are used to obtain a PoToken challenge.
     * See: <a href="https://github.com/FreeTubeApp/FreeTube/blob/4b7208430bc1032019a35a35eb7c8a84987ddbd7/src/botGuardScript.js#L15">botGuardScript.js#L15</a>
     * <p>
     * Since the Spoof streaming data patch was implemented because a valid PoToken cannot be obtained,
     * Blocking /att/get requests are not a problem.
     */
    public static String blockGetAttRequest(String originalUrlString) {
        if (SPOOF_STREAMING_DATA) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("att/get")) {
                    Logger.printDebug(() -> "Blocking 'att/get' by returning internet connection check uri");

                    return INTERNET_CONNECTION_CHECK_URI_STRING;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetAttRequest failure", ex);
            }
        }

        return originalUrlString;
    }

    /**
     * Injection point.
     * Blocks /get_watch requests by returning an internet connection check URI.
     *
     * @param playerRequestUri The URI of the player request.
     * @return An internet connection check URI if the request is a /get_watch request, otherwise the original URI.
     */
    public static Uri blockGetWatchRequest(Uri playerRequestUri) {
        if (SPOOF_STREAMING_DATA) {
            try {
                String path = playerRequestUri.getPath();

                if (path != null && path.contains("get_watch")) {
                    Logger.printDebug(() -> "Blocking 'get_watch' by returning internet connection check uri");

                    return INTERNET_CONNECTION_CHECK_URI;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockGetWatchRequest failure", ex);
            }
        }

        return playerRequestUri;
    }

    /**
     * Injection point.
     * <p>
     * Blocks /initplayback requests.
     */
    public static String blockInitPlaybackRequest(String originalUrlString) {
        if (SPOOF_STREAMING_DATA) {
            try {
                var originalUri = Uri.parse(originalUrlString);
                String path = originalUri.getPath();

                if (path != null && path.contains("initplayback")) {
                    Logger.printDebug(() -> "Blocking 'initplayback' by returning internet connection check uri");

                    return INTERNET_CONNECTION_CHECK_URI_STRING;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "blockInitPlaybackRequest failure", ex);
            }
        }

        return originalUrlString;
    }

    /**
     * Injection point.
     * Fix audio stuttering in YouTube Music.
     */
    public static boolean disableSABR() {
        return SPOOF_STREAMING_DATA;
    }

    /**
     * Injection point.
     * This method is only invoked when playing a livestream on an iOS client.
     */
    public static boolean fixHLSCurrentTime(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     * Turns off a feature flag that interferes with spoofing.
     */
    public static boolean useMediaFetchHotConfigReplacement(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     * Turns off a feature flag that interferes with video playback.
     */
    public static boolean usePlaybackStartFeatureFlag(boolean original) {
        if (!SPOOF_STREAMING_DATA) {
            return original;
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static void fetchStreams(String url, Map<String, String> requestHeader) {
        if (SPOOF_STREAMING_DATA) {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            if (path == null || !path.contains("player")) {
                return;
            }
            String id = uri.getQueryParameter("id");
            if (StringUtils.isEmpty(id)) {
                return;
            }
            String tParameter = YouTubeHelper.generateTParameter(uri.getQueryParameter("t"));
            String cpn = YouTubeHelper.generateContentPlaybackNonce(playerResponseCpn);
            String reasonSkipped;
            if (playerResponseParameter != null &&
                    SPOOF_STREAMING_DATA_USE_JS &&
                    !SPOOF_STREAMING_DATA_USE_JS_ALL) {
                String playerParameter = Objects.requireNonNull(playerResponseParameter);
                if (playerParameter.startsWith(SHORTS_PLAYER_PARAMETERS)) {
                    reasonSkipped = "Shorts";
                } else if ("1".equals(uri.getQueryParameter("inline")) || playerParameter.equals(SCRIM_PARAMETERS)) {
                    reasonSkipped = "Autoplay in feed";
                } else if (playerParameter.length() > 150 || playerParameter.startsWith(CLIPS_PARAMETERS)) {
                    reasonSkipped = "Clips";
                } else {
                    reasonSkipped = "";
                }
            } else {
                reasonSkipped = "";
            }

            AuthPatch.checkAccessToken();
            StreamingDataRequest.fetchRequest(
                    id,
                    tParameter,
                    cpn,
                    requestHeader,
                    reasonSkipped
            );
        }
    }

    public static boolean isValidVideoId(@Nullable String videoId) {
        return videoId != null && !videoId.isEmpty() && !"zzzzzzzzzzz".equals(videoId);
    }

    /**
     * Injection point.
     * Fix playback by replace the streaming data.
     * Called after {@link #fetchStreams(String, Map)}.
     */
    public static StreamingData getStreamingData(@NonNull String videoId) {
        if (SPOOF_STREAMING_DATA && isValidVideoId(videoId)) {
            try {
                StreamingDataRequest request = StreamingDataRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    // This hook is always called off the main thread,
                    // but this can later be called for the same video id from the main thread.
                    // This is not a concern, since the fetch will always be finished
                    // and never block the main thread.
                    // But if debugging, then still verify this is the situation.
                    if (BaseSettings.DEBUG.get() && !request.fetchCompleted() && Utils.isCurrentlyOnMainThread()) {
                        Logger.printException(() -> "Error: Blocking main thread");
                    }

                    var stream = request.getStream();
                    if (stream != null) {
                        Logger.printDebug(() -> "Overriding video stream: " + videoId);
                        return stream;
                    }
                }

                Logger.printDebug(() -> "Not overriding streaming data (video stream is null, it may be video ads): " + videoId);
            } catch (Exception ex) {
                Logger.printException(() -> "getStreamingData failure", ex);
            }
        }

        return null;
    }

    /**
     * Injection point.
     * Called after {@link #getStreamingData(String)}.
     */
    public static List<Object> prioritizeVideoQuality(@Nullable String videoId, @NonNull List<Object> adaptiveFormats) {
        if (SPOOF_STREAMING_DATA_PRIORITIZE_VIDEO_QUALITY && isValidVideoId(videoId)) {
            return prioritizeResolution(adaptiveFormats);
        }

        return adaptiveFormats;
    }

    /**
     * Injection point.
     * Called after {@link #getStreamingData(String)}.
     */
    @Nullable
    public static byte[] removeVideoPlaybackPostBody(Uri uri, int method, byte[] postData) {
        if (SPOOF_STREAMING_DATA) {
            try {
                final int methodPost = 2;
                if (method == methodPost) {
                    String path = uri.getPath();
                    if (path != null && path.contains("videoplayback")) {
                        return null;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "removeVideoPlaybackPostBody failure", ex);
            }
        }

        return postData;
    }

    /**
     * Injection point.
     */
    public static void newPlayerResponseCpn(@Nullable String cpn) {
        if (cpn != null && !cpn.isEmpty()) {
            playerResponseCpn = cpn;
        }
    }

    /**
     * Injection point.
     */
    @Nullable
    public static String newPlayerResponseParameter(@NonNull String newlyLoadedVideoId, @Nullable String playerParameter) {
        return newPlayerResponseParameter(newlyLoadedVideoId, playerParameter, null, false);
    }

    /**
     * Injection point.
     * <p>
     * Since {@link SpoofStreamingDataPatch} is on a shared path,
     * {@link VideoInformation#newPlayerResponseParameter(String, String, String, boolean)} is not used.
     */
    @Nullable
    public static String newPlayerResponseParameter(@NonNull String newlyLoadedVideoId, @Nullable String playerParameter,
                                                    @Nullable String newlyLoadedPlaylistId, boolean isShortAndOpeningOrPlaying) {
        playerResponseParameter = playerParameter;
        return playerParameter; // Return the original value since we are observing and not modifying.
    }

    /**
     * Injection point.
     * <p>
     * It takes about 3-5 seconds to download the JavaScript and initialize the Cipher class.
     * Initialize it before the video starts.
     * Used for {@link ClientType#TV} and {@link ClientType#TV_EMBEDDED}.
     */
    public static void initializeJavascript() {
        if (SPOOF_STREAMING_DATA_USE_JS) {
            // Download JavaScript and initialize the Cipher class
            CompletableFuture.runAsync(() -> ThrottlingParameterUtils.initializeJavascript(
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().getRequirePoToken()
            ));
        }
    }

    /**
     * Injection point.
     */
    public static String appendSpoofedClient(String format) {
        try {
            if (SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_STATS_FOR_NERDS.get()
                    && !TextUtils.isEmpty(format)) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                return "\u202D" + format + String.format("\u2009(%s)", StreamingDataRequest.getLastSpoofedClientName()); // u202D = left to right override
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendSpoofedClient failure", ex);
        }

        return format;
    }

    public static boolean multiAudioTrackAvailable() {
        if (!PatchStatus.SpoofStreamingData()) {
            return true;
        }
        if (!BaseSettings.SPOOF_STREAMING_DATA.get()) {
            return true;
        }
        return BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().getSupportsMultiAudioTracks();
    }

    public static final class ClientAndroidVRAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().name().startsWith("ANDROID_VR");
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(BaseSettings.SPOOF_STREAMING_DATA);
        }
    }

    public static final class ClientJSAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    J2V8_LIBRARY_AVAILABILITY &&
                    BaseSettings.SPOOF_STREAMING_DATA_USE_JS.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().getRequireJS();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(
                    BaseSettings.SPOOF_STREAMING_DATA,
                    BaseSettings.SPOOF_STREAMING_DATA_USE_JS
            );
        }
    }

    public static final class HideAudioFlyoutMenuAvailability implements Setting.Availability {
        private static final boolean AVAILABLE_ON_LAUNCH = SpoofStreamingDataPatch.multiAudioTrackAvailable();

        @Override
        public boolean isAvailable() {
            // Check conditions of launch and now. Otherwise if spoofing is changed
            // without a restart the setting will show as available when it's not.
            return AVAILABLE_ON_LAUNCH && SpoofStreamingDataPatch.multiAudioTrackAvailable();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(BaseSettings.SPOOF_STREAMING_DATA);
        }
    }

    public static final class J2V8Availability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    J2V8_LIBRARY_AVAILABILITY;
        }
    }

    public static final class ShowReloadVideoButtonAvailability implements Setting.Availability {
        private static final boolean AVAILABLE_ON_LAUNCH = BaseSettings.SPOOF_STREAMING_DATA.get() &&
                BaseSettings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON.get();

        @Override
        public boolean isAvailable() {
            // Check conditions of launch and now. Otherwise if spoofing is changed
            // without a restart the setting will show as available when it's not.
            return AVAILABLE_ON_LAUNCH && BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON.get();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(
                    BaseSettings.SPOOF_STREAMING_DATA,
                    BaseSettings.SPOOF_STREAMING_DATA_RELOAD_VIDEO_BUTTON
            );
        }
    }
}
