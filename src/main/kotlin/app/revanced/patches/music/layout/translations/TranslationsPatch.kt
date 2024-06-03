package app.revanced.patches.music.layout.translations

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.translations.APP_LANGUAGES
import app.revanced.patches.shared.translations.TranslationsUtils.invoke
import app.revanced.util.patch.BaseResourcePatch

@Suppress("unused")
object TranslationsPatch : BaseResourcePatch(
    name = "Translations YouTube Music",
    description = "Add translations or remove string resources.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    // Array of supported translations, each represented by its language code.
    private val TRANSLATIONS = arrayOf(
        "bg-rBG", "bn", "cs-rCZ", "el-rGR", "es-rES", "fr-rFR", "hu-rHU", "id-rID", "in", "it-rIT",
        "ja-rJP", "ko-rKR", "nl-rNL", "pl-rPL", "pt-rBR", "ro-rRO", "ru-rRU", "tr-rTR", "uk-rUA",
        "vi-rVN", "zh-rCN", "zh-rTW"
    )

    private var CustomTranslations by stringPatchOption(
        key = "CustomTranslations",
        default = "",
        title = "Custom translations",
        description = """
            The file path to the 'strings.xml' file.
            Please note that applying the 'strings.xml' file will overwrite all existing language translations.
            """.trimIndent()
    )

    private var SelectedTranslations by stringPatchOption(
        key = "SelectedTranslations",
        default = TRANSLATIONS.joinToString(", "),
        title = "Selected translations",
        description = "Selected translations that will be added."
    )

    private var SelectedStringResources by stringPatchOption(
        key = "SelectedStringResources",
        default = APP_LANGUAGES.joinToString(", "),
        title = "Selected string resources",
        description = """
            Selected string resources that will be kept, string resources that are not in the list will be removed from the app.
            Default string resource, English, is not removed.
            """.trimIndent()
    )

    override fun execute(context: ResourceContext) {
        context.invoke(
            CustomTranslations, SelectedTranslations, SelectedStringResources,
            TRANSLATIONS, "music"
        )
    }
}
