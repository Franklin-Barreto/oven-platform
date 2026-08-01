package br.com.f2e.ovenplatform.catalog.application.option;

import java.math.BigDecimal;

public record CreateOptionCommand(String name, BigDecimal priceAdjustment) {}
