package com.pos.pos_backend.security;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_SCHEMA = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantSchema(String schema) {
        CURRENT_SCHEMA.set(schema);
        log.debug("TenantContext SET → schema={} thread={}",
                schema, Thread.currentThread().getName());
    }

    public static String getTenantSchema() {
        String schema = CURRENT_SCHEMA.get();
        log.debug("TenantContext GET → schema={} thread={}",
                schema, Thread.currentThread().getName());
        return schema;
    }

    public static void clear() {
        CURRENT_SCHEMA.remove();
    }
}