package com.pos.pos_backend.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            DataSource dataSource) {

        SchemaMultiTenantConnectionProvider connectionProvider =
                new SchemaMultiTenantConnectionProvider(dataSource);

        TenantSchemaResolver tenantResolver = new TenantSchemaResolver();

        return hibernateProperties -> {
            hibernateProperties.put(
                    AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,
                    connectionProvider);
            hibernateProperties.put(
                    AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                    tenantResolver);
        };
    }
}