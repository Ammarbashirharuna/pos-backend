package com.pos.pos_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String schema) throws SQLException {
        log.debug("Switching Hibernate connection to schema: {}", schema);
        Connection connection = getAnyConnection();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO \"" + schema + "\", public");
        } catch (SQLException e) {
            connection.close();
            throw new SQLException("Failed to set schema: " + schema, e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String schema, Connection connection)
            throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO public");
        } catch (SQLException e) {
            log.warn("Failed to reset schema to public: {}", e.getMessage());
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) return (T) this;
        throw new UnsupportedOperationException("Cannot unwrap to " + unwrapType);
    }
}