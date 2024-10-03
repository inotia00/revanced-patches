@file:Suppress("DEPRECATION")

package app.revanced.patches.youtube.utils.integrations

import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.integrations.BaseIntegrationsPatch
import app.revanced.patches.youtube.utils.integrations.fingerprints.APIPlayerServiceFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.ApplicationInitFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.EmbeddedPlayerControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.EmbeddedPlayerFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.RemoteEmbedFragmentFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.RemoteEmbeddedPlayerFingerprint
import app.revanced.patches.youtube.utils.integrations.fingerprints.StandalonePlayerActivityFingerprint

@Patch(requiresIntegrations = true)
object IntegrationsPatch : BaseIntegrationsPatch(
    setOf(
        ApplicationInitFingerprint,
        StandalonePlayerActivityFingerprint,
        RemoteEmbeddedPlayerFingerprint,
        RemoteEmbedFragmentFingerprint,
        EmbeddedPlayerControlsOverlayFingerprint,
        EmbeddedPlayerFingerprint,
        APIPlayerServiceFingerprint,
    ),
)