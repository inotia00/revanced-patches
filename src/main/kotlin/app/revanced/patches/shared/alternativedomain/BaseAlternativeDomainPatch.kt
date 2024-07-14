package app.revanced.patches.shared.alternativedomain

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patches.shared.alternativedomain.fingerprints.MessageDigestImageUrlFingerprint
import app.revanced.patches.shared.alternativedomain.fingerprints.MessageDigestImageUrlParentFingerprint
import app.revanced.util.resultOrThrow

abstract class BaseAlternativeDomainPatch(
    private val classDescriptor: String
) : BytecodePatch(
    setOf(MessageDigestImageUrlParentFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        MessageDigestImageUrlFingerprint.resolve(
            context,
            MessageDigestImageUrlParentFingerprint.resultOrThrow().classDef
        )

        MessageDigestImageUrlFingerprint.resultOrThrow().mutableMethod.addInstructions(
            0, """
                invoke-static { p1 }, $classDescriptor->overrideImageURL(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
                """
        )

    }
}