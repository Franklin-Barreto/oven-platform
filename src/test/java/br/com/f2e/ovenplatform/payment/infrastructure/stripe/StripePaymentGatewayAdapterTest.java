package br.com.f2e.ovenplatform.payment.infrastructure.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationFailedException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationOutcomeUnknownException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionSpec;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.service.CheckoutService;
import com.stripe.service.V1Services;
import com.stripe.service.checkout.SessionService;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripePaymentGatewayAdapterTest {

  private static final String CURRENCY = "BRL";
  private static final UUID ATTEMPT_ID = UUID.randomUUID();
  private static final Instant EXPIRES_AT = Instant.parse("2026-08-04T18:00:00Z");
  private static final URI REDIRECT_URL = URI.create("https://checkout.test/session");
  private static final URI SUCCESS_URL = URI.create("https://success.test/session");
  private static final URI CANCEL_URL = URI.create("https://cancel.test/session");
  private static final BigDecimal AMOUNT = new BigDecimal("120.00");

  @Mock private StripeClient stripeClient;
  @Mock private V1Services v1Services;
  @Mock private CheckoutService checkoutService;
  @Mock private SessionService sessionService;

  private StripePaymentGatewayAdapter gateway;

  @BeforeEach
  void setUp() {
    var properties = new StripeProperties("sk_test_fake", SUCCESS_URL, CANCEL_URL);

    gateway = new StripePaymentGatewayAdapter(stripeClient, properties);

    when(stripeClient.v1()).thenReturn(v1Services);
    when(v1Services.checkout()).thenReturn(checkoutService);
    when(checkoutService.sessions()).thenReturn(sessionService);
  }

  @Test
  void shouldCreateCheckoutSessionWithExpectedStripeParameters() throws StripeException {

    var session = new Session();
    session.setId("cs_test_123");
    session.setUrl(REDIRECT_URL.toString());
    session.setExpiresAt(EXPIRES_AT.getEpochSecond());

    when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
        .thenReturn(session);

    var sessionCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
    var optionsCaptor = ArgumentCaptor.forClass(RequestOptions.class);

    var spec = new CheckoutSessionSpec(ATTEMPT_ID, AMOUNT, CURRENCY);
    var checkoutSession = gateway.createCheckoutSession(spec);

    verify(sessionService).create(sessionCaptor.capture(), optionsCaptor.capture());

    var capturedSession = sessionCaptor.getValue();
    var capturedOptions = optionsCaptor.getValue();
    var lineItem = capturedSession.getLineItems().getFirst();
    var priceData = lineItem.getPriceData();

    assertThat(checkoutSession.providerReference()).isEqualTo(session.getId());
    assertThat(checkoutSession.checkoutUrl()).hasToString(session.getUrl());
    assertThat(checkoutSession.expiresAt())
        .isEqualTo(Instant.ofEpochSecond(session.getExpiresAt()));

    assertThat(capturedSession.getSuccessUrl()).isEqualTo(SUCCESS_URL.toString());
    assertThat(capturedSession.getCancelUrl()).isEqualTo(CANCEL_URL.toString());
    assertThat(capturedSession.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
    assertThat(capturedSession.getLineItems()).hasSize(1);
    assertThat(lineItem.getQuantity()).isEqualTo(1L);
    assertThat(priceData.getUnitAmount()).isEqualTo(12_000L);
    assertThat(priceData.getCurrency()).isEqualTo(CURRENCY.toLowerCase(Locale.ROOT));
    assertThat(priceData.getProductData().getName()).isEqualTo("Pagamento do pedido");
    assertThat(capturedOptions.getIdempotencyKey()).isEqualTo(ATTEMPT_ID.toString());
  }

  @ParameterizedTest
  @MethodSource("stripeExceptionMappings")
  void shouldTranslateStripeExceptionsAccordingToOutcomeCertainty(
      StripeException stripeException, Class<? extends RuntimeException> expected, String message)
      throws StripeException {

    when(sessionService.create(any(SessionCreateParams.class), any(RequestOptions.class)))
        .thenThrow(stripeException);

    var spec = new CheckoutSessionSpec(ATTEMPT_ID, AMOUNT, CURRENCY);
    assertThatThrownBy(() -> gateway.createCheckoutSession(spec))
        .isInstanceOf(expected)
        .hasMessage(message)
        .hasCause(stripeException);
  }

  private static Stream<Arguments> stripeExceptionMappings() {
    return Stream.of(
        Arguments.of(
            new ApiException("Internal server error", "req_test_123", "api_error", 500, null),
            CheckoutSessionCreationOutcomeUnknownException.class,
            "Checkout session creation outcome is unknown for attempt " + ATTEMPT_ID),
        Arguments.of(
            new ApiConnectionException("Error trying to connect"),
            CheckoutSessionCreationOutcomeUnknownException.class,
            "Checkout session creation outcome is unknown for attempt " + ATTEMPT_ID),
        Arguments.of(
            new InvalidRequestException(
                "Invalid checkout parameters",
                "currency",
                "req_test_123",
                "parameter_invalid",
                400,
                null),
            CheckoutSessionCreationFailedException.class,
            "Checkout session creation failed for attempt " + ATTEMPT_ID));
  }
}
