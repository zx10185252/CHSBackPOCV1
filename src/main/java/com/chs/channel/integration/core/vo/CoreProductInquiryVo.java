package com.chs.channel.integration.core.vo;

import java.util.UUID;

/** HTTP wire types for the sample EIMS product inquiry contract. */
public final class CoreProductInquiryVo {
    private CoreProductInquiryVo() {}

    public record Header(String messageId, String version, UUID requestId) {}
    public record Request(Header header, RequestBody body) {}
    public record RequestBody(String productCode) {}
    public record Response(Header header, String resultCode, ResponseBody body) {}
    public record ResponseBody(String productCode, String productName) {}
}
