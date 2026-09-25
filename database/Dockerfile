FROM postgres:16-alpine
COPY init.sql /docker-entrypoint-initdb.d/1-init.sql
COPY seed.sql /docker-entrypoint-initdb.d/2-seed.sql
