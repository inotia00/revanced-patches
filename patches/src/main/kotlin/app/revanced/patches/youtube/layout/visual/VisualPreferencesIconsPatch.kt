package app.revanced.patches.youtube.layout.visual

import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patches.youtube.layout.branding.icon.customBrandingIconPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.patch.PatchList.VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.RVX_PREFERENCE_PATH
import app.revanced.patches.youtube.utils.settings.ResourceUtils.YOUTUBE_SETTINGS_PATH
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyResources
import app.revanced.util.doRecursively
import app.revanced.util.getStringOptionValue
import app.revanced.util.underBarOrThrow
import org.w3c.dom.Element

private const val DEFAULT_ICON = "extension"
private const val EMPTY_ICON = "empty_icon"

@Suppress("unused")
val visualPreferencesIconsPatch = resourcePatch(
    VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE.title,
    VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    val settingsMenuIconOption = stringOption(
        key = "settingsMenuIcon",
        default = DEFAULT_ICON,
        values = mapOf(
            "Custom branding icon" to "custom_branding_icon",
            "Extension" to DEFAULT_ICON,
            "Gear" to "gear",
            "ReVanced" to "revanced",
            "ReVanced Colored" to "revanced_colored",
            "RVX Letters" to "rvx_letters",
            "RVX Letters Bold" to "rvx_letters_bold",
            "YT alt" to "yt_alt",
        ),
        title = "RVX settings menu icon",
        description = "The icon for the RVX settings menu.",
        required = true,
    )

    val applyToAll by booleanOption(
        key = "applyToAll",
        default = false,
        title = "Apply to all settings menu",
        description = """
            Whether to apply Visual preferences icons to all settings menus.

            If true: icons are applied to the parent PreferenceScreen of YouTube settings, the parent PreferenceScreen of RVX settings and the RVX sub-settings (if supported).

            If false: icons are applied only to the parent PreferenceScreen of YouTube settings and RVX settings.
            """.trimIndentMultiline(),
        required = true
    )

    lateinit var preferenceIcon: Map<String, String>

    fun Set<String>.setPreferenceIcon() = associateWith { title ->
        when (title) {
            // Internal RVX settings
            "revanced_alt_thumbnail_home" -> "revanced_hide_navigation_home_button_icon"
            "revanced_alt_thumbnail_library" -> "revanced_preference_screen_video_icon"
            "revanced_alt_thumbnail_player" -> "revanced_preference_screen_player_icon"
            "revanced_alt_thumbnail_search" -> "revanced_hide_shorts_shelf_search_icon"
            "revanced_alt_thumbnail_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_change_share_sheet" -> "revanced_hide_share_button_icon"
            "revanced_change_shorts_background_repeat_state" -> "revanced_change_shorts_repeat_state_icon"
            "revanced_default_app_settings" -> "revanced_preference_screen_settings_menu_icon"
            "revanced_disable_shorts_background_playback" -> "offline_key_icon"
            "revanced_settings_search_history" -> "history_key_icon"
            "revanced_hide_download_button" -> "revanced_overlay_button_external_downloader_icon"
            "revanced_hide_keyword_content_home" -> "revanced_hide_navigation_home_button_icon"
            "revanced_hide_keyword_content_search" -> "revanced_hide_shorts_shelf_search_icon"
            "revanced_hide_keyword_content_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_hide_like_dislike_button" -> "sb_voting_button_icon"
            "revanced_hide_navigation_label" -> "revanced_swipe_text_overlay_size_icon"
            "revanced_hide_navigation_library_button" -> "revanced_preference_screen_video_icon"
            "revanced_hide_navigation_notifications_button" -> "notification_key_icon"
            "revanced_hide_navigation_shorts_button" -> "revanced_preference_screen_shorts_icon"
            "revanced_hide_player_autoplay_button" -> "revanced_change_player_flyout_menu_toggle_icon"
            "revanced_hide_player_captions_button" -> "captions_key_icon"
            "revanced_hide_player_flyout_menu_ambient_mode" -> "revanced_preference_screen_ambient_mode_icon"
            "revanced_hide_player_flyout_menu_captions" -> "captions_key_icon"
            "revanced_hide_player_flyout_menu_enhanced_bitrate" -> "video_quality_settings_key_icon"
            "revanced_hide_player_flyout_menu_listen_with_youtube_music" -> "revanced_hide_player_youtube_music_button_icon"
            "revanced_hide_player_flyout_menu_loop_video" -> "revanced_overlay_button_always_repeat_icon"
            "revanced_hide_player_flyout_menu_more_info" -> "about_key_icon"
            "revanced_hide_player_flyout_menu_pip" -> "offline_key_icon"
            "revanced_hide_player_flyout_menu_premium_controls" -> "premium_early_access_browse_page_key_icon"
            "revanced_hide_player_flyout_menu_report" -> "revanced_hide_report_button_icon"
            "revanced_hide_player_fullscreen_button" -> "revanced_preference_screen_fullscreen_icon"
            "revanced_hide_shorts_shelf" -> "revanced_preference_screen_shorts_icon"
            "revanced_hide_shorts_shelf_channel" -> "account_switcher_key_icon"
            "revanced_hide_shorts_shelf_home_related_videos" -> "revanced_hide_navigation_home_button_icon"
            "revanced_hide_shorts_shelf_subscriptions" -> "revanced_hide_navigation_subscriptions_button_icon"
            "revanced_open_shorts_in_regular_player" -> "revanced_preference_screen_player_icon"
            "revanced_preference_screen_account_menu" -> "account_switcher_key_icon"
            "revanced_preference_screen_channel_bar" -> "account_switcher_key_icon"
            "revanced_preference_screen_channel_page" -> "account_switcher_key_icon"
            "revanced_preference_screen_feed_flyout_menu" -> "revanced_preference_screen_player_flyout_menu_icon"
            "revanced_preference_screen_general" -> "general_key_icon"
            "revanced_preference_screen_haptic_feedback" -> "revanced_swipe_haptic_feedback_icon"
            "revanced_preference_screen_hook_buttons" -> "revanced_preference_screen_import_export_icon"
            "revanced_preference_screen_miniplayer" -> "offline_key_icon"
            "revanced_preference_screen_patch_information" -> "about_key_icon"
            "revanced_preference_screen_sb" -> "sb_create_new_segment_icon"
            "revanced_preference_screen_shorts_player" -> "revanced_preference_screen_shorts_icon"
            "revanced_preference_screen_snack_bar" -> "revanced_preference_screen_action_buttons_icon"
            "revanced_preference_screen_video_filter" -> "revanced_preference_screen_video_icon"
            "revanced_preference_screen_watch_history" -> "history_key_icon"
            "revanced_swipe_gestures_lock_mode" -> "revanced_hide_player_flyout_menu_lock_screen_icon"
            "revanced_swipe_overlay_progress_brightness_color" -> "revanced_swipe_brightness_icon"
            "revanced_swipe_overlay_progress_volume_color" -> "revanced_swipe_volume_icon"
            else -> "${title}_icon"
        }
    }

    execute {
        // Check patch options first.
        val selectedIconType = settingsMenuIconOption
            .underBarOrThrow()

        val appIconOption = customBrandingIconPatch
            .getStringOptionValue("appIcon")

        val customBrandingIconType = appIconOption
            .underBarOrThrow()

        if (applyToAll == true) {
            preferenceKey += rvxPreferenceKey
        }

        preferenceIcon = preferenceKey.setPreferenceIcon()

        // region copy shared resources.

        arrayOf(
            ResourceGroup(
                "drawable",
                *preferenceIcon.values.map { "$it.xml" }.toTypedArray()
            ),
            ResourceGroup(
                "drawable-xxhdpi",
                "$EMPTY_ICON.png"
            ),
        ).forEach { resourceGroup ->
            copyResources("youtube/visual/shared", resourceGroup)
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
            "revanced_settings_key_icon.xml"
        )

        try {
            copyResources(iconPath, resourceGroup)
        } catch (_: Exception) {
            // Ignore if resource copy fails

            // Add a fallback extended icon
            // It's needed if someone provides custom path to icon(s) folder
            // but custom branding icons for Extended setting are predefined,
            // so it won't copy custom branding icon
            // and will raise an error without fallback icon
            copyResources(fallbackIconPath, resourceGroup)
        }

        // endregion.

        addPreference(VISUAL_PREFERENCES_ICONS_FOR_YOUTUBE)
    }

    finalize {
        // region set visual preferences icon.

        arrayOf(
            RVX_PREFERENCE_PATH,
            YOUTUBE_SETTINGS_PATH
        ).forEach { xmlFile ->
            document(xmlFile).use { document ->
                document.doRecursively loop@{ node ->
                    if (node !is Element) return@loop

                    node.getAttributeNode("android:key")
                        ?.textContent
                        ?.removePrefix("@string/")
                        ?.let { title ->
                            val drawableName = when (title) {
                                in preferenceKey -> preferenceIcon[title]

                                // Add custom RVX settings menu icon
                                in intentKey -> intentIcon[title]
                                in emptyTitles -> EMPTY_ICON
                                else -> null
                            }
                            if (drawableName == EMPTY_ICON &&
                                applyToAll == false
                            ) return@loop

                            drawableName?.let {
                                node.setAttribute("android:icon", "@drawable/$it")
                            }
                        }
                }

                // overlay buttons empty icon
                if (applyToAll == true) {
                    val tags = document.getElementsByTagName("PreferenceScreen")
                    List(tags.length) { tags.item(it) as Element }
                        .find { it.getAttribute("android:key") == "revanced_preference_screen_player_buttons" }
                        ?.let { pref ->
                            val childNodes = pref.childNodes
                            for (i in 0 until childNodes.length) {
                                val node = childNodes.item(i) as? Element
                                node?.getAttributeNode("android:key")
                                    ?.textContent
                                    ?.let { key ->
                                        if (emptyTitlesOverlayButtons.contains(key)) {
                                            node.setAttribute(
                                                "android:icon",
                                                "@drawable/$EMPTY_ICON"
                                            )
                                        }
                                    }
                            }
                        }
                }
            }
        }

        // endregion.
    }
}

