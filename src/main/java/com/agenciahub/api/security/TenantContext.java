package com.agenciahub.api.security;

import java.util.UUID;

/**
 * Holds the current tenant (agency) ID for the duration of a request.
 * Uses ThreadLocal to ensure thread-safety in a servlet container.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void set(UUID agencyId) {
        currentTenant.set(agencyId);
    }

    public static UUID get() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
