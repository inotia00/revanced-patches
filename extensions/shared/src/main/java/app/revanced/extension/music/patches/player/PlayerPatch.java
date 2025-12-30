package app.revanced.extension.music.patches.player;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.hideViewByRemovingFromParentUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.runOnMainThreadDelayed;

import android.graphics.Color;
import android.view.View;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoType;
import app.revanced.extension.music.utils.VideoUtils;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class PlayerPatch {
    private static final boolean ADD_MINIPLAYER_NEXT_BUTTON =
            Settings.ADD_MINIPLAYER_NEXT_BUTTON.get();
    private static final boolean ADD_MINIPLAYER_PREVIOUS_BUTTON =
            Settings.ADD_MINIPLAYER_PREVIOUS_BUTTON.get();
    private static final boolean CHANGE_PLAYER_BACKGROUND_COLOR =
            Settings.CHANGE_PLAYER_BACKGROUND_COLOR.get();
    private static final boolean CHANGE_SEEK_BAR_POSITION =
            Settings.CHANGE_SEEK_BAR_POSITION.get();
    private static final boolean DISABLE_PLAYER_GESTURE =
            Settings.DISABLE_PLAYER_GESTURE.get();
    private static final boolean ENABLE_SMOOTH_TRANSITION_ANIMATION =
            Settings.ENABLE_SMOOTH_TRANSITION_ANIMATION.get();
    private static final boolean ENABLE_SWIPE_TO_DISMISS_MINIPLAYER =
            Settings.ENABLE_SWIPE_TO_DISMISS_MINIPLAYER.get();
    private static final boolean ENABLE_THICK_SEEKBAR =
            Settings.ENABLE_THICK_SEEKBAR.get();
    private static final boolean ENABLE_ZEN_MODE =
            Settings.ENABLE_ZEN_MODE.get();
    private static final boolean ENABLE_ZEN_MODE_PODCAST =
            Settings.ENABLE_ZEN_MODE_PODCAST.get();
    private static final boolean HIDE_DOUBLE_TAP_OVERLAY_FILTER =
            Settings.HIDE_DOUBLE_TAP_OVERLAY_FILTER.get();
    private static final boolean HIDE_FULLSCREEN_SHARE_BUTTON =
            Settings.HIDE_FULLSCREEN_SHARE_BUTTON.get();
    private static final boolean HIDE_SONG_VIDEO_TOGGLE =
            Settings.HIDE_SONG_VIDEO_TOGGLE.get();
    private static final boolean RESTORE_OLD_COMMENTS_POPUP_PANELS =
            Settings.RESTORE_OLD_COMMENTS_POPUP_PANELS.get();
    private static final boolean SETTINGS_INITIALIZED =
            Settings.SETTINGS_INITIALIZED.get();

    private static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY =
            Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY;
    private static final StringSetting CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY =
            Settings.CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY;

    private static final int ZEN_MODE_BACKGROUND_COLOR = 0xFF404040;
    private static final int MUSIC_VIDEO_BACKGROUND_COLOR = 0xFF030303;

    private static final int[] MUSIC_VIDEO_GRADIENT_COLORS = {MUSIC_VIDEO_BACKGROUND_COLOR, MUSIC_VIDEO_BACKGROUND_COLOR};
    private static final int[] ZEN_MODE_GRADIENT_COLORS = {ZEN_MODE_BACKGROUND_COLOR, ZEN_MODE_BACKGROUND_COLOR};
    private static final int[] customColorGradient = new int[2];
    private static boolean colorInitalized = false;

    private static WeakReference<View> previousButtonViewRef = new WeakReference<>(null);
    private static WeakReference<View> nextButtonViewRef = new WeakReference<>(null);

    static {
        if (CHANGE_PLAYER_BACKGROUND_COLOR)
            loadPlayerbackgroundColor();
    }

    private static void loadPlayerbackgroundColor() {
        try {
            customColorGradient[0] = Color.parseColor(CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY.get());
            customColorGradient[1] = Color.parseColor(CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY.get());
            colorInitalized = true;
        } catch (Exception ex) {
            Utils.showToastShort(str("revanced_custom_player_background_invalid_toast"));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            CUSTOM_PLAYER_BACKGROUND_COLOR_PRIMARY.resetToDefault();
            CUSTOM_PLAYER_BACKGROUND_COLOR_SECONDARY.resetToDefault();

            loadPlayerbackgroundColor();
        }
    }

    public static boolean addMiniPlayerNextButton(boolean original) {
        return !ADD_MINIPLAYER_NEXT_BUTTON && original;
    }

    public static boolean changeMiniPlayerColor() {
        return Settings.CHANGE_MINIPLAYER_COLOR.get();
    }

    public static int[] changePlayerBackgroundColor(int[] colors) {
        if (Arrays.equals(MUSIC_VIDEO_GRADIENT_COLORS, colors)) {
            final VideoType videoType = VideoType.getCurrent();
            final boolean isZenMode = ENABLE_ZEN_MODE &&
                    (videoType.isMusicVideo() || (videoType.isPodCast() && ENABLE_ZEN_MODE_PODCAST));
            if (isZenMode) {
                return ZEN_MODE_GRADIENT_COLORS;
            }
        }
        if (CHANGE_PLAYER_BACKGROUND_COLOR && colorInitalized) {
            return customColorGradient;
        }

        return colors;
    }

    public static boolean changeSeekBarPosition(boolean original) {
        return SETTINGS_INITIALIZED
                ? CHANGE_SEEK_BAR_POSITION
                : original;
    }

    public static boolean disableMiniPlayerGesture() {
        return Settings.DISABLE_MINIPLAYER_GESTURE.get();
    }

    public static boolean disablePlayerGesture() {
        return DISABLE_PLAYER_GESTURE;
    }

    public static boolean enableForcedMiniPlayer(boolean original) {
        return Settings.ENABLE_FORCED_MINIPLAYER.get() || original;
    }

    public static View[] getViewArray(View[] oldViewArray) {
        View previousButtonView = previousButtonViewRef.get();
        if (previousButtonView != null) {
            oldViewArray = ArrayUtils.add(oldViewArray, previousButtonView);
            View nextButtonView = nextButtonViewRef.get();
            if (nextButtonView != null) {
                oldViewArray = ArrayUtils.add(oldViewArray, nextButtonView);
            }
        }
        return oldViewArray;
    }

    public static void setNextButtonView(View nextButtonView) {
        nextButtonViewRef = new WeakReference<>(nextButtonView);
    }

    public static void setNextButtonOnClickListener(View nextButtonView) {
        if (nextButtonView != null) {
            hideViewUnderCondition(
                    !ADD_MINIPLAYER_NEXT_BUTTON,
                    nextButtonView
            );

            nextButtonView.setOnClickListener(v -> nextButtonClicked(nextButtonView));
        }
    }

    // rest of the implementation added by patch.
    private static void nextButtonClicked(View view) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "next button clicked: " + view);
    }

    public static void setPreviousButtonView(View previousButtonView) {
        previousButtonViewRef = new WeakReference<>(previousButtonView);
    }

    public static void setPreviousButtonOnClickListener(View previousButtonView) {
        if (previousButtonView != null) {
            hideViewUnderCondition(
                    !ADD_MINIPLAYER_PREVIOUS_BUTTON,
                    previousButtonView
            );

            previousButtonView.setOnClickListener(v -> previousButtonClicked(previousButtonView));
        }
    }

    // rest of the implementation added by patch.
    private static void previousButtonClicked(View view) {
        // These instructions are ignored by patch.
        Logger.printDebug(() -> "previous button clicked: " + view);
    }

    public static boolean enableSmoothTransitionAnimation(boolean original) {
        return SETTINGS_INITIALIZED
                ? ENABLE_SMOOTH_TRANSITION_ANIMATION
                : original;
    }

    public static boolean enableSmoothTransitionAnimationInverted(boolean original) {
        return SETTINGS_INITIALIZED
                ? !ENABLE_SMOOTH_TRANSITION_ANIMATION
                : original;
    }

    public static boolean enableSwipeToDismissMiniPlayer() {
        return ENABLE_SWIPE_TO_DISMISS_MINIPLAYER;
    }

    public static boolean enableSwipeToDismissMiniPlayer(boolean original) {
        return !ENABLE_SWIPE_TO_DISMISS_MINIPLAYER && original;
    }

    public static Object enableSwipeToDismissMiniPlayer(Object object) {
        return ENABLE_SWIPE_TO_DISMISS_MINIPLAYER ? null : object;
    }

    public static boolean enableThickSeekBar(boolean original) {
        return SETTINGS_INITIALIZED
                ? ENABLE_THICK_SEEKBAR
                : original;
    }

    public static int enableZenMode(int originalColor) {
        if (ENABLE_ZEN_MODE && originalColor == MUSIC_VIDEO_BACKGROUND_COLOR) {
            final VideoType videoType = VideoType.getCurrent();
            if (videoType.isMusicVideo() || (videoType.isPodCast() && ENABLE_ZEN_MODE_PODCAST)) {
                return ZEN_MODE_BACKGROUND_COLOR;
            }
        }
        return originalColor;
    }

    public static boolean hideSongVideoToggle(boolean original) {
        return HIDE_SONG_VIDEO_TOGGLE && original;
    }

    public static void hideSongVideoToggle(View view, int originalVisibility) {
        view.setVisibility(
                HIDE_SONG_VIDEO_TOGGLE
                        ? View.GONE
                        : originalVisibility
        );
    }

    public static void hideDoubleTapOverlayFilter(View view) {
        hideViewByRemovingFromParentUnderCondition(HIDE_DOUBLE_TAP_OVERLAY_FILTER, view);
    }

    public static int hideFullscreenShareButton(int original) {
        return HIDE_FULLSCREEN_SHARE_BUTTON ? 0 : original;
    }

    public static void setShuffleState(Enum<?> shuffleState) {
        if (Settings.REMEMBER_SHUFFLE_SATE.get()) {
            Settings.ALWAYS_SHUFFLE.save(shuffleState.ordinal() == 1);
        }
    }

    public static void shuffleTracks() {
        shuffleTracks(false);
    }

    public static void shuffleTracksWithDelay() {
        shuffleTracks(true);
    }

    private static void shuffleTracks(boolean needDelay) {
        if (!Settings.ALWAYS_SHUFFLE.get())
            return;

        if (needDelay) {
            runOnMainThreadDelayed(VideoUtils::shuffleTracks, 1000);
        } else {
            VideoUtils.shuffleTracks();
        }
    }

    public static boolean rememberRepeatState(boolean original) {
        return Settings.REMEMBER_REPEAT_SATE.get() || original;
    }

    public static boolean rememberShuffleState() {
        return Settings.REMEMBER_SHUFFLE_SATE.get();
    }

    public static boolean restoreOldCommentsPopUpPanels() {
        return restoreOldCommentsPopUpPanels(true);
    }

    public static boolean restoreOldCommentsPopUpPanels(boolean original) {
        return SETTINGS_INITIALIZED
                ? !RESTORE_OLD_COMMENTS_POPUP_PANELS && original
                : original;
    }

    public static boolean restoreOldPlayerBackground(boolean original) {
        if (!SETTINGS_INITIALIZED) {
            return original;
        }
        if (!isSDKAbove(23)) {
            // Disable this patch on Android 5.0 / 5.1 to fix a black play button.
            // Android 5.x have a different design for play button,
            // and if the new background is applied forcibly, the play button turns black.
            // 6.20.51 uses the old background from the beginning, so there is no impact.
            return original;
        }
        return !Settings.RESTORE_OLD_PLAYER_BACKGROUND.get();
    }

    public static boolean restoreOldPlayerLayout(boolean original) {
        if (!SETTINGS_INITIALIZED) {
            return original;
        }
        return !Settings.RESTORE_OLD_PLAYER_LAYOUT.get();
    }

}
