package com.chs.channel.controller;

import com.chs.channel.dto.ProductInquiryRequest;
import com.chs.channel.dto.ProductInquiryResponse;
import com.chs.channel.integration.core.CoreCallException;
import com.chs.channel.service.ProductInquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/core/product-inquiries")
public class ProductInquiryController {
    private final ProductInquiryService service;

    public ProductInquiryController(ProductInquiryService service) {
        this.service = service;
    }

    @PostMapping
    public ProductInquiryResponse inquire(@Valid @RequestBody ProductInquiryRequest request) {
        return service.inquire(request).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    @ExceptionHandler(CoreCallException.class)
    ProblemDetail coreFailure(CoreCallException error) {
        return error.isTimeout()
                ? ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, "Core system did not respond in time")
                : ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Core system returned an invalid response or could not be reached");
    }
}
