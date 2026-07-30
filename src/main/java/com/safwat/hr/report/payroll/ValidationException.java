package com.safwat.hr.report.payroll;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}