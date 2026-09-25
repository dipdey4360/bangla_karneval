package com.bangla.karneval;

import com.bangla.karneval.service.ProgrammeFoundationMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="BK_MEMBERSHIP_INTEGRATION", matches="true")
class ProgrammeMigrationTest {
    @Test void failedMigrationRollsBackAllSchemaChangesAndVersionMarker() {
        var source=new DriverManagerDataSource(IsolatedPostgres.newSchemaUrl(),"postgres","membership_test_only");
        var jdbc=new JdbcTemplate(source);
        var migration=new ProgrammeFoundationMigration(jdbc);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        // Missing legacy tables deliberately causes a failure after new-table creation.
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(status->{
            try { migration.migrate(); } catch(Exception e) { throw new RuntimeException(e); }
        }));
        assertNull(jdbc.queryForObject("SELECT to_regclass('programmes')::text",String.class));
        assertNull(jdbc.queryForObject("SELECT to_regclass('application_schema_migrations')::text",String.class));
    }
    @Test void preservesHistoryAndSupportsSeparateEditionsAndLegacyWrites() {
        var source=new DriverManagerDataSource(IsolatedPostgres.newSchemaUrl(),"postgres","membership_test_only");
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("""
            CREATE TABLE event_config(id SERIAL PRIMARY KEY,event_year INT UNIQUE NOT NULL,event_date DATE,
            event_location VARCHAR(255),about_text TEXT,price_per_person NUMERIC(10,2),registration_enabled BOOLEAN,performer_enabled BOOLEAN);
            CREATE TABLE application_settings(id INT PRIMARY KEY,active_event_year INT,membership_single_fee NUMERIC(10,2));
            INSERT INTO application_settings VALUES(1,2026,25);
            INSERT INTO event_config(event_year,about_text,price_per_person,registration_enabled) VALUES(2025,'Historical story',12,true),(2026,'Original story',15,true);
            CREATE TABLE registrations(id SERIAL PRIMARY KEY,event_year INT,reference_code TEXT);
            INSERT INTO registrations(event_year,reference_code) VALUES(2025,'KEEP-1'),(2026,'KEEP-2'),(NULL,'UNKNOWN-YEAR');
            CREATE TABLE events(id SERIAL PRIMARY KEY,event_year INT);
            INSERT INTO events(event_year) VALUES(2025);
            CREATE TABLE performer_registrations(id SERIAL PRIMARY KEY,event_year INT);
            INSERT INTO performer_registrations(event_year) VALUES(2026);
            CREATE TABLE gallery_items(id SERIAL PRIMARY KEY,event_year INT,url TEXT);
            INSERT INTO gallery_items(event_year,url) VALUES(2024,'/uploads/keep.jpg');
            CREATE TABLE members(id INT PRIMARY KEY,name TEXT);
            INSERT INTO members VALUES(1,'Keep member');
            CREATE TABLE board_members(id INT PRIMARY KEY,name TEXT);
            INSERT INTO board_members VALUES(1,'Keep board');
            """);
        var migration=new ProgrammeFoundationMigration(jdbc);
        var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        Runnable migrate=()->tx.executeWithoutResult(status->{try {migration.migrate();} catch(Exception e) {throw new RuntimeException(e);}});
        migrate.run();
        assertEquals(6,jdbc.queryForObject("SELECT count(*) FROM programmes",Integer.class));
        assertEquals(7,jdbc.queryForObject("SELECT count(*) FROM event_editions",Integer.class));
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM event_editions WHERE slug LIKE 'club-%' AND event_date IS NULL AND registration_enabled=false",Integer.class));
        jdbc.update("UPDATE event_editions SET tagline='Keep custom wording' WHERE slug='club-eid-2026'");

        assertTrue(jdbc.queryForObject("SELECT story FROM organisation_profile",String.class).startsWith("Our journey began in a small pub in Cologne"));
        assertEquals("Bangla Karneval e.V.",jdbc.queryForObject("SELECT name FROM organisation_profile",String.class));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM registrations WHERE event_edition_id IS NOT NULL",Integer.class));
        assertEquals("UNKNOWN-YEAR",jdbc.queryForObject("SELECT reference_code FROM registrations WHERE event_edition_id IS NULL",String.class));
        assertEquals("/uploads/keep.jpg",jdbc.queryForObject("SELECT url FROM gallery_items WHERE event_edition_id IS NOT NULL",String.class));
        for(String table:new String[]{"event_config","events","performer_registrations","gallery_items"})
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM "+table+" WHERE event_edition_id IS NULL",Integer.class));
        assertEquals("Keep member",jdbc.queryForObject("SELECT name FROM members",String.class));
        assertEquals("Keep board",jdbc.queryForObject("SELECT name FROM board_members",String.class));
        jdbc.update("UPDATE organisation_profile SET story='Edited organisation story'");
        migrate.run();
        assertEquals(7,jdbc.queryForObject("SELECT count(*) FROM application_schema_migrations",Integer.class));
        assertEquals("Keep custom wording",jdbc.queryForObject("SELECT tagline FROM event_editions WHERE slug='club-eid-2026'",String.class));
        assertEquals(7,jdbc.queryForObject("SELECT count(*) FROM event_editions",Integer.class));

        assertEquals("Edited organisation story",jdbc.queryForObject("SELECT story FROM organisation_profile",String.class));
        jdbc.update("UPDATE event_config SET about_text='Event only',price_per_person=18 WHERE event_year=2026");
        assertEquals("Event only",jdbc.queryForObject("SELECT description FROM event_editions WHERE legacy_event_year=2026",String.class));
        assertEquals("Edited organisation story",jdbc.queryForObject("SELECT story FROM organisation_profile",String.class));
        jdbc.update("INSERT INTO event_config(event_year) VALUES(2027)");
        jdbc.update("INSERT INTO registrations(event_year,reference_code) VALUES(2027,'NEW')");
        jdbc.update("UPDATE application_settings SET active_event_year=2027,active_event_edition_id=(SELECT id FROM event_editions WHERE legacy_event_year=2027)");
        assertEquals(2027,jdbc.queryForObject("SELECT e.event_year FROM application_settings s JOIN event_editions e ON e.id=s.active_event_edition_id",Integer.class));
        assertEquals(25,jdbc.queryForObject("SELECT membership_single_fee FROM application_settings",Integer.class));
        jdbc.update("INSERT INTO event_editions(programme_id,slug,title,event_year,theme_key) SELECT id,'puja-2026','Puja 2026',2026,'puja' FROM programmes WHERE code='puja'");
        jdbc.update("INSERT INTO event_editions(programme_id,slug,title,event_year,theme_key) SELECT id,'bbq-spring-2026','Spring BBQ',2026,'bbq' FROM programmes WHERE code='bbq'");
        jdbc.update("INSERT INTO event_editions(programme_id,slug,title,event_year,theme_key) SELECT id,'bbq-summer-2026','Summer BBQ',2026,'bbq' FROM programmes WHERE code='bbq'");
        jdbc.update("INSERT INTO registrations(event_year,event_edition_id,reference_code) SELECT 2026,id,'PUJA' FROM event_editions WHERE slug='puja-2026'");
        assertEquals(8,jdbc.queryForObject("SELECT count(*) FROM event_editions WHERE event_year=2026",Integer.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO registrations(event_year,event_edition_id) SELECT 2025,id FROM event_editions WHERE slug='puja-2026'"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("DELETE FROM event_editions WHERE legacy_event_year=2025"));
        assertEquals(5,jdbc.queryForObject("SELECT count(*) FROM registrations",Integer.class));
    }
}
