package com.chs.channel;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
class CoreProductInquiryApiTest {
    private static final String API_PATH = "/api/v1/core/product-inquiries";
    private static final String CORE_PATH = "/core/v1/product-inquiries";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicReference<JsonNode> OBSERVED = new AtomicReference<>();
    private static final HttpServer CORE = startStub();

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void coreAddress(DynamicPropertyRegistry properties) {
        properties.add("core.http.base-url", () -> "http://127.0.0.1:" + CORE.getAddress().getPort());
    }

    @AfterAll
    static void stopStub() {
        CORE.stop(0);
    }

    @Test
    void sendsEimsEnvelopeAndReturnsOnlyChannelDto() throws Exception {
        mvc.perform(post(API_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"SAMPLE-DEPOSIT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("SAMPLE-DEPOSIT"))
                .andExpect(jsonPath("$.productName").value("POC deposit"))
                .andExpect(jsonPath("$.header").doesNotExist())
                .andExpect(jsonPath("$.resultCode").doesNotExist());

        JsonNode vo = OBSERVED.get();
        assertEquals("CHS-CORE-001", vo.at("/header/messageId").asString());
        assertEquals("1.0", vo.at("/header/version").asString());
        assertDoesNotThrow(() -> UUID.fromString(vo.at("/header/requestId").asString()));
        assertEquals("SAMPLE-DEPOSIT", vo.at("/body/productCode").asString());
    }

    @Test
    void mapsCoreBusinessResultToNotFound() throws Exception {
        mvc.perform(post(API_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"UNKNOWN"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsMismatchedCoreResponseHeader() throws Exception {
        mvc.perform(post(API_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productCode":"MISMATCH"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(CORE_PATH, exchange -> {
                try (exchange) {
                    if (!"POST".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }
                    JsonNode request = JSON.readTree(exchange.getRequestBody());
                    String code = request.at("/body/productCode").asString();
                    if ("SAMPLE-DEPOSIT".equals(code)) {
                        OBSERVED.set(request);
                    }
                    var response = JSON.createObjectNode();
                    response.set("header", request.path("header").deepCopy());
                    if ("UNKNOWN".equals(code)) {
                        response.put("resultCode", "1001");
                    } else {
                        response.put("resultCode", "0000");
                        response.putObject("body").put("productCode", code).put("productName", "POC deposit");
                    }
                    if ("MISMATCH".equals(code)) {
                        response.withObject("/header").put("requestId", UUID.randomUUID().toString());
                    }
                    byte[] bytes = JSON.writeValueAsBytes(response);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("Cannot start core HTTP test stub", error);
        }
    }
}
