package br.com.f2e.ovenplatform.payment.infrastructure.web;

import br.com.f2e.ovenplatform.identity.application.api.security.TenantPermission;
import br.com.f2e.ovenplatform.identity.domain.TenantMembershipRole;
import br.com.f2e.ovenplatform.payment.application.OrderPaymentResult;
import br.com.f2e.ovenplatform.payment.application.PaymentService;
import br.com.f2e.ovenplatform.payment.application.checkout.PaymentCheckoutSessionResult;
import br.com.f2e.ovenplatform.payment.application.checkout.PaymentCheckoutSessionService;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationFailedException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationOutcomeUnknownException;
import br.com.f2e.ovenplatform.payment.application.checkout.UnsupportedCheckoutPaymentMethodException;
import br.com.f2e.ovenplatform.payment.domain.ExternalPaymentAttemptStatus;
import br.com.f2e.ovenplatform.payment.domain.PaymentMethod;
import br.com.f2e.ovenplatform.payment.domain.PaymentProcessingMode;
import br.com.f2e.ovenplatform.payment.domain.PaymentStatus;
import br.com.f2e.ovenplatform.payment.domain.exception.ExternalPaymentAttemptNotAllowedException;
import br.com.f2e.ovenplatform.payment.domain.exception.InvalidExternalPaymentAttemptStatusTransitionException;
import br.com.f2e.ovenplatform.shared.infrastructure.web.exception.ApiErrorCodes;
import br.com.f2e.ovenplatform.shared.infrastructure.web.test.AbstractControllerTest;
import br.com.f2e.ovenplatform.shared.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

@ActiveProfiles("test")
@WebMvcTest(controllers = PaymentController.class)
class PaymentControllerTest extends AbstractControllerTest {

  private static final String BASE_URL = "/payments";
  private static final String LOOK_UP_URL = BASE_URL + "/orders/lookup";
  private static final Instant PAID_AT = Instant.parse("2026-05-12T20:18:00Z");
  private static final Instant EXPIRES_AT = Instant.parse("2026-05-12T20:18:00Z");
  private static final UUID ORDER_ID = UUID.randomUUID();
  private static final String CHECKOUT_URI = BASE_URL + "/orders/" + ORDER_ID + "/checkout-sessions";
  private static final URI CHECKOUT_URL = URI.create("https://checkout.test/session");
  private static final UUID ATTEMPT_ID = UUID.fromString("c6210129-f1d5-4942-8d0a-b144e518aecc");

  @MockitoBean private PaymentService paymentService;
  @MockitoBean private PaymentCheckoutSessionService paymentCheckoutSessionService;

