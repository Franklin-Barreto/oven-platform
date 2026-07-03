package br.com.f2e.ovenplatform.kitchen.application;

import java.util.UUID;

public record CreateTicketItemCommand(UUID productId, String productName, int quantity) {}
