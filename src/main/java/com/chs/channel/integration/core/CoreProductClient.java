package com.chs.channel.integration.core;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.chs.channel.integration.core.vo.CoreProductInquiryVo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CoreProductClient {
    private static final String PATH = "/core/v1/product-inquiries";
    private static final String MESSAGE_ID = "CHS-CORE-001";
    private static final String VERSION = "1.0";

    private final RestClient http;

    public CoreProductClient(
            RestClient.Builder builder,
            @Value("${core.http.base-url}") String baseUrl,
            @Value("${core.http.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${core.http.read-timeout-ms}") int readTimeoutMs) {
        var client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
        var factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.http = builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    public Optional<Product> findProduct(String productCode) {
        var header = new CoreProductInquiryVo.Header(MESSAGE_ID, VERSION, UUID.randomUUID());
        var request = new CoreProductInquiryVo.Request(header, new CoreProductInquiryVo.RequestBody(productCode));

        CoreProductInquiryVo.Response response = exchange(request);
        // Check the echoed header before interpreting the result as this request's response.
        if (response == null || !header.equals(response.header())) {
            throw new CoreCallException(false, null);
        }
        if ("1001".equals(response.resultCode())) {
            return Optional.empty();
        }
        var body = response.body();
        if (!"0000".equals(response.resultCode()) || body == null
                || !productCode.equals(body.productCode()) || body.productName() == null
                || body.productName().isBlank()) {
            throw new CoreCallException(false, null);
        }
        return Optional.of(new Product(body.productCode(), body.productName()));
    }

    private CoreProductInquiryVo.Response exchange(CoreProductInquiryVo.Request request) {
        try {
            return http.post().uri(PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> status.value() != 200, (ignored, response) -> {
                        throw new CoreCallException(false, null);
                    })
                    .body(CoreProductInquiryVo.Response.class);
        } catch (ResourceAccessException ex) {
            Throwable cause = ex.getMostSpecificCause();
            throw new CoreCallException(cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException, ex);
        } catch (RestClientException ex) {
            throw new CoreCallException(false, ex);
        }
    }

    // Map the wire VO to a result the application can consume without the EIMS envelope.
    public record Product(String productCode, String productName) {}
}
