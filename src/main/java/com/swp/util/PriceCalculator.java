package com.swp.util;

import com.swp.model.PriceRule;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class PriceCalculator {

    public static BigDecimal calculateCurrentPrice(List<PriceRule> rules, Long fieldId, Integer fieldTypeId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate date = now.toLocalDate();
        LocalTime time = now.toLocalTime();
        DayOfWeek dow = now.getDayOfWeek();
        boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);

        PriceRule bestRule = null;

        for (PriceRule pr : rules) {
            // Filter by field_id and field_type_id
            if (pr.getFieldId() != null && fieldId != null && !pr.getFieldId().equals(fieldId)) continue;
            if (pr.getFieldTypeId() != null && fieldTypeId != null && !pr.getFieldTypeId().equals(fieldTypeId)) continue;

            // Check day
            boolean dayMatch = false;
            String ruleDow = pr.getDayOfWeek();
            
            if (ruleDow == null || "All".equalsIgnoreCase(ruleDow)) {
                dayMatch = true;
            } else if ("Weekday".equalsIgnoreCase(ruleDow) && !isWeekend) {
                dayMatch = true;
            } else if ("Weekend".equalsIgnoreCase(ruleDow) && isWeekend) {
                dayMatch = true;
            } else if ("SpecificDate".equalsIgnoreCase(ruleDow) && pr.getSpecificDate() != null && pr.getSpecificDate().equals(date)) {
                dayMatch = true;
            } else if (dow.name().equalsIgnoreCase(ruleDow)) {
                dayMatch = true;
            }

            if (!dayMatch) continue;

            // Check time
            boolean timeMatch = true;
            if (pr.getStartTime() != null && pr.getEndTime() != null) {
                // If it spans midnight e.g. 22:00 to 02:00
                if (pr.getStartTime().isAfter(pr.getEndTime())) {
                    if (time.isBefore(pr.getStartTime()) && time.isAfter(pr.getEndTime())) {
                        timeMatch = false;
                    }
                } else {
                    if (time.isBefore(pr.getStartTime()) || time.isAfter(pr.getEndTime())) {
                        timeMatch = false;
                    }
                }
            }

            if (!timeMatch) continue;

            if (bestRule == null) {
                bestRule = pr;
            } else {
                int currentPriority = pr.getPriority() != null ? pr.getPriority() : 0;
                int bestPriority = bestRule.getPriority() != null ? bestRule.getPriority() : 0;
                if (currentPriority > bestPriority || 
                   (currentPriority == bestPriority && pr.getPrice().compareTo(bestRule.getPrice()) > 0)) {
                    bestRule = pr;
                }
            }
        }

        return bestRule != null ? bestRule.getPrice() : null;
    }
}