  @Test
  void shouldReturnPaymentsByOrderIds() throws Exception {
    var orderId = UUID.randomUUID();
    var request = new OrderPaymentsLookupRequest(List.of(orderId));
    when(paymentService.findByTenantIdAndOrderIdIn(TENANT_ID, request.orderIds()))
        .thenReturn(
            List.of(
                new OrderPaymentResult(
                    orderId,
                    PaymentMethod.CASH,
                    PaymentStatus.PENDING,
                    PaymentProcessingMode.MANUAL,
                    PAID_AT)));

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
        .andExpect(jsonPath("$[0].processingMode").value(PaymentProcessingMode.MANUAL.name()))
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

  @Test
  void shouldReturnSuccessfullyWhenCreatingSessionCheckout() throws Exception {

    when(paymentCheckoutSessionService.createOrReuseCheckoutSession(TENANT_ID, ORDER_ID))
        .thenReturn(new PaymentCheckoutSessionResult(ATTEMPT_ID, CHECKOUT_URL, EXPIRES_AT));

    mockMvc
        .perform(
            post(CHECKOUT_URI)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.PAYMENT_MANAGE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attemptId").value(ATTEMPT_ID.toString()))
        .andExpect(jsonPath("$.checkoutUrl").value(CHECKOUT_URL.toString()))
        .andExpect(jsonPath("$.expiresAt").value(EXPIRES_AT.toString()));

    verify(paymentCheckoutSessionService).createOrReuseCheckoutSession(TENANT_ID, ORDER_ID);
  }

  @ParameterizedTest
  @MethodSource("expectedExceptions")
  void shouldHandleKnownExceptions(
      Exception exception, HttpStatus httpStatus, String apiErrorCode, String errorMessage)
      throws Exception {

    when(paymentCheckoutSessionService.createOrReuseCheckoutSession(TENANT_ID, ORDER_ID))
        .thenThrow(exception);

    mockMvc
        .perform(
            post(CHECKOUT_URI)
                .with(
                    authenticatedTenantUser(
                        TENANT_ID, TenantMembershipRole.MANAGER, TenantPermission.PAYMENT_MANAGE)))
        .andExpect(status().is(httpStatus.value()))
        .andExpectAll(
            expectValidationErrors(
                httpStatus,
                CHECKOUT_URI,
                httpStatus.getReasonPhrase(),
                apiErrorCode,
                errorMessage,
                null,
                httpStatus.value()));
  }

  @Test
  void shouldReturnForbiddenWhenPaymentManagePermissionIsMissing() throws Exception {
    mockMvc.perform(post(CHECKOUT_URI).with(paymentReadUser())).andExpect(status().isForbidden());

    verifyNoInteractions(paymentCheckoutSessionService);
  }

  @Test
  void shouldReturnUnauthorizedWhenCheckoutAuthenticationIsMissing() throws Exception {
    mockMvc.perform(post(CHECKOUT_URI)).andExpect(status().isUnauthorized());

    verifyNoInteractions(paymentCheckoutSessionService);
  }

  private static Stream<Arguments> expectedExceptions() {
    return Stream.of(
        Arguments.of(
            new CheckoutSessionCreationOutcomeUnknownException(ATTEMPT_ID, null),
            HttpStatus.SERVICE_UNAVAILABLE,
            ApiErrorCodes.PAYMENT_GATEWAY_OUTCOME_UNKNOWN,
            "Checkout session creation outcome is unknown for attempt " + ATTEMPT_ID),
        Arguments.of(
            new CheckoutSessionCreationFailedException(ATTEMPT_ID, null),
            HttpStatus.BAD_GATEWAY,
            ApiErrorCodes.PAYMENT_GATEWAY_FAILURE,
            "Checkout session creation failed for attempt " + ATTEMPT_ID),
        Arguments.of(
            new InvalidExternalPaymentAttemptStatusTransitionException(
                ExternalPaymentAttemptStatus.CREATED, ExternalPaymentAttemptStatus.SUCCEEDED),
            HttpStatus.CONFLICT,
            ApiErrorCodes.INVALID_EXTERNAL_PAYMENT_ATTEMPT_STATUS_TRANSITION,
            "Cannot transition external payment attempt from %s to %s."
                .formatted(
                    ExternalPaymentAttemptStatus.CREATED, ExternalPaymentAttemptStatus.SUCCEEDED)),
        Arguments.of(
            new ExternalPaymentAttemptNotAllowedException(
                "Only pending payments can create external payment attempts."),
            HttpStatus.CONFLICT,
            ApiErrorCodes.EXTERNAL_PAYMENT_ATTEMPT_NOT_ALLOWED,
            "Only pending payments can create external payment attempts."),
        Arguments.of(
            new UnsupportedCheckoutPaymentMethodException(PaymentMethod.PIX),
            HttpStatus.CONFLICT,
            ApiErrorCodes.EXTERNAL_PAYMENT_METHOD_NOT_ALLOWED,
            "Checkout sessions are only supported for CARD payments, but received PIX."));
  }

  private static RequestPostProcessor paymentReadUser() {
    return authenticatedTenantUser(
        TENANT_ID, TenantMembershipRole.ATTENDANT, TenantPermission.PAYMENT_READ);
  }
}
