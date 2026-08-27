package com.slotengine.api.config;

import com.slotengine.api.ledger.OperatingMode;
import com.slotengine.api.ledger.WalletProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "slot")
public class SlotProperties {

    /**
     * {@link OperatingMode#LIVE} forces math, studio import, client seed and top-up off.
     */
    private OperatingMode mode = OperatingMode.SIMULATION;
    private boolean mathEnabled = true;
    private boolean studioEnabled = true;
    private boolean allowClientSeed = true;
    private boolean allowTopUp = true;
    private long defaultCredits = 100_000;
    private String gamesDir = "./games";
    private String currency = "CREDITS";
    private Wallet wallet = new Wallet();

    public OperatingMode getMode() {
        return mode;
    }

    public void setMode(OperatingMode mode) {
        this.mode = mode == null ? OperatingMode.SIMULATION : mode;
    }

    public boolean isMathEnabled() {
        return mathEnabled;
    }

    public void setMathEnabled(boolean mathEnabled) {
        this.mathEnabled = mathEnabled;
    }

    public boolean isStudioEnabled() {
        return studioEnabled;
    }

    public void setStudioEnabled(boolean studioEnabled) {
        this.studioEnabled = studioEnabled;
    }

    public boolean isAllowClientSeed() {
        return allowClientSeed;
    }

    public void setAllowClientSeed(boolean allowClientSeed) {
        this.allowClientSeed = allowClientSeed;
    }

    public boolean isAllowTopUp() {
        return allowTopUp;
    }

    public void setAllowTopUp(boolean allowTopUp) {
        this.allowTopUp = allowTopUp;
    }

    public long getDefaultCredits() {
        return defaultCredits;
    }

    public void setDefaultCredits(long defaultCredits) {
        this.defaultCredits = defaultCredits;
    }

    public String getGamesDir() {
        return gamesDir;
    }

    public void setGamesDir(String gamesDir) {
        this.gamesDir = gamesDir;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet == null ? new Wallet() : wallet;
    }

    public boolean isLive() {
        return mode == OperatingMode.LIVE;
    }

    public boolean mathApiEnabled() {
        return mathEnabled && !isLive();
    }

    public boolean studioApiEnabled() {
        return studioEnabled && !isLive();
    }

    public boolean clientSeedAllowed() {
        return allowClientSeed && !isLive();
    }

    public boolean topUpAllowed() {
        return allowTopUp && !isLive();
    }

    public static class Wallet {
        private WalletProvider provider = WalletProvider.SIMULATED;
        private HttpWallet http = new HttpWallet();

        public WalletProvider getProvider() {
            return provider;
        }

        public void setProvider(WalletProvider provider) {
            this.provider = provider == null ? WalletProvider.SIMULATED : provider;
        }

        public HttpWallet getHttp() {
            return http;
        }

        public void setHttp(HttpWallet http) {
            this.http = http == null ? new HttpWallet() : http;
        }
    }

    public static class HttpWallet {
        private String baseUrl = "";
        private String debitPath = "/wallet/debit";
        private String creditPath = "/wallet/credit";
        private String rollbackPath = "/wallet/rollback";
        private String balancePath = "/wallet/balance/{playerId}";
        private String authHeader = "Authorization";
        private String authToken = "";
        private int timeoutMs = 3000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl == null ? "" : baseUrl;
        }

        public String getDebitPath() {
            return debitPath;
        }

        public void setDebitPath(String debitPath) {
            this.debitPath = debitPath;
        }

        public String getCreditPath() {
            return creditPath;
        }

        public void setCreditPath(String creditPath) {
            this.creditPath = creditPath;
        }

        public String getRollbackPath() {
            return rollbackPath;
        }

        public void setRollbackPath(String rollbackPath) {
            this.rollbackPath = rollbackPath;
        }

        public String getBalancePath() {
            return balancePath;
        }

        public void setBalancePath(String balancePath) {
            this.balancePath = balancePath;
        }

        public String getAuthHeader() {
            return authHeader;
        }

        public void setAuthHeader(String authHeader) {
            this.authHeader = authHeader;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken == null ? "" : authToken;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
