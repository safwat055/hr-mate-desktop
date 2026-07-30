package com.safwat.hr.report.payroll;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PayrollReport {
    String code();

    String displayName();

    String category() default "yearly_payroll";

    String mainReport();
}