package app.revanced.extension.shared.patches;

import static app.revanced.extension.shared.patches.AppCheckPatch.IS_YOUTUBE;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class FullscreenAdsPatch {
    private static final BooleanSetting HIDE_FULLSCREEN_ADS =
            BaseSettings.HIDE_FULLSCREEN_ADS;
    private static final ByteArrayFilterGroup filter =
            new ByteArrayFilterGroup(
                    HIDE_FULLSCREEN_ADS,
                    "_interstitial"
            );

    /**
     * Whether the last built dialog contains an ad
     */
    private static boolean isFullscreenAds = false;

    /**
     * {@link Dialog#dismiss()} can be used after {@link Dialog#show()}
     * When {@link Dialog#show()} is invoked, byte buffer register may not exist
     * (byte buffer register has already been assigned to another value)
     * <p>
     * Therefore, make sure that the dialog contains the ads at the beginning of the Method
     *
     * @param bytes proto buffer array
     */
    public static void checkDialog(byte[] bytes) {
        isFullscreenAds = bytes != null &&
                filter.check(bytes).isFiltered();
    }

    /**
     * Called after {@link #checkDialog(byte[])}
     *
     * @param customDialog Custom dialog which bound by litho
     *                     Can be cast as {@link Dialog} or {@link DialogInterface}
     */
    public static void dismissDialog(Object customDialog) {
        if (isFullscreenAds && customDialog instanceof Dialog dialog) {
            Window window = dialog.getWindow();
            if (window != null) {
                // Set the dialog size to 0 before closing.
                // If the dialog is not resized to 0, it will remain visible for about a second before closing.
                WindowManager.LayoutParams params = window.getAttributes();
                params.height = 0;
                params.width = 0;

                // Change the size of dialog to 0.
                window.setAttributes(params);

                // Disable dialog's background dim.
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // Restore window flags.
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);

                // Restore decorView visibility.
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }

            // Dismiss dialog.
            dialog.dismiss();

            if (BaseSettings.DEBUG_TOAST_ON_ERROR.get()
                    || (!IS_YOUTUBE && BaseSettings.DEBUG.get())) {
                Utils.showToastShort(str("revanced_fullscreen_ads_closed_toast"));
            }
        }
    }

    /**
     * Injection point.
     * Invoke only in old clients.
     */
    public static void hideFullscreenAds(View view) {
        hideViewBy0dpUnderCondition(
                HIDE_FULLSCREEN_ADS,
                view
        );
    }
}
