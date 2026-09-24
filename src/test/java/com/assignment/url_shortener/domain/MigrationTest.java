package com.assignment.url_shortener.domain;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {
    @Test void upgradesExistingV1RowsWithoutDataLoss() throws Exception {
        String url = "jdbc:h2:mem:migration-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var insert = connection.prepareStatement("INSERT INTO short_links(id,code,original_url,created_at) VALUES(?,?,?,CURRENT_TIMESTAMP)")) {
            insert.setObject(1, UUID.randomUUID()); insert.setString(2, "legacy");
            insert.setString(3, "https://example.com/legacy"); insert.executeUpdate();
        }
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM short_links WHERE code = 'legacy'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString("original_url")).isEqualTo("https://example.com/legacy");
            assertThat(rows.getBoolean("enabled")).isTrue();
            assertThat(rows.getLong("click_count")).isZero();
            assertThat(rows.getTimestamp("expires_at")).isNull();
        }
    }
}
