package com.chs.channel.dbio;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelRequestDbio {
    private final JdbcClient jdbc;

    public ChannelRequestDbio(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(ChannelRequestRow row) {
        jdbc.sql("""
                INSERT INTO channel_request (id, subject, status, created_at)
                VALUES (:id, :subject, :status, :createdAt)
                """)
                .param("id", row.id().toString())
                .param("subject", row.subject())
                .param("status", row.status())
                // SQLite stores the UTC timestamp as ISO-8601 text.
                .param("createdAt", row.createdAt().toString())
                .update();
    }

    public Optional<ChannelRequestRow> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, subject, status, created_at
                FROM channel_request
                WHERE id = :id
                """)
                .param("id", id.toString())
                .query((result, rowNumber) -> new ChannelRequestRow(
                        UUID.fromString(result.getString("id")),
                        result.getString("subject"),
                        result.getString("status"),
                        Instant.parse(result.getString("created_at"))))
                .optional();
    }
}
