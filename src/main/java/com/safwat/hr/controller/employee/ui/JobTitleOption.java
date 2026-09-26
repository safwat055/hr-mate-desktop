package com.safwat.hr.controller.employee.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * يمثّل JobTitleOptionDto اللي راجع من GET /api/job-titles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JobTitleOption(Long id, String code, String nameAr) {

    @Override
    public String toString() {
        return nameAr != null ? nameAr : "";
    }
}