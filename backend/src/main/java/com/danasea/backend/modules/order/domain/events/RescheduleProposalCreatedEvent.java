package com.danasea.backend.modules.order.domain.events;

import java.util.UUID;

public record RescheduleProposalCreatedEvent(UUID proposalId) {}
