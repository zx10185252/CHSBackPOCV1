package com.chs.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChannelRequest(
        @NotBlank @Size(max = 120) String subject
) {}
