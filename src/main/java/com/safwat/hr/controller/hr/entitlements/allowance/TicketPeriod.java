package com.safwat.hr.controller.hr.entitlements.allowance;

import java.time.LocalDate;

/**
 * فترة تذاكر واحدة — نسخة الفرونت.
 * نصف التذكرة بيحسب فرد، فالحد: fullTickets + halfTickets ≤ 3.
 */
public record TicketPeriod(
        LocalDate from,
        LocalDate to,        // null = مفتوحة
        int fullTickets,
        int halfTickets
) {
    public boolean covers(LocalDate targetDate) {
        if (targetDate.isBefore(from)) return false;
        if (to == null) return true;
        return !targetDate.isAfter(to);
    }
}