package app.revanced.extension.youtube.patches.shorts;

import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;
import static app.revanced.extension.shared.utils.Utils.validateValue;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.libraries.youtube.rendering.ui.pivotbar.PivotBar;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;
import app.revanced.extension.youtube.shared.ShortsPlayerState;
import app.revanced.extension.youtube.utils.VideoUtils;
import kotlin.Unit;

@SuppressWarnings("unused")
public class ShortsPatch {
    private static final boolean ENABLE_SHORTS_TIME_STAMP =
            Settings.ENABLE_SHORTS_TIME_STAMP.get();
    private static final boolean ENABLE_SHORTS_CLEAR_MODE =
            ENABLE_SHORTS_TIME_STAMP && Settings.ENABLE_SHORTS_CLEAR_MODE.get();
    public static final boolean HIDE_SHORTS_NAVIGATION_BAR =
            Settings.HIDE_SHORTS_NAVIGATION_BAR.get();
    private static final double NAVIGATION_BAR_HEIGHT_PERCENTAGE;
    private static int navigationBarHeight = -1;

    static {
        if (HIDE_SHORTS_NAVIGATION_BAR) {
            ShortsPlayerState.getOnChange().addObserver((ShortsPlayerState state) -> {
                setNavigationBarLayoutParams(state);
                return Unit.INSTANCE;
            });
        }

        final int heightPercentage = validateValue(
                Settings.SHORTS_NAVIGATION_BAR_HEIGHT_PERCENTAGE,
                0,
                100,
                "revanced_shorts_navigation_bar_height_percentage_invalid_toast"
        );

        NAVIGATION_BAR_HEIGHT_PERCENTAGE = heightPercentage / 100d;
    }

    public static boolean disableResumingStartupShortsPlayer() {
        return Settings.DISABLE_RESUMING_SHORTS_PLAYER.get();
    }

    public static boolean disableResumingStartupShortsPlayer(boolean original) {
        return !Settings.DISABLE_RESUMING_SHORTS_PLAYER.get() && original;
    }

    public static boolean enableShortsTimeStamp(boolean original) {
        return ENABLE_SHORTS_TIME_STAMP || original;
    }

    // If this is not overridden, timestamps will not be enabled on Shorts played on the channel.
    public static int enableShortsTimeStamp(int original) {
        return ENABLE_SHORTS_TIME_STAMP ? 10010 : original;
    }

    public static boolean enableShortsTimeStampReverse(boolean original) {
        return !ENABLE_SHORTS_TIME_STAMP && original;
    }

    public static boolean enableShortsClearMode(boolean original) {
        return ENABLE_SHORTS_CLEAR_MODE || original;
    }

