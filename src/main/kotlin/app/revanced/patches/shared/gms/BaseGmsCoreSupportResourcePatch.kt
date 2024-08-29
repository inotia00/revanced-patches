package app.revanced.patches.shared.gms

import app.revanced.patcher.PatchClass
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.valueOrThrow
import org.w3c.dom.Element
import org.w3c.dom.Node

/**
 * Abstract resource patch that allows Google apps to run without root and under a different package name
 * by using GmsCore instead of Google Play Services.
 *
 * @param fromPackageName The package name of the original app.
 * @param spoofedPackageSignature The signature of the package to spoof to.
 * @param dependencies Additional dependencies of this patch.
 */
@Suppress("DEPRECATION", "PrivatePropertyName", "PropertyName", "unused")
abstract class BaseGmsCoreSupportResourcePatch(
    private val fromPackageName: String,
    private val spoofedPackageSignature: String,
    dependencies: Set<PatchClass> = setOf(),
) : ResourcePatch(
    dependencies = dependencies
) {
    internal val GmsCoreVendorGroupId =
        stringPatchOption(
            key = "GmsCoreVendorGroupId",
            default = DEFAULT_GMS_CORE_VENDOR_GROUP_ID,
            values =
            mapOf(
                "ReVanced" to DEFAULT_GMS_CORE_VENDOR_GROUP_ID,
            ),
            title = "GmsCore vendor group ID",
            description = "The vendor's group ID for GmsCore.",
            required = true,
        ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) }

    private val CheckGmsCore by booleanPatchOption(
        key = "CheckGmsCore",
        default = true,
        title = "Check GmsCore",
        description = """
            Check if GmsCore is installed on the device and has battery optimizations disabled when the app starts. 
            
            If GmsCore is not installed the app will not work, so disabling this is not recommended.
            """.trimIndentMultiline(),
        required = true,
    )
    internal val PackageNameYouTube = stringPatchOption(
        key = "PackageNameYouTube",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE
        ),
        title = "Package name of YouTube",
        description = "The name of the package to use in GmsCore support.",
        required = true
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) && it != ORIGINAL_PACKAGE_NAME_YOUTUBE }

    internal val PackageNameYouTubeMusic = stringPatchOption(
        key = "PackageNameYouTubeMusic",
        default = DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC,
        values = mapOf(
            "Clone" to CLONE_PACKAGE_NAME_YOUTUBE_MUSIC,
            "Default" to DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC
        ),
        title = "Package name of YouTube Music",
        description = "The name of the package to use in GmsCore support.",
        required = true
    ) { it!!.matches(Regex(PACKAGE_NAME_REGEX_PATTERN)) && it != ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC }

    private val gmsCoreVendorGroupId by GmsCoreVendorGroupId

    override fun execute(context: ResourceContext) {
        context.patchManifest()
        context.addSpoofingMetadata()
    }

    /**
     * Add metadata to manifest to support spoofing the package name and signature of GmsCore.
     */
    private fun ResourceContext.addSpoofingMetadata() {
        fun Node.adoptChild(
            tagName: String,
            block: Element.() -> Unit,
        ) {
            val child = ownerDocument.createElement(tagName)
            child.block()
            appendChild(child)
        }

        xmlEditor["AndroidManifest.xml"].use { editor ->
            val document = editor.file

            val applicationNode =
                document
                    .getElementsByTagName("application")
                    .item(0)

            // Spoof package name and signature.
            applicationNode.adoptChild("meta-data") {
                setAttribute(
                    "android:name",
                    "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_NAME"
                )
                setAttribute("android:value", fromPackageName)
            }

            applicationNode.adoptChild("meta-data") {
                setAttribute(
                    "android:name",
                    "$gmsCoreVendorGroupId.android.gms.SPOOFED_PACKAGE_SIGNATURE"
                )
                setAttribute("android:value", spoofedPackageSignature)
            }

            // GmsCore presence detection in ReVanced Integrations.
            applicationNode.adoptChild("meta-data") {
                // TODO: The name of this metadata should be dynamic.
                setAttribute("android:name", "app.revanced.MICROG_PACKAGE_NAME")
                setAttribute("android:value", "$gmsCoreVendorGroupId.android.gms")
            }
        }
    }

    /**
     * Patch the manifest to support GmsCore.
     */
    private fun ResourceContext.patchManifest() {
        val packageName = getPackageName(fromPackageName)

        val manifest = this["AndroidManifest.xml"].readText()
        this["AndroidManifest.xml"].writeText(
            manifest.replace(
                "package=\"$fromPackageName",
                "package=\"$packageName",
            ).replace(
                "android:authorities=\"$fromPackageName",
                "android:authorities=\"$packageName",
            ).replace(
                "$fromPackageName.permission.C2D_MESSAGE",
                "$packageName.permission.C2D_MESSAGE",
            ).replace(
                "$fromPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
                "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            ).replace(
                "com.google.android.c2dm",
                "$gmsCoreVendorGroupId.android.c2dm",
            ),
        )

        // 'QUERY_ALL_PACKAGES' permission is required,
        // To check whether apps such as GmsCore, YouTube or YouTube Music are installed on the device.
        xmlEditor["AndroidManifest.xml"].use { editor ->
            editor.file.getElementsByTagName("manifest").item(0).also {
                it.appendChild(it.ownerDocument.createElement("uses-permission").also { element ->
                    element.setAttribute("android:name", "android.permission.QUERY_ALL_PACKAGES")
                })
            }
        }
    }

    private fun getPackageName(originalPackageName: String): String {
        if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE) {
            return PackageNameYouTube.valueOrThrow()
        } else if (originalPackageName == ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC) {
            return PackageNameYouTubeMusic.valueOrThrow()
        }
        throw PatchException("Unknown package name!")
    }

    companion object {
        internal const val DEFAULT_GMS_CORE_VENDOR_GROUP_ID = "app.revanced"

        private const val CLONE_PACKAGE_NAME_YOUTUBE = "com.rvx.android.youtube"
        private const val DEFAULT_PACKAGE_NAME_YOUTUBE = "app.rvx.android.youtube"
        internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE = "com.google.android.youtube"

        private const val CLONE_PACKAGE_NAME_YOUTUBE_MUSIC = "com.rvx.android.apps.youtube.music"
        private const val DEFAULT_PACKAGE_NAME_YOUTUBE_MUSIC = "app.rvx.android.apps.youtube.music"
        internal const val ORIGINAL_PACKAGE_NAME_YOUTUBE_MUSIC =
            "com.google.android.apps.youtube.music"

        private const val PACKAGE_NAME_REGEX_PATTERN = "^[a-z]\\w*(\\.[a-z]\\w*)+\$"
    }
}
