package app.revanced.extension.shared.patches.auth;

import static app.revanced.extension.shared.patches.AppCheckPatch.IS_YOUTUBE;
import static app.revanced.extension.shared.patches.auth.requests.AuthRequest.fetch;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;

import app.revanced.extension.shared.settings.preference.SharedPrefCategory;
import app.revanced.extension.shared.utils.Utils;

public class YouTubeAuthPatch {
    /**
     * The last time the access token was updated.
     */
    private static long lastTimeAccessTokenUpdated = 0L;

    /**
     * Access token expiration time.
     */
    private static long accessTokenExpiration = 1770 * 1000L;

    public static void checkAccessToken() {
        if (!IS_YOUTUBE && isEmailAvailable() && isRefreshTokenAvailable()) {
            setAccessToken();
        }
    }

    public static void clearAll() {
        saveEmail("");
        saveRefreshToken("");
        lastTimeAccessTokenUpdated = 0L;
        accessTokenExpiration = 1770 * 1000L;
        authorization = "";

        Utils.showToastShort(str("revanced_spoof_streaming_data_sign_in_android_no_sdk_toast_reset"));
    }

    public static void setAccessToken() {
        final long now = System.currentTimeMillis();
        if (lastTimeAccessTokenUpdated > 0L &&
                now - lastTimeAccessTokenUpdated < accessTokenExpiration) {
            return;
        }
        Pair<String, String> result = fetch(getEmail(), getRefreshToken(), true);
        if (result != null) {
            String accessToken = result.second;
            if (StringUtils.isNotEmpty(accessToken)) {
                authorization = "Bearer " + accessToken;
                lastTimeAccessTokenUpdated = System.currentTimeMillis();
            }
        }
    }

    public static void setAccessToken(Activity mActivity, String email, String oauthToken) {
        Pair<String, String> result = fetch(email, oauthToken, false);
        if (result != null) {
            String refreshToken = result.first;
            String accessToken = result.second;
            if (StringUtils.isNotEmpty(refreshToken) && StringUtils.isNotEmpty(accessToken)) {
                saveEmail(email);
                saveRefreshToken(refreshToken);
                authorization = "Bearer " + accessToken;
                lastTimeAccessTokenUpdated = System.currentTimeMillis();

                AlertDialog.Builder builder = Utils.getDialogBuilder(mActivity);

                String dialogTitle =
                        str("revanced_spoof_streaming_data_sign_in_android_no_sdk_success_dialog_title");
                String dialogMessage =
                        str("revanced_spoof_streaming_data_sign_in_android_no_sdk_success_dialog_message");

                builder.setTitle(dialogTitle);
                builder.setMessage(dialogMessage);
                builder.setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> mActivity.finish());
                builder.show();
                return;
            }
        }
        mActivity.finish();
    }

    /**
     * Using OAuth tokens allows access to all information in a user's Google account,
     * including changes to their Google account information, without leaving any logs or notifications.
     * Importing / exporting 2.0 tokens must be strictly prohibited.
     */
    private static final SharedPrefCategory preferences = new SharedPrefCategory("youtube_no_sdk");
    private static final String emailKey = "youtube_no_sdk_auth_email";
    private static final String refreshTokenKey = "youtube_no_sdk_auth_refresh_token";
    private static String authorization = "";

    public static String getAuthorization() {
        return authorization;
    }

    private static String getEmail() {
        return preferences.getString(emailKey, "");
    }

    private static String getRefreshToken() {
        return preferences.getString(refreshTokenKey, "");
    }

    public static boolean isAuthorizationAvailable() {
        return !authorization.isEmpty();
    }

    public static boolean isEmailAvailable() {
        return !getEmail().isEmpty();
    }

    private static boolean isRefreshTokenAvailable() {
        return !getRefreshToken().isEmpty();
    }

    private static void saveEmail(String value) {
        preferences.saveString(emailKey, value);
    }

    private static void saveRefreshToken(String value) {
        preferences.saveString(refreshTokenKey, value);
    }
}
