package com.safwat.hr.scale;

import java.time.LocalDate;

// في package الـ network/dto
public class ScaleResult {
    private Long employeeId;
    private String empName;
    private int law;
    private LocalDate startDate;
    private List<ScaleTimelinePoint> timeline;
    // getters & setters

    public Optional<ScaleTimelinePoint> lastPoint() {
        if (timeline == null || timeline.isEmpty()) return Optional.empty();
        return Optional.of(timeline.get(timeline.size() - 1));
    }
}