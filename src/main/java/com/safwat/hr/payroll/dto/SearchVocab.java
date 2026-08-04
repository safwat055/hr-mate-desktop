package com.safwat.hr.payroll.dto;

/**
 *
 * @param nationalID
 * @param payID
 * @param name
 * @param management
 */
public record SearchVocab(
        String nationalID,
        String payID,
        String name,
        String management
) {
}