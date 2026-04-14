package com.example.my_blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrator.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public DatabaseSchemaMigrator(
            JdbcTemplate jdbcTemplate,
            @Value("${app.db.auto-migrate:true}") boolean enabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Database schema auto-migration is disabled.");
            return;
        }

        log.info("Checking and applying database schema patches...");
        jdbcTemplate.execute("ALTER TABLE post_meta ADD COLUMN IF NOT EXISTS is_published BOOLEAN");
        jdbcTemplate.execute("UPDATE post_meta SET is_published = TRUE WHERE is_published IS NULL");
        jdbcTemplate.execute("ALTER TABLE post_meta ALTER COLUMN is_published SET DEFAULT TRUE");
        jdbcTemplate.execute("ALTER TABLE post_meta ALTER COLUMN is_published SET NOT NULL");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_post_meta_is_published_created_at ON post_meta (is_published, created_at DESC)"
        );
        log.info("Database schema patches applied.");
    }
}
