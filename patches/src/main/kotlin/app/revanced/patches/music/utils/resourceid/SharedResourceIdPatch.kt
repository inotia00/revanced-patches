package app.revanced.patches.music.utils.resourceid

import app.revanced.patcher.patch.resourcePatch
import app.revanced.patches.shared.mapping.ResourceType.ATTR
import app.revanced.patches.shared.mapping.ResourceType.BOOL
import app.revanced.patches.shared.mapping.ResourceType.COLOR
import app.revanced.patches.shared.mapping.ResourceType.DIMEN
import app.revanced.patches.shared.mapping.ResourceType.DRAWABLE
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.LAYOUT
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.getResourceId
import app.revanced.patches.shared.mapping.resourceMappingPatch

var accountSwitcherAccessibility = -1L
    private set
var actionBarLogo = -1L
    private set
var actionBarLogoRingo2 = -1L
    private set
var bottomSheetRecyclerView = -1L
    private set
var buttonContainer = -1L
    private set
var buttonIconPaddingMedium = -1L
    private set
var channelHandle = -1L
    private set
var chipCloud = -1L
    private set
var colorGrey = -1L
    private set
var darkBackground = -1L
    private set
var designBottomSheetDialog = -1L
    private set
var elementsContainer = -1L
    private set
var elementsLottieAnimationViewTagId = -1L
    private set
var endButtonsContainer = -1L
    private set
var floatingLayout = -1L
    private set
var historyMenuItem = -1L
    private set
var inlineTimeBarAdBreakMarkerColor = -1L
    private set
var inlineTimeBarProgressColor = -1L
    private set
var isTablet = -1L
    private set
var likeDislikeContainer = -1L
    private set
var mainActivityLaunchAnimation = -1L
    private set
var menuEntry = -1L
    private set
var miniPlayerDefaultText = -1L
    private set
var miniPlayerMdxPlaying = -1L
    private set
var miniPlayerPlayPauseReplayButton = -1L
    private set
var miniPlayerViewPager = -1L
    private set
var modernDialogBackground = -1L
    private set
var musicNotifierShelf = -1L
    private set
var musicSnackbarActionColor = -1L
    private set
var musicTasteBuilderShelf = -1L
    private set
var namesInactiveAccountThumbnailSize = -1L
    private set
var offlineSettingsMenuItem = -1L
    private set
var playerOverlayChip = -1L
    private set
var playerViewPager = -1L
    private set
var privacyTosFooter = -1L
    private set
var qualityAuto = -1L
    private set
var remixGenericButtonSize = -1L
    private set
var searchButton = -1L
    private set
var tapBloomView = -1L
    private set
var text1 = -1L
    private set
var toolTipContentView = -1L
    private set
var topEnd = -1L
    private set
var topStart = -1L
    private set
var topBarMenuItemImageView = -1L
    private set
var tosFooter = -1L
    private set
var touchOutside = -1L
    private set
var trimSilenceSwitch = -1L
    private set
var varispeedUnavailableTitle = -1L
    private set
var ytFillSamples = -1L
    private set
var ytFillYouTubeMusic = -1L
    private set
var ytOutlineSamples = -1L
    private set
var ytOutlineYouTubeMusic = -1L
    private set
var ytmLogo = -1L
    private set
var ytmLogoRingo2 = -1L
    private set

