package app.revanced.extension.music.patches.misc;

import static app.revanced.extension.music.shared.VideoInformation.parameterIsAgeRestricted;
import static app.revanced.extension.music.shared.VideoInformation.parameterIsSample;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.shared.VideoInformation;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class SpoofPlayerParameterPatch {
    /**
     * Used in YouTube Music.
     */
    private static final boolean SPOOF_PLAYER_PARAMETER = Settings.SPOOF_PLAYER_PARAMETER.get();

    /**
     * Parameter to fix playback issues.
     * Used in YouTube Music Samples.
     */
    private static final String PLAYER_PARAMETER_SAMPLES =
            "8AEB2AUBogYVAUY4C8W9wrM-FdhjSW4MnCgH44uhkAcI";

    /**
     * Parameter to fix playback issues.
     * Used in YouTube Shorts.
     */
    private static final String PLAYER_PARAMETER_SHORTS =
            "8AEByAMkuAQ0ogYVAePzwRN3uesV1sPI2x4-GkDYlvqUkAcC";

    /**
     * On app first start, the first video played usually contains a single non-default window setting value
     * and all other subtitle settings for the video are (incorrect) default Samples window settings.
     * For this situation, the Samples settings must be replaced.
     * <p>
     * But some videos use multiple text positions on screen (such as youtu.be/3hW1rMNC89o),
     * and by chance many of the subtitles uses window positions that match a default Samples position.
     * To handle these videos, selectively allowing the Samples specific window settings to 'pass thru' unchanged,
     * but only if the video contains multiple non-default subtitle window positions.
     * <p>
     * Do not enable 'pass thru mode' until this many non default subtitle settings are observed for a single video.
     */
    private static final int NUMBER_OF_NON_DEFAULT_SUBTITLES_BEFORE_ENABLING_PASSTHRU = 2;

    /**
     * The number of non default subtitle settings encountered for the current video.
     */
    private static int numberOfNonDefaultSettingsObserved;

    @GuardedBy("itself")
    private static final Map<String, Boolean> lastVideoIds = new LinkedHashMap<>() {
        private static final int NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK = 5;

        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            return size() > NUMBER_OF_LAST_VIDEO_IDS_TO_TRACK;
        }
    };

    /**
     * Injection point.
     */
    public static String spoofParameter(@NonNull String videoId, @Nullable String parameter) {
        if (SPOOF_PLAYER_PARAMETER) {
            synchronized (lastVideoIds) {
                Boolean isSamples = parameterIsSample(parameter);
                if (lastVideoIds.put(videoId, isSamples) == null) {
                    Logger.printDebug(() -> "New video loaded (videoId: " + videoId + ", isSamples: " + isSamples + ")");
                }
            }
            return parameterIsAgeRestricted(parameter)
                    ? PLAYER_PARAMETER_SHORTS
                    : PLAYER_PARAMETER_SAMPLES;
        }
        return parameter;
    }

    /**
     * Injection point.  Overrides values passed into SubtitleWindowSettings constructor.
     *
     * @param ap anchor position. A bitmask with 6 bit fields, that appears to indicate the layout position on screen
     * @param ah anchor horizontal. A percentage [0, 100], that appears to be a horizontal text anchor point
     * @param av anchor vertical. A percentage [0, 100], that appears to be a vertical text anchor point
     * @param vs appears to indicate if subtitles exist, and the value is always true.
     * @param sd function is not entirely clear
     */
    public static int[] fixSubtitleWindowPosition(int ap, int ah, int av, boolean vs, boolean sd) {
        // Videos with custom captions that specify screen positions appear to always have correct screen positions (even with spoofing).
        // But for auto generated and most other captions, the spoof incorrectly gives various default Samples caption settings.
        // Check for these known default Samples captions parameters, and replace with the known correct values.
        //
        // If a regular video uses a custom subtitle setting that match a default Samples setting,
        // then this will incorrectly replace the setting.
        // But, if the video uses multiple subtitles in different screen locations, then detect the non-default values
        // and do not replace any window settings for the video (regardless if they match a Samples default).
        if (SPOOF_PLAYER_PARAMETER &&
                numberOfNonDefaultSettingsObserved < NUMBER_OF_NON_DEFAULT_SUBTITLES_BEFORE_ENABLING_PASSTHRU) {
            synchronized (lastVideoIds) {
                String videoId = VideoInformation.getVideoId();
                Boolean isSample = lastVideoIds.get(videoId);
                if (BooleanUtils.isFalse(isSample)) {
                    for (SubtitleWindowReplacementSettings setting : SubtitleWindowReplacementSettings.values()) {
                        if (setting.match(ap, ah, av, vs, sd)) {
                            return setting.replacementSetting();
                        }
                    }

                    numberOfNonDefaultSettingsObserved++;
                }
            }
        }

        return new int[]{ap, ah, av};
    }

    /**
     * Injection point.
     * <p>
     * Return false to force disable age restricted playback feature flag.
     */
    public static boolean forceDisableAgeRestrictedPlaybackFeatureFlag(boolean original) {
        if (SPOOF_PLAYER_PARAMETER) {
            return false;
        }
        return original;
    }

    /**
     * Known incorrect default Samples subtitle parameters, and the corresponding correct (non-Samples) values.
     */
    private enum SubtitleWindowReplacementSettings {
        DEFAULT_SAMPLES_PARAMETERS_1(10, 50, 0, true, false,
                34, 50, 95),
        DEFAULT_SAMPLES_PARAMETERS_2(9, 20, 0, true, false,
                34, 50, 90),
        DEFAULT_SAMPLES_PARAMETERS_3(9, 20, 0, true, true,
                33, 20, 100);

        // original values
        final int ap, ah, av;
        final boolean vs, sd;

        // replacement int values
        final int[] replacement;

        SubtitleWindowReplacementSettings(int ap, int ah, int av, boolean vs, boolean sd,
                                          int replacementAp, int replacementAh, int replacementAv) {
            this.ap = ap;
            this.ah = ah;
            this.av = av;
            this.vs = vs;
            this.sd = sd;
            this.replacement = new int[]{replacementAp, replacementAh, replacementAv};
        }

        boolean match(int ap, int ah, int av, boolean vs, boolean sd) {
            return this.ap == ap && this.ah == ah && this.av == av && this.vs == vs && this.sd == sd;
        }

        int[] replacementSetting() {
            return replacement;
        }
    }
}
