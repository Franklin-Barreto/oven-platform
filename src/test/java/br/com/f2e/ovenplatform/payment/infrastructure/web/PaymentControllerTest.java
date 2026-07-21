package br.com.f2e.ovenplatform.payment.infrastructure.web;

import static br.com.f2e.ovenplatform.identity.infrastructure.security.test.SecurityTestRequestPostProcessors.authenticatedTenantUser;
import static br.com.f2e.ovenplatform.shared.infrastructure.web.test.ApiErrorResponseMatchers.expectValidationErrors;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.payment.application.OrderPaymentResponse;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@ActiveProfiles("test")
@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/payments";
  private static final String LOOK_UP_URL = BASE_URL + "/orders/lookup";
  private static final Instant PAID_AT = Instant.parse("2026-05-12T20:18:00Z");

  @MockitoBean private PaymentService paymentService;

  @Test
  void shouldReturnPaymentsByOrderIds() throws Exception {
    var orderId = UUID.randomUUID();
    var request = new OrderPaymentsLookupRequest(List.of(orderId));
    when(paymentService.findByTenantIdAndOrderIdIn(TENANT_ID, request.orderIds()))
        .thenReturn(
            List.of(
                new OrderPaymentResponse(
                    orderId, PaymentMethod.CASH, PaymentStatus.PENDING, PAID_AT)));

    mockMvc
        .perform(
            post(LOOK_UP_URL)
                .with(paymentReadUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].orderId").value(orderId.toString()))
        .andExpect(jsonPath("$[0].method").value(PaymentMethod.CASH.name()))
        .andExpect(jsonPath("$[0].status").value(PaymentStatus.PENDING.name()))
        .andExpect(jsonPath("$[0].paidAt").value(PAID_AT.toString()))
        .andReturn();

    verify(paymentService).findByTenantIdAndOrderIdIn(TENANT_ID, request.orderIds());
  }

  @Test
  void shouldReturnEmptyListWhenNoPaymentsMatchRequestedOrderIds() throws Exception {

    when(paymentService.findByTenantIdAndOrderIdIn(any(), anyList()))
        .thenReturn(Collections.emptyList());

    var request = new OrderPaymentsLookupRequest(List.of(UUID.randomUUID()));
    mockMvc
        .perform(
            post(LOOK_UP_URL)
                .with(paymentReadUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    verify(paymentService).findByTenantIdAndOrderIdIn(any(), anyList());
  }

  @Test
  void shouldReturnBadRequestWhenOrderIdsIsEmpty() throws Exception {

    mockMvc
        .perform(
            post(LOOK_UP_URL)
                .with(paymentReadUser())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(new OrderPaymentsLookupRequest(Collections.emptyList()))))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            expectValidationErrors(
                HttpStatus.BAD_REQUEST,
                LOOK_UP_URL,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ApiErrorCodes.VALIDATION_ERROR,
                "At least one order id must be provided",
                "orderIds",
                HttpStatus.BAD_REQUEST.value()));

    verifyNoInteractions(paymentService);
  }

  @Test
  void shouldReturnForbiddenWhenPaymentReadPermissionIsMissing() throws Exception {
    var request = new OrderPaymentsLookupRequest(List.of(UUID.randomUUID()));

    mockMvc
        .perform(
            post(LOOK_UP_URL)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.ORDER_READ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isForbidden());

    verifyNoInteractions(paymentService);
  }

  @Test
  void shouldReturnUnauthorizedWhenAuthenticationIsMissing() throws Exception {
    var request = new OrderPaymentsLookupRequest(List.of(UUID.randomUUID()));

    mockMvc
        .perform(
            post(LOOK_UP_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request)))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(paymentService);
  }

  private static RequestPostProcessor paymentReadUser() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.PAYMENT_READ);
  }
}
