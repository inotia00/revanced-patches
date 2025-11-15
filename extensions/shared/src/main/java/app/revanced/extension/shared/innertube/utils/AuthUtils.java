package app.revanced.extension.shared.innertube.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class AuthUtils {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    // Used to identify brand accounts.
    private static final String PAGE_ID_HEADER = "X-Goog-PageId";
    private static final String VISITOR_ID_HEADER = "X-Goog-Visitor-Id";
    private static final Map<String, String> REQUEST_HEADER = new LinkedHashMap<>(3);
    @NonNull
    private static String authorization = "";
    @NonNull
    private static String pageId = "";
    @NonNull
    private static String visitorId = "";
    private static boolean incognitoStatus = false;

    /**
     * Injection point.
     */
    public static void setRequestHeaders(String url, Map<String, String> requestHeaders) {
        if (requestHeaders == null) {
            return;
        }
        String newlyLoadedAuthorization = requestHeaders.get(AUTHORIZATION_HEADER);
        String newlyLoadedVisitorId = requestHeaders.get(VISITOR_ID_HEADER);
        boolean authorizationNeedsUpdating =
                StringUtils.isNotEmpty(newlyLoadedAuthorization) && !authorization.equals(newlyLoadedAuthorization);
        boolean visitorIdNeedsUpdating =
                StringUtils.isNotEmpty(newlyLoadedVisitorId) && !visitorId.equals(newlyLoadedVisitorId);

        if (authorizationNeedsUpdating && visitorIdNeedsUpdating) {
            REQUEST_HEADER.remove(AUTHORIZATION_HEADER);
            REQUEST_HEADER.remove(VISITOR_ID_HEADER);
            REQUEST_HEADER.put(AUTHORIZATION_HEADER, newlyLoadedAuthorization);
            REQUEST_HEADER.put(VISITOR_ID_HEADER, newlyLoadedVisitorId);
            authorization = newlyLoadedAuthorization;
            visitorId = newlyLoadedVisitorId;
            Logger.printDebug(() -> "new Authorization loaded: " + newlyLoadedAuthorization);
            Logger.printDebug(() -> "new VisitorId loaded: " + newlyLoadedVisitorId);
        }
    }

    /**
     * Injection point.
     */
    public static void setAccountIdentity(@Nullable String newlyLoadedPageId,
                                          boolean newlyLoadedIncognitoStatus) {
        if (StringUtils.isEmpty(newlyLoadedPageId)) {
            REQUEST_HEADER.remove(PAGE_ID_HEADER);
            pageId = "";
        } else if (!pageId.equals(newlyLoadedPageId)) {
            REQUEST_HEADER.remove(PAGE_ID_HEADER);
            REQUEST_HEADER.put(PAGE_ID_HEADER, newlyLoadedPageId);
            pageId = newlyLoadedPageId;
            Logger.printDebug(() -> "new PageId loaded: " + newlyLoadedPageId);
        }
        incognitoStatus = newlyLoadedIncognitoStatus;
    }

    public static Map<String, String> getRequestHeader() {
        return REQUEST_HEADER;
    }

    public static boolean isNotLoggedIn() {
        return authorization.isEmpty() || (pageId.isEmpty() && incognitoStatus);
    }

    public static Map<String, String> parseCookies(String cookie) {
        Map<String, String> cookies = new HashMap<>();
        String[] cookiePairs = cookie.split("; ");
        for (String pair : cookiePairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                cookies.put(keyValue[0], keyValue[1]);
            }
        }
        return cookies;
    }

    public static String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            Logger.printException(() -> "sha1 failed", ex);
        }
        return "";
    }

    // Converts Header string Cookies to 'Authorization' header.
    // This function is required to support login Cookies on 'Mobile Web' clients.
    public static String getAuthorizationHeader(Map<String, String> cookies) {
        String ytURL = "https://www.youtube.com";
        try {
            String sapisid = cookies.get("SAPISID");

            if (sapisid == null) {
                sapisid = cookies.get("__Secure-3PAPISID");
                if (sapisid == null) {
                    return "";
                }
            }

            long currentTimestamp = System.currentTimeMillis() / 1000;
            String initialData = currentTimestamp + " " + sapisid + " " + ytURL;
            String hash = sha1(initialData);

            return "SAPISIDHASH " + currentTimestamp + "_" + hash;
        } catch (Exception ex) {
            Logger.printException(() -> "getAuthorizationHeader failed", ex);
        }
        return "";
    }

}