internal val sharedResourceIdPatch = resourcePatch(
    description = "sharedResourceIdPatch"
) {
    dependsOn(resourceMappingPatch)

    execute {
        accountSwitcherAccessibility = getResourceId(STRING, "account_switcher_accessibility_label")
        actionBarLogo = getResourceId(DRAWABLE, "action_bar_logo")
        actionBarLogoRingo2 = getResourceId(DRAWABLE, "action_bar_logo_ringo2")
        bottomSheetRecyclerView = getResourceId(LAYOUT, "bottom_sheet_recycler_view")
        buttonContainer = getResourceId(ID, "button_container")
        buttonIconPaddingMedium = getResourceId(DIMEN, "button_icon_padding_medium")
        channelHandle = getResourceId(ID, "channel_handle")
        chipCloud = getResourceId(LAYOUT, "chip_cloud")
        colorGrey = getResourceId(COLOR, "ytm_color_grey_12")
        darkBackground = getResourceId(ID, "dark_background")
        designBottomSheetDialog = getResourceId(LAYOUT, "design_bottom_sheet_dialog")
        elementsContainer = getResourceId(ID, "elements_container")
        elementsLottieAnimationViewTagId =
            getResourceId(ID, "elements_lottie_animation_view_tag_id")
        endButtonsContainer = getResourceId(ID, "end_buttons_container")
        floatingLayout = getResourceId(ID, "floating_layout")
        historyMenuItem = getResourceId(ID, "history_menu_item")
        inlineTimeBarAdBreakMarkerColor =
            getResourceId(COLOR, "inline_time_bar_ad_break_marker_color")
        inlineTimeBarProgressColor = getResourceId(COLOR, "inline_time_bar_progress_color")
        isTablet = getResourceId(BOOL, "is_tablet")
        likeDislikeContainer = getResourceId(ID, "like_dislike_container")
        mainActivityLaunchAnimation = getResourceId(LAYOUT, "main_activity_launch_animation")
        menuEntry = getResourceId(LAYOUT, "menu_entry")
        miniPlayerDefaultText = getResourceId(STRING, "mini_player_default_text")
        miniPlayerMdxPlaying = getResourceId(STRING, "mini_player_mdx_playing")
        miniPlayerPlayPauseReplayButton = getResourceId(ID, "mini_player_play_pause_replay_button")
        miniPlayerViewPager = getResourceId(ID, "mini_player_view_pager")
        modernDialogBackground = getResourceId(DRAWABLE, "modern_dialog_background")
        musicNotifierShelf = getResourceId(LAYOUT, "music_notifier_shelf")
        musicSnackbarActionColor = getResourceId(ATTR, "musicSnackbarActionColor")
        musicTasteBuilderShelf = getResourceId(LAYOUT, "music_tastebuilder_shelf")
        namesInactiveAccountThumbnailSize =
            getResourceId(DIMEN, "names_inactive_account_thumbnail_size")
        offlineSettingsMenuItem = getResourceId(ID, "offline_settings_menu_item")
        playerOverlayChip = getResourceId(ID, "player_overlay_chip")
        playerViewPager = getResourceId(ID, "player_view_pager")
        privacyTosFooter = getResourceId(ID, "privacy_tos_footer")
        qualityAuto = getResourceId(STRING, "quality_auto")
        remixGenericButtonSize = getResourceId(DIMEN, "remix_generic_button_size")
        searchButton = getResourceId(LAYOUT, "search_button")
        tapBloomView = getResourceId(ID, "tap_bloom_view")
        text1 = getResourceId(ID, "text1")
        toolTipContentView = getResourceId(LAYOUT, "tooltip_content_view")
        topEnd = getResourceId(ID, "TOP_END")
        topStart = getResourceId(ID, "TOP_START")
        topBarMenuItemImageView = getResourceId(ID, "top_bar_menu_item_image_view")
        tosFooter = getResourceId(ID, "tos_footer")
        touchOutside = getResourceId(ID, "touch_outside")
        trimSilenceSwitch = getResourceId(ID, "trim_silence_switch")
        varispeedUnavailableTitle = getResourceId(STRING, "varispeed_unavailable_title")
        ytFillSamples = getResourceId(DRAWABLE, "yt_fill_samples_vd_theme_24")
        ytFillYouTubeMusic = getResourceId(DRAWABLE, "yt_fill_youtube_music_vd_theme_24")
        ytOutlineSamples = getResourceId(DRAWABLE, "yt_outline_samples_vd_theme_24")
        ytOutlineYouTubeMusic = getResourceId(DRAWABLE, "yt_outline_youtube_music_vd_theme_24")
        ytmLogo = getResourceId(DRAWABLE, "ytm_logo")
        ytmLogoRingo2 = getResourceId(DRAWABLE, "ytm_logo_ringo2")
    }
}