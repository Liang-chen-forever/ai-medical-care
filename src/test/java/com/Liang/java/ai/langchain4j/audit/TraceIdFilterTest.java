package com.Liang.java.ai.langchain4j.audit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generatesTraceIdAndReturnsItInResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/appointments");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            assertThat(req.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)).isInstanceOf(String.class);
        });

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).matches("[0-9a-f-]{36}");
    }

    @Test
    void preservesAValidIncomingTraceId() throws Exception {
        String traceId = "11111111-1111-1111-1111-111111111111";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/appointments/me");
        request.addHeader(TraceIdFilter.HEADER_NAME, traceId);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (FilterChain) (req, res) -> { });

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isEqualTo(traceId);
    }
}
