# Add administrators with ordinary passwords

The application uses BCrypt for admin login. PostgreSQL can generate compatible
BCrypt hashes during insertion using `pgcrypto`; no application change is needed.

Run this once in the target database with an account permitted to create extensions:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

For one administrator, replace the example details and password before running:

```sql
INSERT INTO admin_users (name, email, password_hash, role)
VALUES (
    'New Admin Name',
    'new-admin@example.com',
    crypt('REPLACE_WITH_A_UNIQUE_PASSWORD', gen_salt('bf', 12)),
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING
RETURNING id, name, email, role;
```

For five new administrators, replace every placeholder below. Each administrator
should have a different password. The query hashes each password with a random salt
before storing it in `password_hash`.

```sql
INSERT INTO admin_users (name, email, password_hash, role)
SELECT name, email, crypt(password, gen_salt('bf', 12)), 'ADMIN'
FROM (VALUES
    ('Admin 1 Name', 'admin1@example.com', 'REPLACE_WITH_UNIQUE_PASSWORD_1'),
    ('Admin 2 Name', 'admin2@example.com', 'REPLACE_WITH_UNIQUE_PASSWORD_2'),
    ('Admin 3 Name', 'admin3@example.com', 'REPLACE_WITH_UNIQUE_PASSWORD_3'),
    ('Admin 4 Name', 'admin4@example.com', 'REPLACE_WITH_UNIQUE_PASSWORD_4'),
    ('Admin 5 Name', 'admin5@example.com', 'REPLACE_WITH_UNIQUE_PASSWORD_5')
) AS new_admins(name, email, password)
ON CONFLICT (email) DO NOTHING
RETURNING id, name, email, role;
```

Existing email addresses are skipped; this does not reset existing passwords.
Do not insert a plain password directly into `password_hash`: always wrap it in
`crypt(..., gen_salt('bf', 12))`. BCrypt supports passwords up to 72 bytes.
Use a trusted local or TLS database connection. Keep real passwords out of committed
scripts; SQL text may be retained in client history or database logs.

Reference: [PostgreSQL password hashing functions](https://www.postgresql.org/docs/16/pgcrypto.html#PGCRYPTO-PASSWORD-HASHING-FUNCS).
