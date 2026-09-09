package com.liang.medical.waitlist;

import java.util.EnumSet;

public enum WaitlistStatus {
    WAITING,
    OFFERED,
    ACCEPTED,
    CANCELLED,
    EXPIRED;

    public static boolean canTransition(WaitlistStatus from, WaitlistStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return switch (from) {
            case WAITING -> EnumSet.of(OFFERED, CANCELLED).contains(to);
            case OFFERED -> EnumSet.of(ACCEPTED, EXPIRED, CANCELLED).contains(to);
            case ACCEPTED, CANCELLED, EXPIRED -> false;
        };
    }
}
