package com.chs.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductInquiryRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_-]{1,20}") String productCode
) {}
