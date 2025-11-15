package app.revanced.patches.youtube.utils.auth

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.shared.extension.Constants.EXTENSION_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.request.buildRequestPatch
import app.revanced.patches.youtube.utils.request.hookBuildRequest
import app.revanced.patches.youtube.utils.request.hookInitPlaybackBuildRequest
import app.revanced.util.fingerprint.methodOrThrow

private const val EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/innertube/utils/AuthUtils;"

val authHookPatch = bytecodePatch(
    description = "authHookPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        buildRequestPatch,
    )

    execute {
        // Get incognito status and data sync id.
        accountIdentityFingerprint.methodOrThrow().addInstruction(
            1,
            "invoke-static {p3, p4}, $EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setAccountIdentity(Ljava/lang/String;Z)V"
        )

        // Get the header to use the auth token.
        hookBuildRequest("$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
        hookInitPlaybackBuildRequest("$EXTENSION_AUTH_UTILS_CLASS_DESCRIPTOR->setRequestHeaders(Ljava/lang/String;Ljava/util/Map;)V")
    }
}