    public static void hideShortsCommentsButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_COMMENTS_BUTTON.get(), view);
    }

    public static boolean hideShortsDislikeButton() {
        return Settings.HIDE_SHORTS_DISLIKE_BUTTON.get();
    }

    public static ViewGroup hideShortsInfoPanel(ViewGroup viewGroup) {
        return Settings.HIDE_SHORTS_INFO_PANEL.get() ? null : viewGroup;
    }

    public static boolean hideShortsLikeButton() {
        return Settings.HIDE_SHORTS_LIKE_BUTTON.get();
    }

    public static void hideShortsRemixButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_REMIX_BUTTON.get(), view);
    }

    public static void hideShortsShareButton(View view) {
        hideViewUnderCondition(Settings.HIDE_SHORTS_SHARE_BUTTON.get(), view);
    }

    public static boolean hideShortsSoundButton() {
        return Settings.HIDE_SHORTS_SOUND_BUTTON.get();
    }

    private static final int zeroPaddingDimenId =
            ResourceUtils.getDimenIdentifier("revanced_zero_padding");

    public static int getShortsSoundButtonDimenId(int dimenId) {
        return Settings.HIDE_SHORTS_SOUND_BUTTON.get()
                ? zeroPaddingDimenId
                : dimenId;
    }

    public static int hideShortsSubscribeButton(int original) {
        return Settings.HIDE_SHORTS_SUBSCRIBE_BUTTON.get() ? 0 : original;
    }

    // YouTube 18.29.38 ~ YouTube 19.28.42
    public static boolean hideShortsPausedHeader() {
        return Settings.HIDE_SHORTS_PAUSED_HEADER.get();
    }

    // YouTube 19.29.42 ~
    public static boolean hideShortsPausedHeader(boolean original) {
        return Settings.HIDE_SHORTS_PAUSED_HEADER.get() || original;
    }

    public static boolean hideShortsToolBar(boolean original) {
        return !Settings.HIDE_SHORTS_TOOLBAR.get() && original;
    }

    /**
     * BottomBarContainer is the parent view of {@link PivotBar},
     * And can be hidden using {@link View#setVisibility} only when it is initialized.
     * <p>
     * If it was not hidden with {@link View#setVisibility} when it was initialized,
     * it should be hidden with {@link FrameLayout.LayoutParams}.
     * <p>
     * When Shorts is opened, {@link FrameLayout.LayoutParams} should be changed to 0dp,
     * When Shorts is closed, {@link FrameLayout.LayoutParams} should be changed to the original.
     */
    private static WeakReference<View> bottomBarContainerRef = new WeakReference<>(null);

    private static FrameLayout.LayoutParams originalLayoutParams;
    private static final FrameLayout.LayoutParams zeroLayoutParams =
            new FrameLayout.LayoutParams(0, 0);

    public static void setNavigationBar(View view) {
        if (!HIDE_SHORTS_NAVIGATION_BAR) {
            return;
        }
        bottomBarContainerRef = new WeakReference<>(view);
        if (!(view.getLayoutParams() instanceof FrameLayout.LayoutParams lp)) {
            return;
        }
        if (originalLayoutParams == null) {
            originalLayoutParams = lp;
        }
    }

    public static int setNavigationBarHeight(int original) {
        if (HIDE_SHORTS_NAVIGATION_BAR) {
            if (navigationBarHeight == -1) {
                navigationBarHeight = (int) Math.round(original * NAVIGATION_BAR_HEIGHT_PERCENTAGE);
            }
            return navigationBarHeight;
        }
        return original;
    }

    private static void setNavigationBarLayoutParams(@NonNull ShortsPlayerState shortsPlayerState) {
        final View navigationBar = bottomBarContainerRef.get();
        if (navigationBar == null) {
            return;
        }
        if (!(navigationBar.getLayoutParams() instanceof FrameLayout.LayoutParams lp)) {
            return;
        }
        navigationBar.setLayoutParams(
                shortsPlayerState.isClosed()
                        ? originalLayoutParams
                        : zeroLayoutParams
        );
    }

    public static boolean restoreShortsOldPlayerLayout() {
        return !Settings.RESTORE_SHORTS_OLD_PLAYER_LAYOUT.get();
    }

    public static boolean openShortInRegularPlayer(String videoId) {
        try {
            if (!Settings.OPEN_SHORTS_IN_REGULAR_PLAYER.get()) {
                return false; // Default unpatched behavior.
            }

            if (videoId.isEmpty()) {
                // Shorts was opened using launcher app shortcut.
                //
                // This check will not detect if the Shorts app shortcut is used
                // while the app is running in the background (instead the regular player is opened).
                // To detect that the hooked method map parameter can be checked
                // if integer key 'com.google.android.apps.youtube.app.endpoint.flags'
                // has bitmask 16 set.
                //
                // This use case seems unlikely if the user has the Shorts
                // set to open in the regular player, so it's ignored as
                // checking the map makes the patch more complicated.
                Logger.printDebug(() -> "Ignoring Short with no videoId");
                return false;
            }

            if (NavigationButton.getSelectedNavigationButton() == NavigationButton.SHORTS) {
                return false; // Always use Shorts player for the Shorts nav button.
            }

            VideoUtils.openVideo(videoId, true);
            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "openShortInRegularPlayer failure", ex);
            return false;
        }
    }

}
