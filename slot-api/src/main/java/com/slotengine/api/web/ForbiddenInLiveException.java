package com.slotengine.api.web;

public class ForbiddenInLiveException extends RuntimeException {

    private final String code;

    public ForbiddenInLiveException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ForbiddenInLiveException math() {
        return new ForbiddenInLiveException("MATH_DISABLED", "Math simulation API is disabled in this process");
    }

    public static ForbiddenInLiveException studio() {
        return new ForbiddenInLiveException("STUDIO_DISABLED", "Game import is disabled in LIVE mode");
    }

    public static ForbiddenInLiveException seed() {
        return new ForbiddenInLiveException("CLIENT_SEED_FORBIDDEN", "Client-supplied RNG seed is forbidden in LIVE mode");
    }

    public static ForbiddenInLiveException topUp() {
        return new ForbiddenInLiveException("TOP_UP_FORBIDDEN", "Credit top-up is forbidden in LIVE mode");
    }
}
