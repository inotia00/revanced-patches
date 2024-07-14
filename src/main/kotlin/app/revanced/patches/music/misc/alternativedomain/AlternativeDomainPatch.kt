package app.revanced.patches.music.misc.alternativedomain

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import java.io.Closeable

@Suppress("unused")
object AlternativeDomainPatch : BaseBytecodePatch(
    name = "Alternative domain",
    description = "Adds options to replace static images(avatars, playlist covers, etc.) domain.",
    dependencies = setOf(
        AlternativeDomainBytecodePatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
), Closeable {
    override fun execute(context: BytecodeContext) {
    }
    // Use Closeable for lexicographic arrangement of settings.
    override fun close() {
        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_use_alternative_domain",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_alternative_domain",
            "revanced_use_alternative_domain"
        )
    }
}
