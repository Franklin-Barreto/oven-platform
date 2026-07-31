package br.com.f2e.ovenplatform.catalog.application.optiongroup;

public record UpdateOptionGroupCommand(String name, int minimumSelections, int maximumSelections) {}
