package br.com.f2e.ovenplatform.payment.infrastructure.stripe;

import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationFailedException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionCreationOutcomeUnknownException;
import br.com.f2e.ovenplatform.payment.application.gateway.CheckoutSessionSpec;
import br.com.f2e.ovenplatform.payment.application.gateway.CreatedCheckoutSession;
import br.com.f2e.ovenplatform.payment.application.gateway.PaymentGateway;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.ApiException;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Locale;

@Component
public class StripePaymentGatewayAdapter implements PaymentGateway {

  private final StripeClient stripeClient;
  private final StripeProperties properties;

  public StripePaymentGatewayAdapter(StripeClient stripeClient, StripeProperties properties) {
    this.stripeClient = stripeClient;
    this.properties = properties;
  }

  @Override
  public CreatedCheckoutSession createCheckoutSession(CheckoutSessionSpec spec) {
    var price = toMinorUnit(spec.amount());
    var params =
        SessionCreateParams.builder()
            .setSuccessUrl(properties.successUrl().toString())
            .setCancelUrl(properties.cancelUrl().toString())
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(spec.currency().toLowerCase(Locale.ROOT))
                            .setUnitAmount(price)
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Pagamento do pedido")
                                    .build())
                            .build())
                    .setQuantity(1L)
                    .build())
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .putMetadata("oven_attempt_id", spec.attemptId().toString())
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .build();

    var options = RequestOptions.builder().setIdempotencyKey(spec.attemptId().toString()).build();

    try {
      var session = stripeClient.v1().checkout().sessions().create(params, options);

      return new CreatedCheckoutSession(
          session.getId(),
          URI.create(session.getUrl()),
          Instant.ofEpochSecond(session.getExpiresAt()));

    } catch (ApiConnectionException | ApiException exception) {
      throw new CheckoutSessionCreationOutcomeUnknownException(spec.attemptId(), exception);

    } catch (StripeException exception) {
      throw new CheckoutSessionCreationFailedException(spec.attemptId(), exception);
    }
  }

  private long toMinorUnit(BigDecimal amount) {
    return amount.movePointRight(2).longValueExact();
  }
}
