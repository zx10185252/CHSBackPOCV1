package com.chs.channel.dbio;

import java.time.Instant;
import java.util.UUID;

public record ChannelRequestRow(UUID id, String subject, String status, Instant createdAt) {}
