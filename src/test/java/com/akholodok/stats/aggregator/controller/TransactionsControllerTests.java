package com.akholodok.stats.aggregator.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.akholodok.stats.aggregator.conf.AppConfiguration;
import com.akholodok.stats.aggregator.model.Stats;
import com.akholodok.stats.aggregator.service.TimeSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TransactionsControllerTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TimeSource timeSource;

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test(timeout = 5_000)
    public void testGetStatsEmpty() throws Exception {
        this.mockMvc.perform(get(TransactionsController.PATH)
            .accept(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE)))
            .andExpect(status().isNoContent());
    }

    @Test(timeout = 5_000)
    public void testAddStatsAndGetStats() throws Exception {

        double amount = 10.0;
        AddTransactionRequest request = new AddTransactionRequest(amount, timeSource.now().toEpochMilli());

        this.mockMvc.perform(
            post(TransactionsController.PATH)
                .content(mapper.writeValueAsBytes(request))
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isOk());

        Stats expectedResponse = new Stats(1, amount, amount, amount);
        this.mockMvc.perform(
            get(TransactionsController.PATH)
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(content().json(mapper.writeValueAsString(expectedResponse)));
    }

    @Test(timeout = 5_000)
    public void testAddStatsInFuture() throws Exception {

        double amount = 10.0;
        AddTransactionRequest request = new AddTransactionRequest(amount, timeSource.now().plusSeconds(10).toEpochMilli());

        this.mockMvc.perform(
            post(TransactionsController.PATH)
                .content(mapper.writeValueAsBytes(request))
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isNoContent());
    }

    @Test(timeout = 5_000)
    public void testAddStatsMissingAmount() throws Exception {
        AddTransactionRequest request = new AddTransactionRequest(null, timeSource.now().plusSeconds(10).toEpochMilli());
        this.mockMvc.perform(
            post(TransactionsController.PATH)
                .content(mapper.writeValueAsBytes(request))
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isBadRequest());
        //.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }

    @Test(timeout = 5_000)
    public void testAddStatsMissingTimestamp() throws Exception {
        AddTransactionRequest request = new AddTransactionRequest(10.0, null);
        this.mockMvc.perform(
            post(TransactionsController.PATH)
                .content(mapper.writeValueAsBytes(request))
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(status().isBadRequest());
        //.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
    }
}
