package br.com.f2e.ovenplatform.shared.infrastructure.tracing;

import br.com.f2e.ovenplatform.shared.infrastructure.web.ApiHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class RequestTracingFilter extends OncePerRequestFilter {

  private final TraceContext traceContext;

  public RequestTracingFilter(TraceContext traceContext) {
    this.traceContext = traceContext;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
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
      traceContext.clear();
      MDC.remove(TraceConstants.TRACE_ID_KEY);
    }
  }
}
