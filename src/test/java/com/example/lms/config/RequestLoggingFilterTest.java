package com.example.lms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Test
    void should_invokeFilterChain_when_requestProcessed() throws ServletException, IOException {
        // Given
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/classes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, chain);

        // Then
        verify(chain).doFilter(request, response);
    }

    @Test
    void should_setResponseStatus_when_chainCompletes() throws ServletException, IOException {
        // Given
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/register");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            response.setStatus(201);
            return null;
        }).when(chain).doFilter(request, response);

        // When
        filter.doFilter(request, response, chain);

        // Then
        assertThat(response.getStatus()).isEqualTo(201);
    }

    @Test
    void should_propagateException_when_chainThrows() throws ServletException, IOException {
        // Given
        RequestLoggingFilter filter = new RequestLoggingFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/error");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        // When / Then
        try {
            filter.doFilter(request, response, chain);
        } catch (ServletException e) {
            assertThat(e.getMessage()).isEqualTo("boom");
        }
    }
}
