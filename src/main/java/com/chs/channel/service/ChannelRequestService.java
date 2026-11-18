package com.chs.channel.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.chs.channel.dbio.ChannelRequestDbio;
import com.chs.channel.dbio.ChannelRequestRow;
import com.chs.channel.dto.ChannelRequestResponse;
import com.chs.channel.dto.CreateChannelRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelRequestService {
    private final ChannelRequestDbio dbio;

    public ChannelRequestService(ChannelRequestDbio dbio) {
        this.dbio = dbio;
    }

    @Transactional
    public ChannelRequestResponse create(CreateChannelRequest request) {
        // RECEIVED means the local channel request was saved.
        var row = new ChannelRequestRow(UUID.randomUUID(), request.subject().strip(), "RECEIVED", Instant.now());
        dbio.insert(row);
        return toResponse(row);
    }

    public Optional<ChannelRequestResponse> findById(UUID id) {
        return dbio.findById(id).map(ChannelRequestService::toResponse);
    }

    private static ChannelRequestResponse toResponse(ChannelRequestRow row) {
        return new ChannelRequestResponse(row.id(), row.subject(), row.status(), row.createdAt());
    }
}
