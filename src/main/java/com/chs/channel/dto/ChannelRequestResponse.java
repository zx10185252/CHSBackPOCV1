package com.chs.channel.dto;

import java.time.Instant;
import java.util.UUID;

public record ChannelRequestResponse(
        UUID id,
        String subject,
        String status,
        Instant createdAt
) {}
