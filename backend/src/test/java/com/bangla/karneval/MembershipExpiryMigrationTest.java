package com.bangla.karneval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION", matches="true")
class MembershipExpiryMigrationTest {
    @Test void backfillsApprovalDatesAndPreservesExistingDatesAndReminders() throws Exception {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(IsolatedPostgres.newSchemaUrl(),"postgres","membership_test_only"));
        jdbc.execute("CREATE TABLE members(id BIGINT PRIMARY KEY,status TEXT,reviewed_at TIMESTAMP)");
        jdbc.execute("INSERT INTO members VALUES (1,'APPROVED','2024-02-29 15:00:00'),(2,'PENDING',NULL),(3,'APPROVED',NULL),(4,'REJECTED','2024-03-01 12:00:00')");
        String sql = new ClassPathResource("db/migrations/20260923_membership_expiry.sql").getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(sql);
        assertEquals("2024-02-29",jdbc.queryForObject("SELECT membership_starts_on::text FROM members WHERE id=1",String.class));
        assertEquals("2025-02-28",jdbc.queryForObject("SELECT membership_expires_on::text FROM members WHERE id=1",String.class));
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM members WHERE membership_expires_on IS NULL",Integer.class));
        jdbc.execute("UPDATE members SET membership_expires_on='2026-02-28',expiry_reminder_sent_at='2026-01-28 09:00:00' WHERE id=1");
        jdbc.execute(sql);
        assertEquals("2026-02-28",jdbc.queryForObject("SELECT membership_expires_on::text FROM members WHERE id=1",String.class));
        assertNotNull(jdbc.queryForObject("SELECT expiry_reminder_sent_at FROM members WHERE id=1",java.sql.Timestamp.class));
    }
}
