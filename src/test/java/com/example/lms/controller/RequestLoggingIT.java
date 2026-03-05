package com.example.lms.controller;

import com.example.lms.AbstractIntegrationTest;
import com.example.lms.config.RequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestLoggingIT extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void requestLoggingFilter_isBeanInContext() {
        assertThat(applicationContext.getBean(RequestLoggingFilter.class)).isNotNull();
    }

    @Test
    void getPublicEndpoint_withLoggingFilter_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
