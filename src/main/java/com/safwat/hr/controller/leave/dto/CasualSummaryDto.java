package com.safwat.hr.controller.leave.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CasualSummaryDto {
    private int leaveYear;
    private int entitled;
    private int used;
    private int remaining;
}