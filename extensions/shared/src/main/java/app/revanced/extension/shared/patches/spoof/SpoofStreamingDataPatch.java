package app.revanced.extension.shared.patches.spoof;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.shared.innertube.client.YouTubeAppClient.ClientType;
import app.revanced.extension.shared.patches.PatchStatus;
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class SpoofStreamingDataPatch extends BlockRequestPatch {
    private static final boolean SPOOF_STREAMING_DATA_SKIP_RESPONSE_ENCRYPTION =
            SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_SKIP_RESPONSE_ENCRYPTION.get();
    private static final boolean SPOOF_STREAMING_DATA_TYPE_IOS =
            PatchStatus.SpoofStreamingDataIOS() && BaseSettings.SPOOF_STREAMING_DATA_TYPE_IOS.get();

    /**
     * Any unreachable ip address.  Used to intentionally fail requests.
     */
    private static final String UNREACHABLE_HOST_URI_STRING = "https://127.0.0.0";
    private static final Uri UNREACHABLE_HOST_URI = Uri.parse(UNREACHABLE_HOST_URI_STRING);

    /**
     * Key: video id
     * Value: original video length [streamingData.formats.approxDurationMs]
     */
    private static final Map<String, Long> approxDurationMsMap = Collections.synchronizedMap(
            new LinkedHashMap<>(1) {
                private static final int CACHE_LIMIT = 10;

                @Override
                protected boolean removeEldestEntry(Entry eldest) {
                    return size() >= CACHE_LIMIT; // Evict the oldest entry if over the cache limit.
                }
            });

    /**
     * Injection point.
     */
    public static boolean isSpoofingEnabled() {
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
     * Skip response encryption in OnesiePlayerRequest.
     */
    public static boolean skipResponseEncryption(boolean original) {
        if (!SPOOF_STREAMING_DATA_SKIP_RESPONSE_ENCRYPTION) {
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
            String id = Utils.getVideoIdFromRequest(url);
            if (id == null) {
                Logger.printException(() -> "Ignoring request with no id: " + url);
                return;
            } else if (id.isEmpty()) {
                return;
            }

            StreamingDataRequest.fetchRequest(id, requestHeader);
        }
    }

    /**
     * Injection point.
     * Fix playback by replace the streaming data.
     * Called after {@link #fetchStreams(String, Map)}.
     */
    @Nullable
    public static ByteBuffer getStreamingData(String videoId) {
        if (SPOOF_STREAMING_DATA) {
            try {
                StreamingDataRequest request = StreamingDataRequest.getRequestForVideoId(videoId);
                if (request != null) {
                    // This hook is always called off the main thread,
                    // but this can later be called for the same video id from the main thread.
                    // This is not a concern, since the fetch will always be finished
                    // and never block the main thread.
                    // But if debugging, then still verify this is the situation.
                    if (BaseSettings.ENABLE_DEBUG_LOGGING.get() && !request.fetchCompleted() && Utils.isCurrentlyOnMainThread()) {
                        Logger.printException(() -> "Error: Blocking main thread");
                    }

                    var stream = request.getStream();
                    if (stream != null) {
                        Logger.printDebug(() -> "Overriding video stream: " + videoId);
                        return stream;
                    }
                }

                Logger.printDebug(() -> "Not overriding streaming data (video stream is null): " + videoId);
            } catch (Exception ex) {
                Logger.printException(() -> "getStreamingData failure", ex);
            }
        }

        return null;
    }

    /**
     * Injection point.
     * <p>
     * If spoofed [streamingData.formats] is empty,
     * Put the original [streamingData.formats.approxDurationMs] into the HashMap.
     * <p>
     * Called after {@link #getStreamingData(String)}.
     */
    public static void setApproxDurationMs(String videoId, long approxDurationMs) {
        if (SPOOF_STREAMING_DATA && approxDurationMs != Long.MAX_VALUE) {
            approxDurationMsMap.put(videoId, approxDurationMs);
            Logger.printDebug(() -> "New approxDurationMs loaded, video id: " + videoId + ", video length: " + approxDurationMs);
        }
    }

    /**
     * Injection point.
     * <p>
     * When measuring the length of a video in an Android YouTube client,
     * the client first checks if the streaming data contains [streamingData.formats.approxDurationMs].
     * <p>
     * If the streaming data response contains [approxDurationMs] (Long type, actual value), this value will be the video length.
     * <p>
     * If [streamingData.formats] (List type) is empty, the [approxDurationMs] value cannot be accessed,
     * So it falls back to the value of [videoDetails.lengthSeconds] (Integer type, approximate value) multiplied by 1000.
     * <p>
     * For iOS clients, [streamingData.formats] (List type) is always empty, so it always falls back to the approximate value.
     * <p>
     * Called after {@link #getStreamingData(String)}.
     */
    public static long getApproxDurationMs(String videoId) {
        if (SPOOF_STREAMING_DATA && videoId != null) {
            final Long approxDurationMs = approxDurationMsMap.get(videoId);
            if (approxDurationMs != null) {
                Logger.printDebug(() -> "Replacing video length: " + approxDurationMs + " for videoId: " + videoId);
                return approxDurationMs;
            }
        }
        return Long.MAX_VALUE;
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
    public static String appendSpoofedClient(String videoFormat) {
        try {
            if (SPOOF_STREAMING_DATA && BaseSettings.SPOOF_STREAMING_DATA_STATS_FOR_NERDS.get()
                    && !TextUtils.isEmpty(videoFormat)) {
                // Force LTR layout, to match the same LTR video time/length layout YouTube uses for all languages
                return "\u202D" + videoFormat + String.format("\u2009(%s)", StreamingDataRequest.getLastSpoofedClientName()); // u202D = left to right override
            }
        } catch (Exception ex) {
            Logger.printException(() -> "appendSpoofedClient failure", ex);
        }

        return videoFormat;
    }

    public static String[] getEntries() {
        return SPOOF_STREAMING_DATA_TYPE_IOS
                ? ResourceUtils.getStringArray("revanced_spoof_streaming_data_type_ios_entries")
                : ResourceUtils.getStringArray("revanced_spoof_streaming_data_type_entries");
    }

    public static String[] getEntryValues() {
        return SPOOF_STREAMING_DATA_TYPE_IOS
                ? ResourceUtils.getStringArray("revanced_spoof_streaming_data_type_ios_entry_values")
                : ResourceUtils.getStringArray("revanced_spoof_streaming_data_type_entry_values");
    }

    public static final class AudioStreamLanguageOverrideAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return BaseSettings.SPOOF_STREAMING_DATA.get() &&
                    BaseSettings.SPOOF_STREAMING_DATA_TYPE.get() == ClientType.ANDROID_VR_NO_AUTH;
        }
    }
}
