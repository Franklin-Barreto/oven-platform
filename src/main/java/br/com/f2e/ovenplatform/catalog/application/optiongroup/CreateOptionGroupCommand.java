package br.com.f2e.ovenplatform.catalog.application.optiongroup;

public record CreateOptionGroupCommand(String name, int minimumSelections, int maximumSelections) {}
