package app.revanced.patches.youtube.layout.visual

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.layout.branding.icon.CustomBrandingIconPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow
import org.w3c.dom.Element

@Suppress("DEPRECATION", "unused")
object VisualPreferencesIconsPatch : BaseResourcePatch(
    name = "Visual preferences icons",
    description = "Adds icons to specific preferences in the settings.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    private const val DEFAULT_ICON = "extension"

    private val RVXSettingsMenuIcon = stringPatchOption(
        key = "RVXSettingsMenuIcon",
        default = DEFAULT_ICON,
        values = mapOf(
            "Custom branding icon" to "custom_branding_icon",
            "Extension" to DEFAULT_ICON,
            "Gear" to "gear",
            "ReVanced" to "revanced",
            "ReVanced Colored" to "revanced_colored",
        ),
        title = "RVX settings menu icon",
        description = "Apply different icons for RVX settings menu.",
        required = true
    )

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val selectedIconType = RVXSettingsMenuIcon
            .underBarOrThrow()

        val customBrandingIconType = CustomBrandingIconPatch.AppIcon
            .underBarOrThrow()

        // region copy shared resources.

        arrayOf(
            ResourceGroup(
                "drawable",
                *preferenceIcon.values.map { "$it.xml" }.toTypedArray()
            ),
            ResourceGroup(
                "drawable-xxhdpi",
                "$emptyIcon.png"
            ),
        ).forEach { resourceGroup ->
            context.copyResources("youtube/visual/shared", resourceGroup)
        }

        // endregion.

        // region copy RVX settings menu icon.

        val fallbackIconPath = "youtube/visual/icons/extension"
        val iconPath = when (selectedIconType) {
            "custom_branding_icon" -> "youtube/branding/$customBrandingIconType/settings"
            else -> "youtube/visual/icons/$selectedIconType"
        }
        val resourceGroup = ResourceGroup(
            "drawable",
            "revanced_extended_settings_key_icon.xml"
        )

        try {
            context.copyResources(iconPath, resourceGroup)
        } catch (_: Exception) {
            // Ignore if resource copy fails

            // Add a fallback extended icon
            // It's needed if someone provides custom path to icon(s) folder
            // but custom branding icons for Extended setting are predefined,
            // so it won't copy custom branding icon
            // and will raise an error without fallback icon
            context.copyResources(fallbackIconPath, resourceGroup)
        }

        // endregion.

        // region set visual preferences icon.

        arrayOf(
            "res/xml/revanced_prefs.xml",
            "res/xml/settings_fragment.xml"
        ).forEach { xmlFile ->
            context.xmlEditor[xmlFile].use { editor ->
                editor.file.doRecursively loop@{ node ->
                    if (node !is Element) return@loop

                    node.getAttributeNode("android:key")
                        ?.textContent
                        ?.removePrefix("@string/")
                        ?.let { title ->
                            val drawableName = when (title) {
                                in preferenceKey -> preferenceIcon[title]

                                // Add custom RVX settings menu icon
                                in intentKey -> intentIcon[title]
                                in emptyTitles -> emptyIcon
                                else -> null
                            }
                            drawableName?.let {
                                node.setAttribute("android:icon", "@drawable/$it")
                            }
                        }
                }
            }
        }

        // endregion.

        SettingsPatch.updatePatchStatus(this)
    }

    // region preference key and icon.

    private val preferenceKey = setOf(
        // Main settings (sorted as displayed in the settings)
        "parent_tools_key",
        "general_key",
        "account_switcher_key",
        "data_saving_settings_key",
        "auto_play_key",
        "video_quality_settings_key",
        "offline_key",
        "pair_with_tv_key",
        "history_key",
        "your_data_key",
        "privacy_key",
        "premium_early_access_browse_page_key",
        "subscription_product_setting_key",
        "billing_and_payment_key",
        "notification_key",
        "connected_accounts_browse_page_key",
        "live_chat_key",
        "captions_key",
        "accessibility_settings_key",
        "about_key",

        // Main RVX settings (sorted as displayed in the settings)
        "revanced_preference_screen_ads",
        "revanced_preference_screen_alt_thumbnails",
        "revanced_preference_screen_feed",
        "revanced_preference_screen_general",
        "revanced_preference_screen_player",
        "revanced_preference_screen_shorts",
        "revanced_preference_screen_swipe_controls",
        "revanced_preference_screen_video",
        "revanced_preference_screen_ryd",
        "revanced_preference_screen_sb",
        "revanced_preference_screen_misc",

        // Internal RVX settings (items without prefix are listed first, others are sorted alphabetically)
        "gms_core_settings",
        "sb_enable_create_segment",
        "sb_enable_voting",

        "revanced_alt_thumbnail_home",
        "revanced_alt_thumbnail_library",
        "revanced_alt_thumbnail_player",
        "revanced_alt_thumbnail_search",
        "revanced_alt_thumbnail_subscriptions",
        "revanced_change_shorts_repeat_state",
        "revanced_custom_player_overlay_opacity",
        "revanced_default_app_settings",
        "revanced_default_playback_speed",
        "revanced_default_video_quality_wifi",
        "revanced_disable_hdr_auto_brightness",
        "revanced_disable_hdr_video",
        "revanced_disable_quic_protocol",
        "revanced_enable_debug_logging",
        "revanced_enable_default_playback_speed_shorts",
        "revanced_enable_external_browser",
        "revanced_enable_old_quality_layout",
        "revanced_enable_open_links_directly",
        "revanced_enable_swipe_brightness",
        "revanced_enable_swipe_haptic_feedback",
        "revanced_enable_swipe_lowest_value_auto_brightness",
        "revanced_enable_swipe_press_to_engage",
        "revanced_enable_swipe_volume",
        "revanced_enable_watch_panel_gestures",
        "revanced_hide_clip_button",
        "revanced_hide_download_button",
        "revanced_hide_keyword_content_comments",
        "revanced_hide_keyword_content_home",
        "revanced_hide_keyword_content_search",
        "revanced_hide_keyword_content_subscriptions",
        "revanced_hide_like_dislike_button",
        "revanced_hide_navigation_create_button",
        "revanced_hide_navigation_home_button",
        "revanced_hide_navigation_library_button",
        "revanced_hide_navigation_notifications_button",
        "revanced_hide_navigation_shorts_button",
        "revanced_hide_navigation_subscriptions_button",
        "revanced_hide_player_autoplay_button",
        "revanced_hide_player_captions_button",
        "revanced_hide_player_cast_button",
        "revanced_hide_player_collapse_button",
        "revanced_hide_player_flyout_menu_ambient_mode",
        "revanced_hide_player_flyout_menu_audio_track",
        "revanced_hide_player_flyout_menu_captions",
        "revanced_hide_player_flyout_menu_help",
        "revanced_hide_player_flyout_menu_lock_screen",
        "revanced_hide_player_flyout_menu_loop_video",
        "revanced_hide_player_flyout_menu_more_info",
        "revanced_hide_player_flyout_menu_playback_speed",
        "revanced_hide_player_flyout_menu_quality_footer",
        "revanced_hide_player_flyout_menu_report",
        "revanced_hide_player_flyout_menu_stable_volume",
        "revanced_hide_player_flyout_menu_stats_for_nerds",
        "revanced_hide_player_flyout_menu_watch_in_vr",
        "revanced_hide_player_fullscreen_button",
        "revanced_hide_player_previous_next_button",
        "revanced_hide_player_youtube_music_button",
        "revanced_hide_playlist_button",
        "revanced_hide_quick_actions_comment_button",
        "revanced_hide_quick_actions_dislike_button",
        "revanced_hide_quick_actions_like_button",
        "revanced_hide_quick_actions_live_chat_button",
        "revanced_hide_quick_actions_more_button",
        "revanced_hide_quick_actions_save_to_playlist_button",
        "revanced_hide_quick_actions_share_button",
        "revanced_hide_remix_button",
        "revanced_hide_report_button",
        "revanced_hide_share_button",
        "revanced_hide_shorts_comments_button",
        "revanced_hide_shorts_dislike_button",
        "revanced_hide_shorts_like_button",
        "revanced_hide_shorts_navigation_bar",
        "revanced_hide_shorts_remix_button",
        "revanced_hide_shorts_share_button",
        "revanced_hide_shorts_shelf_history",
        "revanced_hide_shorts_shelf_home_related_videos",
        "revanced_hide_shorts_shelf_search",
        "revanced_hide_shorts_shelf_subscriptions",
        "revanced_hide_shorts_toolbar",
        "revanced_overlay_button_always_repeat",
        "revanced_overlay_button_copy_video_url",
        "revanced_overlay_button_copy_video_url_timestamp",
        "revanced_overlay_button_mute_volume",
        "revanced_overlay_button_external_downloader",
        "revanced_overlay_button_speed_dialog",
        "revanced_overlay_button_time_ordered_playlist",
        "revanced_overlay_button_whitelist",
        "revanced_preference_screen_account_menu",
        "revanced_preference_screen_action_buttons",
        "revanced_preference_screen_ambient_mode",
        "revanced_preference_screen_category_bar",
        "revanced_preference_screen_channel_bar",
        "revanced_preference_screen_channel_profile",
        "revanced_preference_screen_comments",
        "revanced_preference_screen_community_posts",
        "revanced_preference_screen_custom_filter",
        "revanced_preference_screen_feed_flyout_menu",
        "revanced_preference_screen_fullscreen",
        "revanced_preference_screen_haptic_feedback",
        "revanced_preference_screen_import_export",
        "revanced_preference_screen_miniplayer",
        "revanced_preference_screen_navigation_buttons",
        "revanced_preference_screen_patch_information",
        "revanced_preference_screen_player_buttons",
        "revanced_preference_screen_player_flyout_menu",
        "revanced_preference_screen_seekbar",
        "revanced_preference_screen_settings_menu",
        "revanced_preference_screen_shorts_player",
        "revanced_preference_screen_spoof_client",
        "revanced_preference_screen_toolbar",
        "revanced_preference_screen_video_description",
        "revanced_preference_screen_video_filter",
        "revanced_sanitize_sharing_links",
        "revanced_swipe_gestures_lock_mode",
        "revanced_swipe_magnitude_threshold",
        "revanced_swipe_overlay_background_alpha",
        "revanced_swipe_overlay_rect_size",
        "revanced_swipe_overlay_text_size",
        "revanced_swipe_overlay_timeout",
        "revanced_switch_create_with_notifications_button",
        "revanced_change_player_flyout_menu_toggle",
    )

    private val intentKey = setOf(
        "revanced_extended_settings_key",
    )

    private val emptyTitles = setOf(
        "revanced_custom_playback_speeds",
        "revanced_custom_playback_speed_menu_type",
        "revanced_default_video_quality_mobile",
        "revanced_disable_default_playback_speed_live",
        "revanced_enable_custom_playback_speed",
        "revanced_external_downloader_package_name",
        "revanced_hide_shorts_comments_disabled_button",
        "revanced_hide_player_flyout_menu_captions_footer",
        "revanced_remember_playback_speed_last_selected",
        "revanced_remember_video_quality_last_selected",
        "revanced_restore_old_video_quality_menu",
        "revanced_enable_debug_buffer_logging",
        "revanced_whitelist_settings",
    )

    // A lot of mappings here.
    // The performance impact should be negligible in this context,
    // as the operations involved are not computationally intensive.
    private val preferenceIcon = preferenceKey.associateWith { title ->
        when (title) {
            // Main RVX settings
            "revanced_preference_screen_general" -> "general_key_icon"
            "revanced_preference_screen_sb" -> "sb_enable_create_segment_icon"

            // Internal RVX settings
            "revanced_alt_thumbnail_home" -> "revanced_hide_navigation_home_button_icon"
            "revanced_alt_thumbnail_library" -> "revanced_preference_screen_video_icon"
            "revanced_alt_thumbnail_player" -> "revanced_preference_screen_player_icon"
            "revanced_alt_thumbnail_search" -> "revanced_hide_shorts_shelf_search_icon"
            "revanced_alt_thumbnail_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_custom_player_overlay_opacity" -> "revanced_swipe_overlay_background_alpha_icon"
            "revanced_default_app_settings" -> "revanced_preference_screen_settings_menu_icon"
            "revanced_default_playback_speed" -> "revanced_overlay_button_speed_dialog_icon"
            "revanced_enable_old_quality_layout" -> "revanced_default_video_quality_wifi_icon"
            "revanced_enable_watch_panel_gestures" -> "revanced_preference_screen_swipe_controls_icon"
            "revanced_hide_download_button" -> "revanced_overlay_button_external_downloader_icon"
            "revanced_hide_keyword_content_comments" -> "revanced_hide_quick_actions_comment_button_icon"
            "revanced_hide_keyword_content_home" -> "revanced_hide_navigation_home_button_icon"
            "revanced_hide_keyword_content_search" -> "revanced_hide_shorts_shelf_search_icon"
            "revanced_hide_keyword_content_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_hide_like_dislike_button" -> "sb_enable_voting_icon"
            "revanced_hide_navigation_library_button" -> "revanced_preference_screen_video_icon"
            "revanced_hide_navigation_notifications_button" -> "notification_key_icon"
            "revanced_hide_navigation_shorts_button" -> "revanced_preference_screen_shorts_icon"
            "revanced_hide_player_autoplay_button" -> "revanced_change_player_flyout_menu_toggle_icon"
            "revanced_hide_player_captions_button" -> "captions_key_icon"
            "revanced_hide_player_flyout_menu_ambient_mode" -> "revanced_preference_screen_ambient_mode_icon"
            "revanced_hide_player_flyout_menu_captions" -> "captions_key_icon"
            "revanced_hide_player_flyout_menu_loop_video" -> "revanced_overlay_button_always_repeat_icon"
            "revanced_hide_player_flyout_menu_more_info" -> "about_key_icon"
            "revanced_hide_player_flyout_menu_quality_footer" -> "revanced_default_video_quality_wifi_icon"
            "revanced_hide_player_flyout_menu_report" -> "revanced_hide_report_button_icon"
            "revanced_hide_player_fullscreen_button" -> "revanced_preference_screen_fullscreen_icon"
            "revanced_hide_quick_actions_dislike_button" -> "revanced_preference_screen_ryd_icon"
            "revanced_hide_quick_actions_live_chat_button" -> "live_chat_key_icon"
            "revanced_hide_quick_actions_save_to_playlist_button" -> "revanced_hide_playlist_button_icon"
            "revanced_hide_quick_actions_share_button" -> "revanced_hide_shorts_share_button_icon"
            "revanced_hide_remix_button" -> "revanced_hide_shorts_remix_button_icon"
            "revanced_hide_share_button" -> "revanced_hide_shorts_share_button_icon"
            "revanced_hide_shorts_comments_button" -> "revanced_hide_quick_actions_comment_button_icon"
            "revanced_hide_shorts_dislike_button" -> "revanced_preference_screen_ryd_icon"
            "revanced_hide_shorts_like_button" -> "revanced_hide_quick_actions_like_button_icon"
            "revanced_hide_shorts_navigation_bar" -> "revanced_preference_screen_navigation_buttons_icon"
            "revanced_hide_shorts_shelf_home_related_videos" -> "revanced_hide_navigation_home_button_icon"
            "revanced_hide_shorts_shelf_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_hide_shorts_toolbar" -> "revanced_preference_screen_toolbar_icon"
            "revanced_preference_screen_account_menu" -> "account_switcher_key_icon"
            "revanced_preference_screen_channel_bar" -> "account_switcher_key_icon"
            "revanced_preference_screen_channel_profile" -> "account_switcher_key_icon"
            "revanced_preference_screen_comments" -> "revanced_hide_quick_actions_comment_button_icon"
            "revanced_preference_screen_feed_flyout_menu" -> "revanced_preference_screen_player_flyout_menu_icon"
            "revanced_preference_screen_haptic_feedback" -> "revanced_enable_swipe_haptic_feedback_icon"
            "revanced_preference_screen_miniplayer" -> "offline_key_icon"
            "revanced_preference_screen_patch_information" -> "about_key_icon"
            "revanced_preference_screen_shorts_player" -> "revanced_preference_screen_shorts_icon"
            "revanced_preference_screen_video_filter" -> "revanced_preference_screen_video_icon"
            "revanced_swipe_gestures_lock_mode" -> "revanced_hide_player_flyout_menu_lock_screen_icon"
            "revanced_disable_hdr_auto_brightness" -> "revanced_disable_hdr_video_icon"
            else -> "${title}_icon"
        }
    }
    private val intentIcon = intentKey.associateWith { "${it}_icon" }
    private const val emptyIcon = "empty_icon"

    // endregion.

}
