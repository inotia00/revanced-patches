package app.revanced.extension.music.patches.utils;

import static app.revanced.extension.music.utils.ExtendedUtils.IS_7_25_OR_GREATER;

@SuppressWarnings("unused")
public class PatchStatus {
    public static boolean DarkTheme() {
        // Replace this with true if the 'Dark theme' patch succeeds
        return false;
    }

    public static String SpoofAppVersionDefaultString() {
        return IS_7_25_OR_GREATER ? "6.42.55" : "6.35.52";
    }
}
