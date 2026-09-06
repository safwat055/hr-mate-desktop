package com.safwat.hr.controller.scale.scale.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraResultScale {
    private String column_1;
    private String value_1;
    private String column_2;
    private String value_2;
}
