package com.safwat.hr.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * حافز ترقية — تاريخ + رقم القرار
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PromotionIncentiveRecord {
    private LocalDate date;
    private String decisionNumber;
}