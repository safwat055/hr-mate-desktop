package com.safwat.hr.scale;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * تغيير المجموعة النوعية — تاريخ + اسم المجموعة
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupChangeRecord {
    private LocalDate date;
    private String groupName;
}