package com.danasea.backend.shared.core.infrastructure.persistence.generators;

import java.lang.reflect.Member;
import java.util.EnumSet;
import java.util.UUID;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

public class AssignedOrRandomUuidGenerator implements BeforeExecutionGenerator {

    public AssignedOrRandomUuidGenerator() {
    }

    public AssignedOrRandomUuidGenerator(AssignedOrRandomUuid config, Member member, GeneratorCreationContext context) {
    }

    public AssignedOrRandomUuidGenerator(AssignedOrRandomUuid config, Member member) {
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        if (currentValue != null) {
            return currentValue;
        }
        return UUID.randomUUID();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of(EventType.INSERT);
    }

    @Override
    public boolean allowAssignedIdentifiers() {
        return true;
    }
}
