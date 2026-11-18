package com.chs.channel.controller;

import java.net.URI;
import java.util.UUID;

import com.chs.channel.dto.ChannelRequestResponse;
import com.chs.channel.dto.CreateChannelRequest;
import com.chs.channel.service.ChannelRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(ChannelRequestController.PATH)
public class ChannelRequestController {
    static final String PATH = "/api/v1/channel-requests";

    private final ChannelRequestService service;

    public ChannelRequestController(ChannelRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ChannelRequestResponse> create(@Valid @RequestBody CreateChannelRequest request) {
        ChannelRequestResponse response = service.create(request);
        return ResponseEntity.created(URI.create(PATH + "/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ChannelRequestResponse findById(@PathVariable UUID id) {
        return service.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Channel request not found"));
    }
}