private var preferenceKey = setOf(
    // YouTube settings.
    "about_key",
    "accessibility_settings_key",
    "account_switcher_key",
    "auto_play_key",
    "billing_and_payment_key",
    "captions_key",
    "connected_accounts_browse_page_key",
    "data_saving_settings_key",
    "general_key",
    "history_key",
    "live_chat_key",
    "notification_key",
    "offline_key",
    "pair_with_tv_key",
    "parent_tools_key",
    "playback_key",
    "premium_early_access_browse_page_key",
    "privacy_key",
    "subscription_product_setting_key",
    "video_quality_settings_key",
    "your_data_key",

    // RVX settings.
    "revanced_preference_screen_ads",
    "revanced_preference_screen_alt_thumbnails",
    "revanced_preference_screen_feed",
    "revanced_preference_screen_general",
    "revanced_preference_screen_player",
    "revanced_preference_screen_shorts",
    "revanced_preference_screen_swipe_controls",
    "revanced_preference_screen_video",
    "revanced_preference_screen_ryd",
    "revanced_preference_screen_return_youtube_username",
    "revanced_preference_screen_sb",
    "revanced_preference_screen_misc",
)

private var rvxPreferenceKey = setOf(
    // Internal RVX settings (items without prefix are listed first, others are sorted alphabetically)
    "gms_core_settings",
    "sb_create_new_segment",
    "sb_voting_button",

    "revanced_alt_thumbnail_home",
    "revanced_alt_thumbnail_library",
    "revanced_alt_thumbnail_player",
    "revanced_alt_thumbnail_search",
    "revanced_alt_thumbnail_subscriptions",
    "revanced_bypass_url_redirects",
    "revanced_change_player_flyout_menu_toggle",
    "revanced_change_share_sheet",
    "revanced_change_shorts_background_repeat_state",
    "revanced_change_shorts_repeat_state",
    "revanced_default_app_settings",
    "revanced_disable_hdr_auto_brightness",
    "revanced_disable_quic_protocol",
    "revanced_disable_resuming_shorts_player",
    "revanced_disable_shorts_background_playback",
    "revanced_disable_swipe_to_switch_video",
    "revanced_settings_search_history",
    "revanced_hide_ask_button",
    "revanced_hide_clip_button",
    "revanced_hide_download_button",
    "revanced_hide_hype_button",
    "revanced_hide_keyword_content_comments",
    "revanced_hide_keyword_content_home",
    "revanced_hide_keyword_content_search",
    "revanced_hide_keyword_content_subscriptions",
    "revanced_hide_like_dislike_button",
    "revanced_hide_navigation_create_button",
    "revanced_hide_navigation_home_button",
    "revanced_hide_navigation_label",
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
    "revanced_hide_player_flyout_menu_enhanced_bitrate",
    "revanced_hide_player_flyout_menu_help",
    "revanced_hide_player_flyout_menu_listen_with_youtube_music",
    "revanced_hide_player_flyout_menu_lock_screen",
    "revanced_hide_player_flyout_menu_loop_video",
    "revanced_hide_player_flyout_menu_more_info",
    "revanced_hide_player_flyout_menu_pip",
    "revanced_hide_player_flyout_menu_playback_speed",
    "revanced_hide_player_flyout_menu_premium_controls",
    "revanced_hide_player_flyout_menu_quality_header",
    "revanced_hide_player_flyout_menu_report",
    "revanced_hide_player_flyout_menu_sleep_timer",
    "revanced_hide_player_flyout_menu_stable_volume",
    "revanced_hide_player_flyout_menu_stats_for_nerds",
    "revanced_hide_player_flyout_menu_watch_in_vr",
    "revanced_hide_player_fullscreen_button",
    "revanced_hide_player_previous_next_button",
    "revanced_hide_player_youtube_music_button",
    "revanced_hide_playlist_button",
    "revanced_hide_remix_button",
    "revanced_hide_report_button",
    "revanced_hide_rewards_button",
    "revanced_hide_share_button",
    "revanced_hide_shop_button",
    "revanced_hide_shorts_floating_button",
    "revanced_hide_shorts_shelf",
    "revanced_hide_shorts_shelf_channel",
    "revanced_hide_shorts_shelf_history",
    "revanced_hide_shorts_shelf_home_related_videos",
    "revanced_hide_shorts_shelf_search",
    "revanced_hide_shorts_shelf_subscriptions",
    "revanced_hide_thanks_button",
    "revanced_language",
    "revanced_open_links_externally",
    "revanced_open_shorts_in_regular_player",
    "revanced_overlay_button_always_repeat",
    "revanced_overlay_button_copy_video_url",
    "revanced_overlay_button_copy_video_url_timestamp",
    "revanced_overlay_button_external_downloader",
    "revanced_overlay_button_mute_volume",
    "revanced_overlay_button_play_all",
    "revanced_overlay_button_speed_dialog",
    "revanced_overlay_button_whitelist",
    "revanced_preference_screen_account_menu",
    "revanced_preference_screen_action_buttons",
    "revanced_preference_screen_ambient_mode",
    "revanced_preference_screen_carousel_shelf",
    "revanced_preference_screen_category_bar",
    "revanced_preference_screen_channel_bar",
    "revanced_preference_screen_channel_page",
    "revanced_preference_screen_comments",
    "revanced_preference_screen_community_posts",
    "revanced_preference_screen_custom_filter",
    "revanced_preference_screen_debugging",
    "revanced_preference_screen_feed_flyout_menu",
    "revanced_preference_screen_fullscreen",
    "revanced_preference_screen_haptic_feedback",
    "revanced_preference_screen_hook_buttons",
    "revanced_preference_screen_import_export",
    "revanced_preference_screen_miniplayer",
    "revanced_preference_screen_navigation_bar",
    "revanced_preference_screen_patch_information",
    "revanced_preference_screen_player_buttons",
    "revanced_preference_screen_player_flyout_menu",
    "revanced_preference_screen_seekbar",
    "revanced_preference_screen_settings_menu",
    "revanced_preference_screen_shorts_player",
    "revanced_preference_screen_snack_bar",
    "revanced_preference_screen_spoof_streaming_data",
    "revanced_preference_screen_toolbar",
    "revanced_preference_screen_video_description",
    "revanced_preference_screen_video_filter",
    "revanced_preference_screen_watch_history",
    "revanced_sanitize_sharing_links",
    "revanced_swipe_brightness",
    "revanced_swipe_gestures_lock_mode",
    "revanced_swipe_haptic_feedback",
    "revanced_swipe_lowest_value_enable_auto_brightness",
    "revanced_swipe_overlay_background_opacity",
    "revanced_swipe_overlay_progress_brightness_color",
    "revanced_swipe_overlay_progress_volume_color",
    "revanced_swipe_overlay_rect_size",
    "revanced_swipe_overlay_style",
    "revanced_swipe_overlay_timeout",
    "revanced_swipe_press_to_engage",
    "revanced_swipe_save_and_restore_brightness",
    "revanced_swipe_text_overlay_size",
    "revanced_swipe_threshold",
    "revanced_swipe_volume",
    "revanced_switch_create_with_notifications_button",
)

private val intentKey = setOf(
    "revanced_settings_key",
)

val intentIcon = intentKey.associateWith { "${it}_icon" }

private val emptyTitles = setOf(
    "revanced_disable_like_dislike_glow",
    "revanced_disable_swipe_to_enter_fullscreen_mode_below_the_player",
    "revanced_disable_swipe_to_enter_fullscreen_mode_in_the_player",
    "revanced_disable_swipe_to_exit_fullscreen_mode",
    "revanced_enable_narrow_navigation_buttons",
    "revanced_fix_swipe_tap_and_hold_speed",
    "revanced_hide_player_flyout_menu_captions_footer",
    "revanced_hide_player_flyout_menu_quality_footer",
    "revanced_hide_stop_ads_button",
    "revanced_swipe_brightness_distance_dip",
    "revanced_swipe_volumes_sensitivity",
)

private val emptyTitlesOverlayButtons = setOf(
    "revanced_overlay_button_external_downloader_queue_manager",
    "revanced_external_downloader_package_name_video",
    "revanced_overlay_button_speed_dialog_type",
    "revanced_overlay_button_play_all_type",
    "revanced_whitelist_settings",
)