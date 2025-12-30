package app.revanced.extension.shared.patches.auth;

import static app.revanced.extension.shared.patches.auth.requests.AuthRequest.fetch;
import static app.revanced.extension.shared.patches.auth.requests.AuthRoutes.RequestType;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getDialogBuilder;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Pair;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import app.revanced.extension.shared.settings.preference.SharedPrefCategory;
import app.revanced.extension.shared.ui.CustomDialog;
import app.revanced.extension.shared.utils.BaseThemeUtils;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public class YouTubeVRAuthPatch {
    /**
     * The last time the access token was updated.
     */
    private static long lastTimeAccessTokenUpdated = 0L;

    /**
     * Access token expiration time.
     */
    private static long accessTokenExpiration = 1770 * 1000L;

    public static void checkAccessToken() {
        if (isRefreshTokenAvailable()) {
            setAccessToken();
        }
    }

    public static void clearAll() {
        saveDeviceCode("");
        saveRefreshToken("");
        lastTimeAccessTokenUpdated = 0L;
        accessTokenExpiration = 1770 * 1000L;
        authorization = "";

        Utils.showToastShort(str("revanced_spoof_streaming_data_sign_in_android_vr_toast_reset"));
    }

    public static void setActivationCode(Context context) {
        JSONObject jsonObject =
                fetch(RequestType.GET_ACTIVATION_CODE, null);

        if (jsonObject != null) {
            try {
                String deviceCode = jsonObject.getString("device_code");
                String activationCode = jsonObject.getString("user_code");

                saveDeviceCode(deviceCode);

                String dialogTitle = str("revanced_spoof_streaming_data_sign_in_android_vr_activation_code_dialog_title");
                String dialogMessage = str("revanced_spoof_streaming_data_sign_in_android_vr_activation_code_dialog_message", activationCode);
                String okButtonText = str("revanced_spoof_streaming_data_sign_in_android_vr_activation_code_dialog_open_website_text");
                Runnable onOkClick = () -> {
                    Utils.setClipboard(
                            activationCode,
                            str("revanced_spoof_streaming_data_sign_in_android_vr_activation_code_toast_copy", activationCode)
                    );
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://yt.be/activate"));
                    context.startActivity(i);
                };

                if (BaseThemeUtils.isSupportModernDialog) {
                    Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                            context,
                            // Title.
                            dialogTitle,
                            // Message.
                            dialogMessage,
                            // No EditText.
                            null,
                            // OK button text.
                            okButtonText,
                            // OK button action.
                            onOkClick,
                            // Cancel button action (dismiss only).
                            null,
                            // Neutral button text.
                            null,
                            // Neutral button action.
                            null,
                            // Dismiss dialog when onNeutralClick.
                            false
                    );
                    dialogPair.first.show();
                } else {
                    // For YouTube Music
                    getDialogBuilder(context)
                            .setTitle(dialogTitle)
                            .setMessage(dialogMessage)
                            .setPositiveButton(okButtonText, (dialog, id) -> onOkClick.run())
                            .show();
                }
            } catch (JSONException ex) {
                Logger.printException(() -> "setActivationCode failed", ex);
            }
        }
    }

    public static void setRefreshToken() {
        JSONObject jsonObject =
                fetch(RequestType.GET_REFRESH_TOKEN, getDeviceCode());

        if (jsonObject != null) {
            try {
                String refreshToken = jsonObject.getString("refresh_token");
                saveRefreshToken(refreshToken);
            } catch (JSONException ex) {
                Logger.printException(() -> "setRefreshToken failed", ex);
            }
        }
    }

    public static void setAccessToken() {
        setAccessToken(null);
    }

    public static void setAccessToken(@Nullable Context mContext) {
        final long now = System.currentTimeMillis();
        if (lastTimeAccessTokenUpdated > 0L &&
                now - lastTimeAccessTokenUpdated < accessTokenExpiration) {
            return;
        }
        JSONObject jsonObject =
                fetch(RequestType.GET_ACCESS_TOKEN, getRefreshToken());

        if (jsonObject != null) {
            try {
                String accessToken = jsonObject.getString("access_token");

                lastTimeAccessTokenUpdated = System.currentTimeMillis();
                int expiresIn = jsonObject.getInt("expires_in");
                // Set the access token lifetime to 1770 seconds (00:29:30) instead of 3599 seconds (00:59:59)
                accessTokenExpiration = (expiresIn - 60) / 2 * 1000L;
                authorization = "Bearer " + accessToken;

                if (mContext != null) {
                    Utils.runOnMainThread(() -> {
                        String dialogTitle =
                                str("revanced_spoof_streaming_data_sign_in_android_vr_success_dialog_title");
                        String dialogMessage =
                                str("revanced_spoof_streaming_data_sign_in_android_vr_success_dialog_message");

                        if (BaseThemeUtils.isSupportModernDialog) {
                            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                                    mContext,
                                    // Title.
                                    dialogTitle,
                                    // Message.
                                    dialogMessage,
                                    // No EditText.
                                    null,
                                    // OK button text.
                                    null,
                                    // OK button action.
                                    () -> {
                                    },
                                    // Cancel button action.
                                    null,
                                    // Neutral button text.
                                    null,
                                    // Neutral button action.
                                    YouTubeVRAuthPatch::clearAll,
                                    // Dismiss dialog when onNeutralClick.
                                    true
                            );
                            dialogPair.first.show();
                        } else {
                            AlertDialog.Builder builder = Utils.getDialogBuilder(mContext);
                            builder.setTitle(dialogTitle);
                            builder.setMessage(dialogMessage);
                            builder.setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss());
                            builder.show();
                        }
                    });
                }
            } catch (JSONException ex) {
                Logger.printException(() -> "setAccessToken failed", ex);
            }
        } else if (mContext != null) {
            Utils.showToastShort(str("revanced_spoof_streaming_data_sign_in_android_vr_toast_failed"));
            Utils.showToastShort(str("revanced_spoof_streaming_data_sign_in_android_vr_toast_failed_suggestion"));
        }
    }

    /**
     * Using OAuth tokens allows access to all information in a user's Google account,
     * including changes to their Google account information, without leaving any logs or notifications.
     * Importing / exporting 2.0 tokens must be strictly prohibited.
     */
    private static final SharedPrefCategory preferences = new SharedPrefCategory("youtube_vr");
    private static final String deviceCodeKey = "youtube_vr_auth_device_code";
    private static final String refreshTokenKey = "youtube_vr_auth_refresh_token";
    private static String authorization = "";

    public static String getAuthorization() {
        return authorization;
    }

    private static String getDeviceCode() {
        return preferences.getString(deviceCodeKey, "");
    }

    private static String getRefreshToken() {
        return preferences.getString(refreshTokenKey, "");
    }

    public static boolean isAuthorizationAvailable() {
        return !authorization.isEmpty();
    }

    public static boolean isDeviceCodeAvailable() {
        return !getDeviceCode().isEmpty();
    }

    private static boolean isRefreshTokenAvailable() {
        return !getRefreshToken().isEmpty();
    }

    private static void saveDeviceCode(String value) {
        preferences.saveString(deviceCodeKey, value);
    }

    private static void saveRefreshToken(String value) {
        preferences.saveString(refreshTokenKey, value);
    }
}
