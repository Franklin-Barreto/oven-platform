package br.com.f2e.ovenplatform.orders.infrastructure.observability;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(0)
public class OrderCreationMetricsAspect {

  private final OrderCreationMetrics orderCreationMetrics;

  public OrderCreationMetricsAspect(OrderCreationMetrics orderCreationMetrics) {
    this.orderCreationMetrics = orderCreationMetrics;
  }

  @Around("execution(* br.com.f2e.ovenplatform.orders.application.OrderService.createOrder(..))")
  public Object recordOrderCreation(ProceedingJoinPoint joinPoint) throws Throwable {
    var sample = orderCreationMetrics.start();

    try {
      var result = joinPoint.proceed();
      orderCreationMetrics.recordSuccess(sample);
      return result;
    } catch (Throwable throwable) {
      orderCreationMetrics.recordFailure(sample);
      throw throwable;
    }
  }
}
