package br.com.f2e.ovenplatform.orders.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.orders.domain.Order;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreationMetricsAspectTest {

  @Mock private OrderCreationMetrics orderCreationMetrics;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private Timer.Sample sample;

  @InjectMocks private OrderCreationMetricsAspect metricsAspect;

  @Test
  void shouldRecordSuccessAndReturnOrderCreationResult() throws Throwable {
    var expectedResult = mock(Order.class);

    when(orderCreationMetrics.start()).thenReturn(sample);
    when(joinPoint.proceed()).thenReturn(expectedResult);

    var result = metricsAspect.recordOrderCreation(joinPoint);

    assertThat(result).isSameAs(expectedResult);

    verify(orderCreationMetrics).recordSuccess(sample);
    verify(orderCreationMetrics, never()).recordFailure(any());
  }

  @Test
  void shouldRecordFailureAndRethrowOriginalException() throws Throwable {

    var expectedException = mock(Throwable.class);

    when(orderCreationMetrics.start()).thenReturn(sample);
    when(joinPoint.proceed()).thenThrow(expectedException);

    assertThatThrownBy(() -> metricsAspect.recordOrderCreation(joinPoint))
        .isSameAs(expectedException);
  }
}
