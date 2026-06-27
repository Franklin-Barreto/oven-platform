package br.com.f2e.ovenplatform.orders.application;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderableProduct(UUID productId, String productName, BigDecimal unitPrice) {}
