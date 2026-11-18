package com.chs.channel.integration.core;

public final class CoreCallException extends RuntimeException {
    private final boolean timeout;

    public CoreCallException(boolean timeout, Throwable cause) {
        super(timeout ? "Core HTTP timeout" : "Core HTTP or contract failure", cause);
        this.timeout = timeout;
    }

    public boolean isTimeout() {
        return timeout;
    }
}
