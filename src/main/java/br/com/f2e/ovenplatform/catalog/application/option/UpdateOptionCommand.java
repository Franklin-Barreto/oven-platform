package br.com.f2e.ovenplatform.catalog.application.option;

import java.math.BigDecimal;

public record UpdateOptionCommand(String name, BigDecimal priceAdjustment) {}
