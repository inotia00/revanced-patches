package app.revanced.extension.shared.patches.auth.requests;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import app.revanced.extension.shared.innertube.client.YouTubeVRClient.ClientType;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class AuthRoutes {
    // ANDROID_VR
    private static final String CLIENT_ID_ANDROID_VR =
            "652469312169-4lvs9bnhr9lpns9v451j5oivd81vjvu1.apps.googleusercontent.com";
    private static final String CLIENT_SECRET_ANDROID_VR =
            "3fTWrBJI5Uojm1TK7_iJCW5Z";

    // TVHTML5 (https://www.youtube.com/tv)
    private static final String CLIENT_ID_TVHTML5 =
            "861556708454-d6dlm3lh05idd8npek18k6be8ba3oc68.apps.googleusercontent.com";
    private static final String CLIENT_SECRET_TVHTML5 =
            "SboVhoG9s0rNafixCSGGKXAT";

    // TVHTML5_UNPLUGGED (https://www.youtube.com/tv/upg)
    private static final String CLIENT_ID_TVHTML5_UNPLUGGED =
            "963978912716-uqkvl110l4n1oetvfs5saqp1k7tpoo0d.apps.googleusercontent.com";
    private static final String CLIENT_SECRET_TVHTML5_UNPLUGGED =
            "0cDbu6a0rogKwUFCgWAqEoSC";

    private static final String OAUTH2_ACTIVATION_CODE_API_URL =
            "https://www.youtube.com/o/oauth2/device/code";
    private static final String OAUTH2_TOKEN_API_URL =
            "https://www.youtube.com/o/oauth2/token";

    private static final String OAUTH2_YOUTUBE_SCOPE =
            "https://www.googleapis.com/auth/youtube";
    private static final String GRANT_TYPE_DEFAULT =
            "http://oauth.net/grant_type/device/1.0";
    private static final String GRANT_TYPE_REFRESH =
            "refresh_token";

    private AuthRoutes() {
    }

    public enum RequestType {
        GET_ACTIVATION_CODE(OAUTH2_ACTIVATION_CODE_API_URL),
        GET_REFRESH_TOKEN(OAUTH2_TOKEN_API_URL),
        GET_ACCESS_TOKEN(OAUTH2_TOKEN_API_URL);

        private final String url;

        public String getUrl() {
            return url + "?prettyPrint=false";
        }

        RequestType(String url) {
            this.url = url;
        }
    }

    static byte[] createActivationCodeBody(ClientType clientType) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", CLIENT_ID_ANDROID_VR);
            jsonObject.put("scope", OAUTH2_YOUTUBE_SCOPE);
            jsonObject.put("device_id", UUID.randomUUID().toString());
            jsonObject.put("device_model", clientType.name());
        } catch (JSONException e) {
            Logger.printException(() -> "createActivationCodeBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] createRefreshTokenBody(String code) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", CLIENT_ID_ANDROID_VR);
            jsonObject.put("client_secret", CLIENT_SECRET_ANDROID_VR);
            jsonObject.put("code", code);
            jsonObject.put("grant_type", GRANT_TYPE_DEFAULT);
        } catch (JSONException e) {
            Logger.printException(() -> "createRefreshTokenBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }

    static byte[] createAccessTokenBody(String refreshToken) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("client_id", CLIENT_ID_ANDROID_VR);
            jsonObject.put("client_secret", CLIENT_SECRET_ANDROID_VR);
            jsonObject.put("refresh_token", refreshToken);
            jsonObject.put("grant_type", GRANT_TYPE_REFRESH);
        } catch (JSONException e) {
            Logger.printException(() -> "createAccessTokenBody failed", e);
        }

        return jsonObject.toString().getBytes(StandardCharsets.UTF_8);
    }
}