package com.danasea.backend.modules.order.application.dtos;

public record AcceptSubOrderWaiverCommand(Boolean accepted, Integer version, String language) {}
