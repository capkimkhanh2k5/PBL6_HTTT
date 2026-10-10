package com.danasea.backend.modules.ai.application.ports;

import java.time.Duration;

public final class AiExecutionBudget implements AutoCloseable {
    private static final ThreadLocal<AiExecutionBudget> CURRENT = new ThreadLocal<>();
    private final AiExecutionBudget previous;
    private final long deadline;
    private int modelCalls;
    private final int maximumModelCalls;

    private AiExecutionBudget(Duration duration, int maximumModelCalls) {
        previous = CURRENT.get();
        deadline = System.nanoTime() + duration.toNanos();
        this.maximumModelCalls = maximumModelCalls;
        CURRENT.set(this);
    }
    public static AiExecutionBudget open(Duration duration, int maximumModelCalls) { return new AiExecutionBudget(duration, maximumModelCalls); }
    public static boolean hasTimeFor(Duration transportTimeout) {
        AiExecutionBudget budget = CURRENT.get();
        return budget == null || System.nanoTime() + transportTimeout.toNanos() < budget.deadline;
    }
    public static boolean permitDecision(Duration transportTimeout) {
        AiExecutionBudget budget = CURRENT.get();
        if (budget == null) return true;
        if (budget.modelCalls >= budget.maximumModelCalls || System.nanoTime() + transportTimeout.toNanos() >= budget.deadline) return false;
        budget.modelCalls++;
        return true;
    }
    @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
}
