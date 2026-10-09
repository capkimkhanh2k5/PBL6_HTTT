package com.danasea.backend.modules.ai.domain.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public final class ParticipantGrouping {
    private ParticipantGrouping() { }

    public static List<Integer> propose(int participants, Integer packageCapacity, int quantity) {
        if (participants < 1 || participants > 50 || packageCapacity == null || packageCapacity < 1 || quantity < 1 || quantity > 50
                || (participants - 1) / packageCapacity + 1 != quantity)
            throw new IllegalArgumentException("The party requires an unsupported package grouping; choose another option");
        List<Integer> groups = new ArrayList<>();
        int remaining = participants;
        for (int index = 0; index < quantity; index++) {
            int size = Math.min(remaining, packageCapacity);
            groups.add(size); remaining -= size;
        }
        return List.copyOf(groups);
    }

    public static Map<Integer, Integer> counts(List<Integer> groups) {
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (Integer participants : groups) quantities.merge(participants, 1, Integer::sum);
        return quantities;
    }

    public static boolean valid(List<Integer> groups, int participants, Integer capacity, int quantity) {
        return groups != null && capacity != null && capacity > 0 && quantity > 0 && quantity <= 50
                && groups.size() == quantity && groups.stream().allMatch(size -> size != null && size > 0 && size <= capacity)
                && groups.stream().mapToInt(Integer::intValue).sum() == participants;
    }
}
