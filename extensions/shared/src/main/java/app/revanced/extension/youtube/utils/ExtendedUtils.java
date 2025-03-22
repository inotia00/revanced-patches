package app.revanced.extension.youtube.utils;

import static app.revanced.extension.shared.utils.StringRef.str;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.PackageUtils;
import app.revanced.extension.youtube.settings.Settings;

public class ExtendedUtils extends PackageUtils {
    @SuppressWarnings("unused")
    public static final boolean IS_19_17_OR_GREATER = getAppVersionName().compareTo("19.17.00") >= 0;
    public static final boolean IS_19_20_OR_GREATER = getAppVersionName().compareTo("19.20.00") >= 0;
    public static final boolean IS_19_21_OR_GREATER = getAppVersionName().compareTo("19.21.00") >= 0;
    public static final boolean IS_19_26_OR_GREATER = getAppVersionName().compareTo("19.26.00") >= 0;
    public static final boolean IS_19_28_OR_GREATER = getAppVersionName().compareTo("19.28.00") >= 0;
    public static final boolean IS_19_29_OR_GREATER = getAppVersionName().compareTo("19.29.00") >= 0;
    public static final boolean IS_19_34_OR_GREATER = getAppVersionName().compareTo("19.34.00") >= 0;

    public static int validateValue(IntegerSetting settings, int min, int max, String message) {
        int value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_extended_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    public static float validateValue(FloatSetting settings, float min, float max, String message) {
        float value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_extended_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    public static boolean isFullscreenHidden() {
        return Settings.DISABLE_ENGAGEMENT_PANEL.get() || Settings.HIDE_QUICK_ACTIONS.get();
    }

    public static boolean isSpoofingToLessThan(@NonNull String versionName) {
        if (!Settings.SPOOF_APP_VERSION.get())
            return false;

        return isVersionToLessThan(Settings.SPOOF_APP_VERSION_TARGET.get(), versionName);
    }

    public static void setCommentPreviewSettings() {
        final boolean enabled = Settings.HIDE_PREVIEW_COMMENT.get();
        final boolean newMethod = Settings.HIDE_PREVIEW_COMMENT_TYPE.get();

        Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD.save(enabled && !newMethod);
        Settings.HIDE_PREVIEW_COMMENT_NEW_METHOD.save(enabled && newMethod);
    }

    private static final Setting<?>[] additionalSettings = {
            Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
            Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
            Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
            Settings.SPOOF_APP_VERSION,
            Settings.SPOOF_APP_VERSION_TARGET
    };

    public static boolean anyMatchSetting(Setting<?> setting) {
        for (Setting<?> s : additionalSettings) {
            if (setting == s) return true;
        }
        return false;
    }

    public static void setPlayerFlyoutMenuAdditionalSettings() {
        Settings.HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS.save(isAdditionalSettingsEnabled());
    }

    private static boolean isAdditionalSettingsEnabled() {
        // In the old player flyout panels, the video quality icon and additional quality icon are the same
        // Therefore, additional Settings should not be blocked in old player flyout panels
        if (isSpoofingToLessThan("18.22.00"))
            return false;

        boolean additionalSettingsEnabled = true;
        final BooleanSetting[] additionalSettings = {
                Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
                Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
                Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
        };
        for (BooleanSetting s : additionalSettings) {
            additionalSettingsEnabled &= s.get();
        }
        return additionalSettingsEnabled;
    }
}