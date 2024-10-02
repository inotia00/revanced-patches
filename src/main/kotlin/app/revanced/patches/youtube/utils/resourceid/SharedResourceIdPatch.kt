package app.revanced.patches.youtube.utils.resourceid

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.mapping.ResourceMappingPatch
import app.revanced.patches.shared.mapping.ResourceMappingPatch.getId
import app.revanced.patches.shared.mapping.ResourceType.ATTR
import app.revanced.patches.shared.mapping.ResourceType.COLOR
import app.revanced.patches.shared.mapping.ResourceType.DIMEN
import app.revanced.patches.shared.mapping.ResourceType.DRAWABLE
import app.revanced.patches.shared.mapping.ResourceType.ID
import app.revanced.patches.shared.mapping.ResourceType.INTEGER
import app.revanced.patches.shared.mapping.ResourceType.LAYOUT
import app.revanced.patches.shared.mapping.ResourceType.STRING
import app.revanced.patches.shared.mapping.ResourceType.STYLE

@Patch(dependencies = [ResourceMappingPatch::class])
object SharedResourceIdPatch : ResourcePatch() {
    var AccessibilityOfflineButtonSync = -1L
    var AccountSwitcherAccessibility = -1L
    var ActionBarRingo = -1L
    var ActionBarRingoBackground = -1L
    var ActionBarSearchResultsViewMic = -1L
    var AdAttribution = -1L
    var Appearance = -1L
    var AppRelatedEndScreenResults = -1L
    var AutoNavPreviewStub = -1L
    var AutoNavToggle = -1L
    var BackgroundCategory = -1L
    var BadgeLabel = -1L
    var Bar = -1L
    var BarContainerHeight = -1L
    var BottomBarContainer = -1L
    var BottomSheetFooterText = -1L
    var BottomSheetRecyclerView = -1L
    var BottomUiContainerStub = -1L
    var CaptionToggleContainer = -1L
    var CastMediaRouteButton = -1L
    var CfFullscreenButton = -1L
    var ChannelListSubMenu = -1L
    var CompactLink = -1L
    var CompactListItem = -1L
    var ComponentLongClickListener = -1L
    var ContentPill = -1L
    var ControlsLayoutStub = -1L
    var DarkBackground = -1L
    var DarkSplashAnimation = -1L
    var DesignBottomSheet = -1L
    var DonationCompanion = -1L
    var DrawerContentView = -1L
    var DrawerResults = -1L
    var EasySeekEduContainer = -1L
    var EditSettingsAction = -1L
    var EndScreenElementLayoutCircle = -1L
    var EndScreenElementLayoutIcon = -1L
    var EndScreenElementLayoutVideo = -1L
    var EmojiPickerIcon = -1L
    var ExpandButtonDown = -1L
    var Fab = -1L
    var FadeDurationFast = -1L
    var FilterBarHeight = -1L
    var FloatyBarTopMargin = -1L
    var FullScreenButton = -1L
    var FullScreenEngagementOverlay = -1L
    var FullScreenEngagementPanel = -1L
    var HorizontalCardList = -1L
    var ImageOnlyTab = -1L
    var InlineTimeBarColorizedBarPlayedColorDark = -1L
    var InlineTimeBarPlayedNotHighlightedColor = -1L
    var InsetOverlayViewLayout = -1L
    var InterstitialsContainer = -1L
    var MenuItemView = -1L
    var MetaPanel = -1L
    var ModernMiniPlayerClose = -1L
    var ModernMiniPlayerExpand = -1L
    var ModernMiniPlayerForwardButton = -1L
    var ModernMiniPlayerRewindButton = -1L
    var MusicAppDeeplinkButtonView = -1L
    var Notice = -1L
    var NotificationBigPictureIconWidth = -1L
    var OfflineActionsVideoDeletedUndoSnackbarText = -1L
    var PlayerCollapseButton = -1L
    var PlayerVideoTitleView = -1L
    var PosterArtWidthDefault = -1L
    var QualityAuto = -1L
    var QuickActionsElementContainer = -1L
    var ReelDynRemix = -1L
    var ReelDynShare = -1L
    var ReelFeedbackLike = -1L
    var ReelFeedbackPause = -1L
    var ReelFeedbackPlay = -1L
    var ReelForcedMuteButton = -1L
    var ReelPlayerFooter = -1L
    var ReelPlayerRightPivotV2Size = -1L
    var ReelRightDislikeIcon = -1L
    var ReelRightLikeIcon = -1L
    var ReelTimeBarPlayedColor = -1L
    var ReelVodTimeStampsContainer = -1L
    var ReelWatchPlayer = -1L
    var RelatedChipCloudMargin = -1L
    var RightComment = -1L
    var ScrimOverlay = -1L
    var Scrubbing = -1L
    var SeekEasyHorizontalTouchOffsetToStartScrubbing = -1L
    var SeekUndoEduOverlayStub = -1L
    var SlidingDialogAnimation = -1L
    var SubtitleMenuSettingsFooterInfo = -1L
    var SuggestedAction = -1L
    var TapBloomView = -1L
    var TitleAnchor = -1L
    var ToolTipContentView = -1L
    var TotalTime = -1L
    var TouchArea = -1L
    var VideoQualityBottomSheet = -1L
    var VarispeedUnavailableTitle = -1L
    var VideoQualityUnavailableAnnouncement = -1L
    var VideoZoomSnapIndicator = -1L
    var VoiceSearch = -1L
    var YouTubeControlsOverlaySubtitleButton = -1L
    var YtOutlinePictureInPictureWhite = -1L
    var YtOutlineVideoCamera = -1L
    var YtOutlineXWhite = -1L
    var YtPremiumWordMarkHeader = -1L
    var YtWordMarkHeader = -1L

