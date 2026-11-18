package com.chs.channel.service;

import java.util.Optional;

import com.chs.channel.dto.ProductInquiryRequest;
import com.chs.channel.dto.ProductInquiryResponse;
import com.chs.channel.integration.core.CoreProductClient;
import org.springframework.stereotype.Service;

@Service
public class ProductInquiryService {
    private final CoreProductClient core;

    public ProductInquiryService(CoreProductClient core) {
        this.core = core;
    }

    public Optional<ProductInquiryResponse> inquire(ProductInquiryRequest request) {
        return core.findProduct(request.productCode())
                .map(product -> new ProductInquiryResponse(product.productCode(), product.productName()));
    }
}
