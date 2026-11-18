package com.chs.channel;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
@AutoConfigureMockMvc
class ChannelRequestApiTest {
    private static final String PATH = "/api/v1/channel-requests";

    @Autowired
    MockMvc mvc;

    @Test
    void createsAndReadsRequestThroughSqlite() throws Exception {
        String location = mvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subject":" 채널 POC 요청 "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith(PATH + "/")))
                .andExpect(jsonPath("$.subject").value("채널 POC 요청"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        String id = location.substring(PATH.length() + 1);
        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.subject").value("채널 POC 요청"))
                .andExpect(jsonPath("$.createdAt").isString());
    }

    @Test
    void rejectsBlankSubject() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        mvc.perform(get(PATH + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}
