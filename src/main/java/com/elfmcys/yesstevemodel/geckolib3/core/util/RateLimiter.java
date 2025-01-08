package com.elfmcys.yesstevemodel.geckolib3.core.util;

public class RateLimiter {
    private final float interval;
    private float aggregate;
    private float lastRequestTime;

    public RateLimiter(int limitPerSec) {
        interval = 1f / limitPerSec;
        aggregate = interval;
        lastRequestTime = 0;
    }

    public boolean request(float time) {
        aggregate += time - lastRequestTime;
        lastRequestTime = time;

        if (aggregate < interval) {
            return false;
        }

        this.aggregate = this.aggregate % this.interval;
        return true;
    }

    public float getInterval() {
        return interval;
    }
}
