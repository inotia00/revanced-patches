package app.revanced.extension.youtube.patches.shorts;

import android.app.Activity;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public class ShortsRepeatStatePatch {

    public enum ShortsLoopBehavior {
        UNKNOWN,
        /**
         * Play once, then advanced to the next Short.
         */
        SINGLE_PLAY,
        /**
         * Repeat the same Short forever!
         */
        REPEAT,
        /**
         * Pause playback after 1 play.
         */
        END_SCREEN
    }

    private static WeakReference<Activity> mainActivityRef = new WeakReference<>(null);


    public static void setMainActivity(Activity activity) {
        mainActivityRef = new WeakReference<>(activity);
    }

    /**
     * @return If the app is currently in background PiP mode.
     */
    private static boolean isAppInBackgroundPiPMode() {
        Activity activity = mainActivityRef.get();
        return activity != null && activity.isInPictureInPictureMode();
    }

    /**
     * Injection point.
     */
    @Nullable
    public static Enum<?> changeShortsRepeatBehavior(@Nullable Enum<?> original) {
        try {
            ShortsLoopBehavior behavior = ExtendedUtils.IS_19_34_OR_GREATER &&
                    isAppInBackgroundPiPMode()
                    ? Settings.CHANGE_SHORTS_BACKGROUND_REPEAT_STATE.get()
                    : Settings.CHANGE_SHORTS_REPEAT_STATE.get();
            int originalBehaviorOrdinal = original == null
                    ? 0
                    : original.ordinal();
            int overrideBehaviorOrdinal = behavior.ordinal();

            if (overrideBehaviorOrdinal != 0) {
                Logger.printDebug(() -> {
                    String name = original == null ? "unknown (null)" : original.name();
                    return originalBehaviorOrdinal == overrideBehaviorOrdinal
                            ? "Behavior setting is same as original. Using original: " + name
                            : "Changing Shorts repeat behavior from: " + name + " to: " + behavior.name();
                });

                // For some reason, in YouTube 20.09+, 'UNKNOWN' functions as 'Pause'.
                int finalOverrideBehaviorOrdinal = ExtendedUtils.IS_20_09_OR_GREATER
                        && behavior == ShortsLoopBehavior.END_SCREEN
                        ? 0 : overrideBehaviorOrdinal;

                return getShortsLoopBehaviorEnum(finalOverrideBehaviorOrdinal);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "changeShortsRepeatBehavior failure", ex);
        }

        return original;
    }

    /**
     * Injection point.
     */
    public static boolean isAutoPlay(@Nullable Enum<?> original) {
        return original != null
                && ShortsLoopBehavior.SINGLE_PLAY.ordinal() == original.ordinal();
    }

    /**
     * Rest of the implementation added by patch.
     */
    private static Enum<?> getShortsLoopBehaviorEnum(int ordinal) {
        return null;
    }
}
