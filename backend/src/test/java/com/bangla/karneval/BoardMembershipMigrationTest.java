package com.bangla.karneval;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION",matches="true")
class BoardMembershipMigrationTest {
    private void migrate(JdbcTemplate jdbc,String sql) {
        var tx=new org.springframework.transaction.support.TransactionTemplate(
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        tx.executeWithoutResult(status->jdbc.execute(sql));
    }
    @Test void reservesSlotsRenumbersConflictsPreservesHistoryAndDoesNotReuseIds() throws Exception {
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(IsolatedPostgres.newSchemaUrl(),"postgres","membership_test_only"));
        jdbc.execute("CREATE SEQUENCE membership_number_seq; CREATE TABLE members(id BIGINT PRIMARY KEY,membership_id VARCHAR(40) UNIQUE, membership_expires_on DATE)");
        String sql=new ClassPathResource("db/migrations/20260924_board_membership_ids.sql").getContentAsString(StandardCharsets.UTF_8);
        migrate(jdbc,sql);
        assertEquals(7L,jdbc.queryForObject("SELECT nextval('membership_number_seq')",Long.class));
        jdbc.execute("ALTER TABLE members DROP CONSTRAINT members_board_ids_reserved");
        jdbc.execute("INSERT INTO members VALUES(1,'BKM-00001','2027-09-22',NULL),(2,'BKM-00006','2027-09-22',NULL),(3,'BKM-00020','2027-09-22',NULL)");
        migrate(jdbc,sql);
        assertEquals("BKM-00021",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=1",String.class));
        assertEquals("BKM-00022",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=2",String.class));
        assertEquals("BKM-00001",jdbc.queryForObject("SELECT previous_membership_id FROM members WHERE id=1",String.class));
        assertEquals("2027-09-22",jdbc.queryForObject("SELECT membership_expires_on::text FROM members WHERE id=1",String.class));
        assertEquals("BKM-00020",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=3",String.class));
        migrate(jdbc,sql);
        assertEquals("BKM-00021",jdbc.queryForObject("SELECT membership_id FROM members WHERE id=1",String.class));
        jdbc.execute("DELETE FROM members WHERE id=2");
        migrate(jdbc,sql);
        assertEquals(23L,jdbc.queryForObject("SELECT nextval('membership_number_seq')",Long.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.execute("INSERT INTO members(id,membership_id) VALUES(4,'BKM-00003')"));
    }
}
