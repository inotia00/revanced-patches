package app.revanced.patches.all.misc.versioncode

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.util.valueOrThrow
import org.w3c.dom.Element

@Patch(
    name = "Change version code",
    description = "Changes the version code of the app to the value specified in options.json. " +
            "Except when mounting, this can prevent app stores from updating the app and allow " +
            "the app to be installed over an existing installation that has a higher version code. " +
            "By default, the highest version code is set.",
    use = false,
)
@Suppress("unused")
object ChangeVersionCodePatch : ResourcePatch() {
    private const val MAX_VALUE = Int.MAX_VALUE.toString()

    private val ChangeVersionCode by booleanPatchOption(
        key = "ChangeVersionCode",
        default = false,
        title = "Change version code",
        description = "Changes the version code of the app.",
        required = true
    )

    private val VersionCode = stringPatchOption(
        key = "VersionCode",
        default = MAX_VALUE,
        title = "Version code",
        description = "The version code to use. (1 ~ $MAX_VALUE)",
        required = true
    )

    override fun execute(context: ResourceContext) {
        if (ChangeVersionCode == false) {
            println("INFO: Version code will remain unchanged as 'ChangeVersionCode' is false.")
            return
        }

        val versionCodeString = VersionCode.valueOrThrow()
        val versionCode: Int

        try {
            versionCode = Integer.parseInt(versionCodeString)
        } catch (e: NumberFormatException) {
            throw throwVersionCodeException(versionCodeString)
        }

        if (versionCode < 1) {
            throw throwVersionCodeException(versionCodeString)
        }

        context.document["AndroidManifest.xml"].use { document ->
            val manifestElement = document.getElementsByTagName("manifest").item(0) as Element
            manifestElement.setAttribute("android:versionCode", "$versionCode")
        }
    }

    private fun throwVersionCodeException(versionCodeString: String): PatchException =
        PatchException(
            "Invalid versionCode: $versionCodeString, " +
                    "Version code should be larger than 1 and smaller than $MAX_VALUE."
        )
}
