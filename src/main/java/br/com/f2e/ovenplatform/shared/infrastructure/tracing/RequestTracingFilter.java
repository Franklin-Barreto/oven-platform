package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RequestTracingFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestTracingFilter.class);
  private final TraceContext traceContext;

  public RequestTracingFilter(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var start = System.currentTimeMillis();
    var traceId =
        Optional.ofNullable(request.getHeader(ApiHeaders.TRACE_ID_HEADER))
            .filter(s -> !s.isBlank())
            .orElse(UUID.randomUUID().toString());
    try {
      traceContext.setTraceId(traceId);
      MDC.put(TraceConstants.TRACE_ID_KEY, traceId);
      response.setHeader(ApiHeaders.TRACE_ID_HEADER, traceId);
      filterChain.doFilter(request, response);
    } finally {
      var durationMs = System.currentTimeMillis() - start;
      LOGGER.info(
          "request completed method={} path={} status={} durationMs={}",
          request.getMethod(),
          request.getRequestURI(),
          response.getStatus(),
          durationMs);
      traceContext.clear();
      MDC.remove(TraceConstants.TRACE_ID_KEY);
    }
  }
}
