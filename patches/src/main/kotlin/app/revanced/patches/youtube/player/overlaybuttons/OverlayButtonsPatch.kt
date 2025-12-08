package app.revanced.patches.youtube.player.overlaybuttons

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.patch.stringOption
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.dismiss.dismissPlayerHookPatch
import app.revanced.patches.youtube.utils.extension.Constants.OVERLAY_BUTTONS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.fix.bottomui.cfBottomUIPatch
import app.revanced.patches.youtube.utils.patch.PatchList.OVERLAY_BUTTONS
import app.revanced.patches.youtube.utils.pip.pipStateHookPatch
import app.revanced.patches.youtube.utils.playercontrols.injectControl
import app.revanced.patches.youtube.utils.playercontrols.playerControlsPatch
import app.revanced.patches.youtube.utils.playlist.playlistPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.videoEndMethod
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.printWarn
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.doRecursively
import app.revanced.util.lowerCaseOrThrow
import org.w3c.dom.Element

private const val EXTENSION_ALWAYS_REPEAT_CLASS_DESCRIPTOR =
    "$UTILS_PATH/AlwaysRepeatPatch;"

private val overlayButtonsBytecodePatch = bytecodePatch(
    description = "overlayButtonsBytecodePatch"
) {
    dependsOn(videoInformationPatch)

    execute {

        // region patch for always repeat

        videoEndMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $EXTENSION_ALWAYS_REPEAT_CLASS_DESCRIPTOR->alwaysRepeat()Z
                    move-result v0
                    if-eqz v0, :end
                    return-void
                    """, ExternalLabel("end", getInstruction(0))
            )
        }

        // endregion

    }
}

private const val MARGIN_MINIMUM = "0.1dip"
private const val MARGIN_DEFAULT = "2.5dip"
private const val MARGIN_WIDER = "5.0dip"

private const val DEFAULT_ICON = "thin"

@Suppress("unused")
val overlayButtonsPatch = resourcePatch(
    OVERLAY_BUTTONS.title,
    OVERLAY_BUTTONS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        overlayButtonsBytecodePatch,
        cfBottomUIPatch,
        dismissPlayerHookPatch,
        pipStateHookPatch,
        playerControlsPatch,
        playlistPatch,
        sharedResourceIdPatch,
        settingsPatch,
    )

    val iconTypeOption = stringOption(
        key = "iconType",
        default = DEFAULT_ICON,
        values = mapOf(
            "Bold" to "bold",
            "Rounded" to "rounded",
            "Thin" to DEFAULT_ICON
        ),
        title = "Icon type",
        description = "The icon type.",
        required = true
    )

    val bottomMarginOption = stringOption(
        key = "bottomMargin",
        default = MARGIN_DEFAULT,
        values = mapOf(
            "Default" to MARGIN_DEFAULT,
            "Minimum" to MARGIN_MINIMUM,
            "Wider" to MARGIN_WIDER,
        ),
        title = "Bottom margin",
        description = "The bottom margin for the overlay buttons and timestamp.",
        required = true
    )

    val widerButtonsSpace by booleanOption(
        key = "widerButtonsSpace",
        default = false,
        title = "Wider between-buttons space",
        description = "Prevent adjacent button presses by increasing the horizontal spacing between buttons.",
        required = true
    )

    val changeTopButtons by booleanOption(
        key = "changeTopButtons",
        default = true,
        title = "Change top buttons",
        description = "Change the icons at the top of the player.",
        required = true
    )

    execute {

        // Check patch options first.
        val iconType = iconTypeOption
            .lowerCaseOrThrow()

        var marginBottom = bottomMarginOption
            .lowerCaseOrThrow()

        try {
            val marginBottomFloat = marginBottom.split("dip")[0].toFloat()
            if (marginBottomFloat <= 0f) {
                printWarn("Patch option \"Bottom margin\" must be greater than 0, fallback to minimum.")
                marginBottom = MARGIN_MINIMUM
            }
        } catch (_: Exception) {
            printWarn("Patch option \"Bottom margin\" failed validation, fallback to default.")
            marginBottom = MARGIN_DEFAULT
        }

        val useWiderButtonsSpace = widerButtonsSpace == true

        // Inject hooks for overlay buttons.
        setOf(
            "AlwaysRepeatButton",
            "CopyVideoUrlButton",
            "CopyVideoUrlTimestampButton",
            "ExternalDownloadButton",
            "MuteVolumeButton",
            "PlayAllButton",
            "PlaybackSpeedDialogButton",
            "WhitelistButton",
        ).forEach { className ->
            injectControl("$OVERLAY_BUTTONS_PATH/${className};", false)
        }

        // Copy necessary resources for the overlay buttons.
        copyResources(
            "youtube/overlaybuttons/shared",
            ResourceGroup(
                "drawable",
                "playlist_repeat_button.xml",
                "playlist_shuffle_button.xml",
                "revanced_repeat_button.xml",
                "revanced_mute_volume_button.xml",
            )
        )

        // Apply the selected icon type to the overlay buttons.
        arrayOf(
            "xxxhdpi",
            "xxhdpi",
            "xhdpi",
            "hdpi",
            "mdpi"
        ).forEach { dpi ->
            copyResources(
                "youtube/overlaybuttons/$iconType",
                ResourceGroup(
                    "drawable-$dpi",
                    "ic_vr.png",
                    "quantum_ic_fullscreen_exit_grey600_24.png",
                    "quantum_ic_fullscreen_exit_white_24.png",
                    "quantum_ic_fullscreen_grey600_24.png",
                    "quantum_ic_fullscreen_white_24.png",
                    "revanced_copy_button.png",
                    "revanced_copy_timestamp_button.png",
                    "revanced_external_download_button.png",
                    "revanced_play_all_button.png",
                    "revanced_playback_speed_dialog_button.png",
                    "revanced_volume_muted_button.png",
                    "revanced_volume_unmuted_button.png",
                    "revanced_whitelist_button.png",
                    "yt_fill_arrow_repeat_white_24.png",
                    "yt_outline_arrow_repeat_1_white_24.png",
                    "yt_outline_arrow_shuffle_1_white_24.png",
                    "yt_outline_arrow_shuffle_black_24.png",
                    "yt_outline_list_play_arrow_black_24.png",
                    "yt_outline_list_play_arrow_white_24.png",
                    "yt_outline_screen_full_exit_white_24.png",
                    "yt_outline_screen_full_vd_theme_24.png",
                    "yt_outline_screen_full_white_24.png",
                    "yt_outline_screen_vertical_vd_theme_24.png",
                ),
                ResourceGroup(
                    "drawable",
                    "yt_outline_screen_vertical_vd_theme_24.xml"
                )
            )
        }

        // Merge XML nodes from the host to their respective XML files.
        copyXmlNode(
            "youtube/overlaybuttons/shared/host",
            "layout/youtube_controls_bottom_ui_container.xml",
            "android.support.constraint.ConstraintLayout"
        )

        document("res/layout/youtube_controls_bottom_ui_container.xml").use { document ->
            document.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                // Change the relationship between buttons
                node.getAttributeNode("yt:layout_constraintRight_toLeftOf")
                    ?.let { attribute ->
                        if (attribute.textContent == "@id/fullscreen_button") {
                            attribute.textContent = "@+id/revanced_playback_speed_dialog_button"
                        }
                    }
            }
        }

        arrayOf(
            "youtube_controls_bottom_ui_container.xml",
            "youtube_controls_fullscreen_button.xml",
            "youtube_controls_cf_fullscreen_button.xml"
        ).forEach { xmlFile ->
            val targetXml = get("res").resolve("layout").resolve(xmlFile)
            if (targetXml.exists()) {
                document("res/layout/$xmlFile").use { document ->
                    document.doRecursively loop@{ node ->
                        if (node !is Element) return@loop

                        // Change the relationship between buttons
                        node.getAttributeNode("yt:layout_constraintRight_toLeftOf")
                            ?.let { attribute ->
                                if (attribute.textContent == "@id/fullscreen_button") {
                                    attribute.textContent =
                                        "@+id/revanced_playback_speed_dialog_button"
                                }
                            }

                        node.getAttributeNode("yt:layout_constraintBottom_toTopOf")
                            ?.let { attribute ->
                                if (attribute.textContent == "@id/quick_actions_container") {
                                    attribute.textContent =
                                        "@+id/revanced_overlay_buttons_bottom_margin"
                                }
                            }

                        val (id, height, width) = Triple(
                            node.getAttribute("android:id"),
                            node.getAttribute("android:layout_height"),
                            node.getAttribute("android:layout_width")
                        )
                        val (heightIsNotZero, widthIsNotZero) = Pair(
                            height != "0.0dip",
                            width != "0.0dip",
                        )

                        val isButton =
                            id.endsWith("_button") && id != "@id/multiview_button" || id == "@id/youtube_controls_fullscreen_button_stub"

                        // Adjust TimeBar and Chapter bottom padding
                        val timBarItem = mutableMapOf(
                            "@id/time_bar_chapter_title" to "16.0dip",
                            "@id/timestamps_container" to "14.0dip"
                        )

                        val layoutHeightWidth = if (useWiderButtonsSpace)
                            "56.0dip"
                        else
                            "48.0dip"

                        if (isButton) {
                            node.setAttribute("android:paddingBottom", "12.0dip")
                            node.setAttribute("android:paddingTop", "12.0dip")
                            if (heightIsNotZero && widthIsNotZero) {
                                node.setAttribute("android:layout_height", layoutHeightWidth)
                                node.setAttribute("android:layout_width", layoutHeightWidth)
                            }
                        } else if (timBarItem.containsKey(id)) {
                            if (!useWiderButtonsSpace) {
                                node.setAttribute("android:paddingBottom", timBarItem.getValue(id))
                            }
                        }

                        if (useWiderButtonsSpace && id.endsWith("_placeholder")) {
                            node.setAttribute("android:layout_height", "56.0dip")
                            node.setAttribute("android:layout_width", "56.0dip")
                        }
                        
                        if (id.equals("@+id/revanced_overlay_buttons_bottom_margin")) {
                            node.setAttribute("android:layout_height", marginBottom)
                        } else if (id.equals("@id/time_bar_reference_view")) {
                            node.setAttribute(
                                "yt:layout_constraintBottom_toTopOf",
                                "@id/quick_actions_container"
                            )
                        }
                    }
                }
            }
        }

        if (changeTopButtons == true) {
            // Apply the selected icon type to the top buttons.
            arrayOf(
                "xxxhdpi",
                "xxhdpi",
                "xhdpi",
                "hdpi",
                "mdpi"
            ).forEach { dpi ->
                copyResources(
                    "youtube/overlaybuttons/$iconType",
                    ResourceGroup(
                        "drawable-$dpi",
                        "yt_outline_gear_white_24.png",
                        "yt_outline_chevron_down_white_24.png",
                        "quantum_ic_closed_caption_off_grey600_24.png",
                        "quantum_ic_closed_caption_off_white_24.png",
                        "quantum_ic_closed_caption_white_24.png"
                    )
                )
            }
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: PLAYER",
                "PREFERENCE_SCREENS: PLAYER_BUTTONS",
                "SETTINGS: OVERLAY_BUTTONS"
            ),
            OVERLAY_BUTTONS
        )

        // endregion

    }
}
