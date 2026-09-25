package com.bangla.karneval.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Transactional, versioned PostgreSQL migration after legacy settings initialization. */
@Service
public class ProgrammeFoundationMigration {
    private final JdbcTemplate jdbc;
    public ProgrammeFoundationMigration(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Transactional(rollbackFor = Exception.class)
    public void migrate() throws IOException {
        jdbc.execute("SELECT pg_advisory_xact_lock(20260918, 1)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS application_schema_migrations (version VARCHAR(100) PRIMARY KEY, applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        apply("20260918_programme_foundation");
        apply("20260918_programme_management");
        apply("20260923_membership_ids");
        apply("20260923_membership_expiry");
        apply("20260924_club_homepage");
        apply("20260924_organisation_story");
        apply("20260924_board_membership_ids");
    }
    private void apply(String version) throws IOException {
        if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM application_schema_migrations WHERE version=?)",Boolean.class,version))) return;
        String sql=new ClassPathResource("db/migrations/"+version+".sql").getContentAsString(StandardCharsets.UTF_8);
        // PostgreSQL handles the dollar-quoted functions; do not split on semicolons.
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement=connection.createStatement()) { statement.execute(sql); }
            return null;
        });
        jdbc.update("INSERT INTO application_schema_migrations(version) VALUES (?)",version);
    }
}
