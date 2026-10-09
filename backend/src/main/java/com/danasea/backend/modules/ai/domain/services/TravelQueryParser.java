package com.danasea.backend.modules.ai.domain.services;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.danasea.backend.modules.ai.domain.models.TravelContext;

public final class TravelQueryParser {
    private static final Pattern BUDGET = Pattern.compile("(?:duoi|toi da|ngan sach|under|budget|up to)\\s+(\\d{1,3}(?:[.,]\\d{3})+|\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|million|nghin|ngan|k|vnd|dong)?");
    private static final Map<String, String> ACTIVITIES = Map.of(
            "sup", "SUP", "kayak", "kayak", "lan bien", "lặn", "snorkel", "lặn",
            "diving", "lặn", "cano", "cano", "du thuyen", "du thuyền");

    private TravelQueryParser() {}

    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
    }

    public static String activityKeyword(String text) {
        String normalized = normalize(text);
        List<String> found = new ArrayList<>();
        for (var entry : ACTIVITIES.entrySet()) {
            var matcher = Pattern.compile("(?<![a-z])" + Pattern.quote(entry.getKey()) + "(?![a-z])").matcher(normalized);
            while (matcher.find()) {
                int clause = Math.max(normalized.lastIndexOf(',', matcher.start()), normalized.lastIndexOf('.', matcher.start())) + 1;
                String prefix = normalized.substring(clause, matcher.start());
                if (!prefix.matches(".*\\b(khong|tranh|not|except|no)\\b.*")) found.add(entry.getValue());
            }
        }
        List<String> unique = found.stream().distinct().toList();
        return unique.size() == 1 ? unique.get(0) : null;
    }

    public static List<String> excludedActivities(String text) {
        String normalized = normalize(text);
        List<String> excluded = new ArrayList<>();
        for (var entry : ACTIVITIES.entrySet()) {
            var matcher = Pattern.compile("(?<![a-z])" + Pattern.quote(entry.getKey()) + "(?![a-z])").matcher(normalized);
            while (matcher.find()) {
                int clause = Math.max(normalized.lastIndexOf(',', matcher.start()), normalized.lastIndexOf('.', matcher.start())) + 1;
                if (normalized.substring(clause, matcher.start()).matches(".*\\b(khong|tranh|not|except|no)\\b.*")) excluded.add(normalize(entry.getValue()));
            }
        }
        return excluded.stream().distinct().toList();
    }

    public static Integer partySize(String text) {
        var matcher = Pattern.compile("(?<!\\d)(\\d{1,2})\\s*(nguoi|khach|people|pax)\\b").matcher(normalize(text));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    public static LocalDate date(String text) {
        String normalized = normalize(text);
        LocalDate today = LocalDate.now(TravelContext.ZONE);
        if (normalized.contains("ngay mai") || normalized.contains("tomorrow")) return today.plusDays(1);
        if (normalized.contains("hom nay") || normalized.contains("today")) return today;
        return null;
    }

    public static boolean perPersonBudget(String text) {
        return normalize(text).matches(".*(moi nguoi|/nguoi|per person|each person).*?");
    }

    public static BigDecimal budget(String text) {
        var matcher = BUDGET.matcher(normalize(text));
        if (!matcher.find()) return null;
        String unit = matcher.group(2);
        if (unit == null) return null;
        String number = matcher.group(1);
        boolean currency = unit.equals("vnd") || unit.equals("dong");
        if (currency && number.matches("\\d{1,3}(?:[.,]\\d{3})+")) number = number.replace(".", "").replace(",", "");
        BigDecimal amount = new BigDecimal(number.replace(',', '.'));
        if (unit.equals("trieu") || unit.equals("tr") || unit.equals("million")) amount = amount.multiply(new BigDecimal("1000000"));
        else if (unit.equals("nghin") || unit.equals("ngan") || unit.equals("k")) amount = amount.multiply(new BigDecimal("1000"));
        else if (number.matches("\\d{1,3}(?:[.,]\\d{3})+")) amount = new BigDecimal(number.replace(".", "").replace(",", ""));
        return amount;
    }

    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dlat = Math.toRadians(lat2 - lat1);
        double dlon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dlon / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}
