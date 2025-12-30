package app.revanced.extension.music.patches.misc;

import android.view.View;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.music.patches.misc.requests.PlaylistRequest;
import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoInformation;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class AlbumMusicVideoPatch {

    public enum RedirectType {
        REDIRECT,
        ON_CLICK,
        ON_LONG_CLICK
    }

    private static final RedirectType REDIRECT_TYPE =
            Settings.DISABLE_MUSIC_VIDEO_IN_ALBUM_REDIRECT_TYPE.get();

    private static final boolean DISABLE_MUSIC_VIDEO_IN_ALBUM =
            Settings.DISABLE_MUSIC_VIDEO_IN_ALBUM.get();

    private static final boolean REDIRECT = REDIRECT_TYPE == RedirectType.REDIRECT;

    private static final boolean ON_CLICK = REDIRECT_TYPE == RedirectType.ON_CLICK;

    private static final boolean ON_LONG_CLICK = REDIRECT_TYPE == RedirectType.ON_LONG_CLICK;

    private static final String YOUTUBE_MUSIC_ALBUM_PREFIX = "OLAK";

    private static volatile boolean isVideoLaunched = false;

    @NonNull
    private static volatile String playerResponseVideoId = "";

    @NonNull
    private static volatile String currentVideoId = "";

    @GuardedBy("itself")
    private static final Map<String, String> lastVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 10;

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    /**
     * Injection point.
     */
    public static void newPlayerResponse(@NonNull String videoId, @NonNull String playlistId, final int playlistIndex) {
        if (!DISABLE_MUSIC_VIDEO_IN_ALBUM) {
            return;
        }
        if (!playlistId.startsWith(YOUTUBE_MUSIC_ALBUM_PREFIX)) {
            return;
        }
        if (playlistIndex < 0) {
            return;
        }
        if (playerResponseVideoId.equals(videoId)) {
            return;
        }
        playerResponseVideoId = videoId;

        // Fetch.
        PlaylistRequest.fetchRequestIfNeeded(videoId, playlistId, playlistIndex);
    }

    /**
     * Injection point.
     */
    public static void newVideoLoaded(@NonNull String videoId) {
        if (!DISABLE_MUSIC_VIDEO_IN_ALBUM) {
            return;
        }
        if (currentVideoId.equals(videoId)) {
            return;
        }
        currentVideoId = videoId;
        checkVideo(videoId);
    }

    private static void checkVideo(@NonNull String videoId) {
        try {
            PlaylistRequest request = PlaylistRequest.getRequestForVideoId(videoId);
            if (request == null) {
                return;
            }
            String songId = request.getSongId();
            if (songId.isEmpty()) {
                Logger.printDebug(() -> "Official song not found, videoId: " + videoId);
                return;
            }
            synchronized (lastVideoIds) {
                if (lastVideoIds.put(videoId, songId) == null) {
                    Logger.printDebug(() -> "Official song found, videoId: " + videoId + ", songId: " + songId);
                    if (REDIRECT) {
                        openMusic(songId);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "check failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static boolean openMusic() {
        if (DISABLE_MUSIC_VIDEO_IN_ALBUM && ON_CLICK) {
            try {
                String videoId = VideoInformation.getVideoId();
                synchronized (lastVideoIds) {
                    String songId = lastVideoIds.get(videoId);
                    if (songId != null) {
                        openMusic(songId);
                        return true;
                    }
                }
            } catch (Exception ex) {
                Logger.printException(() -> "openMusic failure", ex);
            }
        }
        return false;
    }

    private static void openMusic(@NonNull String songId) {
        try {
            // The newly opened video is not a music video.
            // To prevent fetch requests from being sent, set the video id to the newly opened video
            VideoUtils.runOnMainThreadDelayed(() -> {
                isVideoLaunched = true;
                playerResponseVideoId = songId;
                currentVideoId = songId;
                VideoUtils.openInYouTubeMusic(songId);
                VideoUtils.runOnMainThreadDelayed(() -> isVideoLaunched = false, 3000);
            }, 1500);

        } catch (Exception ex) {
            Logger.printException(() -> "openMusic failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void setAudioVideoSwitchToggleOnLongClickListener(View view) {
        if (DISABLE_MUSIC_VIDEO_IN_ALBUM && ON_LONG_CLICK) {
            view.setOnLongClickListener(v -> {
                try {
                    String videoId = VideoInformation.getVideoId();
                    synchronized (lastVideoIds) {
                        String songId = lastVideoIds.get(videoId);
                        if (songId != null) {
                            openMusic(songId);
                        }
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "onLongClickListener failure", ex);
                }
                return true;
            });
        }
    }

    /**
     * Injection point.
     */
    public static boolean hideSnackBar() {
        return DISABLE_MUSIC_VIDEO_IN_ALBUM && isVideoLaunched;
    }

}
