package com.safwat.hr.controller.template;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
public class TemplateInfo {
    private String reportTyp;
    private Integer rowsCount;
}
