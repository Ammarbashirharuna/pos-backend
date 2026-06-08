package com.pos.pos_backend.config;

import com.pos.pos_backend.security.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

@Slf4j
public class TenantSchemaResolver
        implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_SCHEMA = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        String schema = TenantContext.getTenantSchema();
        String resolved = (schema != null && !schema.isBlank())
                ? schema : DEFAULT_SCHEMA;
        log.debug("Resolving tenant schema → {}", resolved);
        return resolved;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}