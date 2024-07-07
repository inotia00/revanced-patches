package app.revanced.patches.music.misc.alternativedomain

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.music.misc.alternativedomain.fingerprints.MessageDigestImageUrlFingerprint
import app.revanced.patches.music.misc.alternativedomain.fingerprints.MessageDigestImageUrlParentFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.ALTERNATIVE_DOMAIN_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object AlternativeDomainPatch : BaseBytecodePatch(
    name = "Alternative domain",
    description = "Adds options to replace static images(avatars, playlist covers, etc.) domain.",
    dependencies = setOf(
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        MessageDigestImageUrlParentFingerprint
    )
) {

    override fun execute(context: BytecodeContext) {
        fun MethodFingerprint.alsoResolve(fingerprint: MethodFingerprint) =
            also { resolve(context, fingerprint.resultOrThrow().classDef) }.resultOrThrow()

        fun MethodFingerprint.resolveAndLetMutableMethod(
            fingerprint: MethodFingerprint,
            block: (MutableMethod) -> Unit
        ) = alsoResolve(fingerprint).also { block(it.mutableMethod) }

        MessageDigestImageUrlFingerprint.resolveAndLetMutableMethod(
            MessageDigestImageUrlParentFingerprint
        ) {
            it.addInstructions(
                0,
                """
                invoke-static { p1 }, $ALTERNATIVE_DOMAIN_CLASS_DESCRIPTOR->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                """
            )
        }

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