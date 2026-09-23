package com.bangla.karneval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION", matches="true")
class MembershipIdMigrationTest {
    @Test void backfillsApprovedMembersPreservesIdsAndNeverTruncatesOrReusesNumbers() throws Exception {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(IsolatedPostgres.newSchemaUrl(),"postgres","membership_test_only"));
        jdbc.execute("CREATE TABLE members(id BIGINT PRIMARY KEY,status TEXT,membership_id VARCHAR(40)); CREATE TABLE application_settings(id INT); CREATE TABLE registrations(id INT);");
        jdbc.execute("INSERT INTO members VALUES (1,'APPROVED','BKM-99999'),(2,'APPROVED',NULL),(3,'PENDING',NULL)");
        String sql = new ClassPathResource("db/migrations/20260923_membership_ids.sql").getContentAsString(StandardCharsets.UTF_8);
        jdbc.execute(sql);
        assertEquals("BKM-99999",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=1",String.class));
        assertEquals("BKM-100000",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=2",String.class));
        assertNull(jdbc.queryForObject("SELECT membership_id FROM members WHERE id=3",String.class));
        jdbc.execute("DELETE FROM members WHERE id=2");
        jdbc.execute(sql);
        assertEquals(100001L,jdbc.queryForObject("SELECT nextval('membership_number_seq')",Long.class));
        {
            var values = java.util.stream.IntStream.range(0,40).parallel()
                .mapToObj(i -> jdbc.queryForObject("SELECT nextval('membership_number_seq')",Long.class)).toList();
            assertEquals(40,new java.util.HashSet<>(values).size());
        }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbc.execute("INSERT INTO members VALUES (4,'APPROVED','BKM-99999')"));
    }
}