    override fun execute(context: ResourceContext) {

        AccessibilityOfflineButtonSync = getId(STRING, "accessibility_offline_button_sync")
        AccountSwitcherAccessibility = getId(STRING, "account_switcher_accessibility_label")
        ActionBarRingo = getId(LAYOUT, "action_bar_ringo")
        ActionBarRingoBackground = getId(LAYOUT, "action_bar_ringo_background")
        ActionBarSearchResultsViewMic = getId(LAYOUT, "action_bar_search_results_view_mic")
        AdAttribution = getId(ID, "ad_attribution")
        Appearance = getId(STRING, "app_theme_appearance_dark")
        AppRelatedEndScreenResults = getId(LAYOUT, "app_related_endscreen_results")
        AutoNavPreviewStub = getId(ID, "autonav_preview_stub")
        AutoNavToggle = getId(ID, "autonav_toggle")
        BackgroundCategory = getId(STRING, "pref_background_and_offline_category")
        BadgeLabel = getId(ID, "badge_label")
        Bar = getId(LAYOUT, "bar")
        BarContainerHeight = getId(DIMEN, "bar_container_height")
        BottomBarContainer = getId(ID, "bottom_bar_container")
        BottomSheetFooterText = getId(ID, "bottom_sheet_footer_text")
        BottomSheetRecyclerView = getId(LAYOUT, "bottom_sheet_recycler_view")
        BottomUiContainerStub = getId(ID, "bottom_ui_container_stub")
        CaptionToggleContainer = getId(ID, "caption_toggle_container")
        CastMediaRouteButton = getId(LAYOUT, "castmediaroutebutton")
        CfFullscreenButton = getId(ID, "cf_fullscreen_button")
        ChannelListSubMenu = getId(LAYOUT, "channel_list_sub_menu")
        CompactLink = getId(LAYOUT, "compact_link")
        CompactListItem = getId(LAYOUT, "compact_list_item")
        ComponentLongClickListener = getId(ID, "component_long_click_listener")
        ContentPill = getId(LAYOUT, "content_pill")
        ControlsLayoutStub = getId(ID, "controls_layout_stub")
        DarkBackground = getId(ID, "dark_background")
        DarkSplashAnimation = getId(ID, "dark_splash_animation")
        DesignBottomSheet = getId(ID, "design_bottom_sheet")
        DonationCompanion = getId(LAYOUT, "donation_companion")
        DrawerContentView = getId(ID, "drawer_content_view")
        DrawerResults = getId(ID, "drawer_results")
        EasySeekEduContainer = getId(ID, "easy_seek_edu_container")
        EditSettingsAction = getId(STRING, "edit_settings_action")
        EndScreenElementLayoutCircle = getId(LAYOUT, "endscreen_element_layout_circle")
        EndScreenElementLayoutIcon = getId(LAYOUT, "endscreen_element_layout_icon")
        EndScreenElementLayoutVideo = getId(LAYOUT, "endscreen_element_layout_video")
        EmojiPickerIcon = getId(ID, "emoji_picker_icon")
        ExpandButtonDown = getId(LAYOUT, "expand_button_down")
        Fab = getId(ID, "fab")
        FadeDurationFast = getId(INTEGER, "fade_duration_fast")
        FilterBarHeight = getId(DIMEN, "filter_bar_height")
        FloatyBarTopMargin = getId(DIMEN, "floaty_bar_button_top_margin")
        FullScreenButton = getId(ID, "fullscreen_button")
        FullScreenEngagementOverlay = getId(LAYOUT, "fullscreen_engagement_overlay")
        FullScreenEngagementPanel = getId(ID, "fullscreen_engagement_panel_holder")
        HorizontalCardList = getId(LAYOUT, "horizontal_card_list")
        ImageOnlyTab = getId(LAYOUT, "image_only_tab")
        InlineTimeBarColorizedBarPlayedColorDark =
            getId(COLOR, "inline_time_bar_colorized_bar_played_color_dark")
        InlineTimeBarPlayedNotHighlightedColor =
            getId(COLOR, "inline_time_bar_played_not_highlighted_color")
        InsetOverlayViewLayout = getId(ID, "inset_overlay_view_layout")
        InterstitialsContainer = getId(ID, "interstitials_container")
        MenuItemView = getId(ID, "menu_item_view")
        MetaPanel = getId(ID, "metapanel")
        ModernMiniPlayerClose = getId(ID, "modern_miniplayer_close")
        ModernMiniPlayerExpand = getId(ID, "modern_miniplayer_expand")
        ModernMiniPlayerForwardButton = getId(ID, "modern_miniplayer_forward_button")
        ModernMiniPlayerRewindButton = getId(ID, "modern_miniplayer_rewind_button")
        MusicAppDeeplinkButtonView = getId(ID, "music_app_deeplink_button_view")
        Notice = getId(ID, "notice")
        NotificationBigPictureIconWidth = getId(DIMEN, "notification_big_picture_icon_width")
        OfflineActionsVideoDeletedUndoSnackbarText =
            getId(STRING, "offline_actions_video_deleted_undo_snackbar_text")
        PlayerCollapseButton = getId(ID, "player_collapse_button")
        PlayerVideoTitleView = getId(ID, "player_video_title_view")
        PosterArtWidthDefault = getId(DIMEN, "poster_art_width_default")
        QualityAuto = getId(STRING, "quality_auto")
        QuickActionsElementContainer = getId(ID, "quick_actions_element_container")
        ReelDynRemix = getId(ID, "reel_dyn_remix")
        ReelDynShare = getId(ID, "reel_dyn_share")
        ReelFeedbackLike = getId(ID, "reel_feedback_like")
        ReelFeedbackPause = getId(ID, "reel_feedback_pause")
        ReelFeedbackPlay = getId(ID, "reel_feedback_play")
        ReelForcedMuteButton = getId(ID, "reel_player_forced_mute_button")
        ReelPlayerFooter = getId(LAYOUT, "reel_player_dyn_footer_vert_stories3")
        ReelPlayerRightPivotV2Size = getId(DIMEN, "reel_player_right_pivot_v2_size")
        ReelRightDislikeIcon = getId(DRAWABLE, "reel_right_dislike_icon")
        ReelRightLikeIcon = getId(DRAWABLE, "reel_right_like_icon")
        ReelTimeBarPlayedColor = getId(COLOR, "reel_time_bar_played_color")
        ReelVodTimeStampsContainer = getId(ID, "reel_vod_timestamps_container")
        ReelWatchPlayer = getId(ID, "reel_watch_player")
        RelatedChipCloudMargin = getId(LAYOUT, "related_chip_cloud_reduced_margins")
        RightComment = getId(DRAWABLE, "ic_right_comment_32c")
        ScrimOverlay = getId(ID, "scrim_overlay")
        Scrubbing = getId(DIMEN, "vertical_touch_offset_to_enter_fine_scrubbing")
        SeekEasyHorizontalTouchOffsetToStartScrubbing =
            getId(DIMEN, "seek_easy_horizontal_touch_offset_to_start_scrubbing")
        SeekUndoEduOverlayStub = getId(ID, "seek_undo_edu_overlay_stub")
        SlidingDialogAnimation = getId(STYLE, "SlidingDialogAnimation")
        SubtitleMenuSettingsFooterInfo = getId(STRING, "subtitle_menu_settings_footer_info")
        SuggestedAction = getId(LAYOUT, "suggested_action")
        TapBloomView = getId(ID, "tap_bloom_view")
        TitleAnchor = getId(ID, "title_anchor")
        ToolTipContentView = getId(LAYOUT, "tooltip_content_view")
        TotalTime = getId(STRING, "total_time")
        TouchArea = getId(ID, "touch_area")
        VideoQualityBottomSheet = getId(LAYOUT, "video_quality_bottom_sheet_list_fragment_title")
        VarispeedUnavailableTitle = getId(STRING, "varispeed_unavailable_title")
        VideoQualityUnavailableAnnouncement =
            getId(STRING, "video_quality_unavailable_announcement")
        VideoZoomSnapIndicator = getId(ID, "video_zoom_snap_indicator")
        VoiceSearch = getId(ID, "voice_search")
        YouTubeControlsOverlaySubtitleButton =
            getId(LAYOUT, "youtube_controls_overlay_subtitle_button")
        YtOutlinePictureInPictureWhite = getId(DRAWABLE, "yt_outline_picture_in_picture_white_24")
        YtOutlineVideoCamera = getId(DRAWABLE, "yt_outline_video_camera_black_24")
        YtOutlineXWhite = getId(DRAWABLE, "yt_outline_x_white_24")
        YtPremiumWordMarkHeader = getId(ATTR, "ytPremiumWordmarkHeader")
        YtWordMarkHeader = getId(ATTR, "ytWordmarkHeader")

    }
}
