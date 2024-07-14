package app.revanced.patches.music.utils.integrations

@Suppress("MemberVisibilityCanBePrivate")
object Constants {
    const val INTEGRATIONS_PATH = "Lapp/revanced/integrations/music"
    const val SHARED_PATH = "$INTEGRATIONS_PATH/shared"
    const val PATCHES_PATH = "$INTEGRATIONS_PATH/patches"

    const val ACCOUNT_PATH = "$PATCHES_PATH/account"
    const val ACTIONBAR_PATH = "$PATCHES_PATH/actionbar"
    const val ADS_PATH = "$PATCHES_PATH/ads"
    const val COMPONENTS_PATH = "$PATCHES_PATH/components"
    const val FLYOUT_PATH = "$PATCHES_PATH/flyout"
    const val GENERAL_PATH = "$PATCHES_PATH/general"
    const val MISC_PATH = "$PATCHES_PATH/misc"
    const val NAVIGATION_PATH = "$PATCHES_PATH/navigation"
    const val PLAYER_PATH = "$PATCHES_PATH/player"
    const val VIDEO_PATH = "$PATCHES_PATH/video"
    const val UTILS_PATH = "$PATCHES_PATH/utils"

    const val ACCOUNT_CLASS_DESCRIPTOR = "$ACCOUNT_PATH/AccountPatch;"
    const val ACTIONBAR_CLASS_DESCRIPTOR = "$ACTIONBAR_PATH/ActionBarPatch;"
    const val FLYOUT_CLASS_DESCRIPTOR = "$FLYOUT_PATH/FlyoutPatch;"
    const val GENERAL_CLASS_DESCRIPTOR = "$GENERAL_PATH/GeneralPatch;"
    const val NAVIGATION_CLASS_DESCRIPTOR = "$NAVIGATION_PATH/NavigationPatch;"
    const val PLAYER_CLASS_DESCRIPTOR = "$PLAYER_PATH/PlayerPatch;"
}