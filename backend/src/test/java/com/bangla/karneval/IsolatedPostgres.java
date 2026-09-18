package com.bangla.karneval;

import java.sql.DriverManager;
import java.util.UUID;

final class IsolatedPostgres {
    static String newSchemaUrl() {
        String schema="test_"+UUID.randomUUID().toString().replace("-", "");
        String url="jdbc:postgresql://127.0.0.1:15432/membership_test";
        try (var c=DriverManager.getConnection(url,"postgres","membership_test_only"); var s=c.createStatement()) {
            s.execute("CREATE SCHEMA "+schema);
        } catch (Exception e) { throw new IllegalStateException(e); }
        return url+"?currentSchema="+schema;
    }
}
