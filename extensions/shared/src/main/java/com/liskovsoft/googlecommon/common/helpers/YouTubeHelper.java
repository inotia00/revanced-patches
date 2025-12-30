package com.liskovsoft.googlecommon.common.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class YouTubeHelper {
    /**
     * Try to generate a {@code t} parameter, sent by mobile clients as a query of the player
     * request.
     *
     * <p>
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     * </p>
     *
     * @return a 12 characters string to try to reproduce the {@code} parameter
     */
    @NonNull
    public static String generateTParameter() {
        return RandomStringFromAlphabetGenerator.generate(12);
    }

    @NonNull
    public static String generateTParameter(@Nullable String tParameter) {
        return tParameter != null && tParameter.length() == 12
                ? tParameter
                : generateTParameter();
    }
}
