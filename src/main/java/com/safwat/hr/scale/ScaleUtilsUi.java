package com.safwat.hr.scale;

import com.safwat.hr.ui.TextFieldSetupHelper;

public class ScaleUtilsUi {
    private final ScaleController controller;

    public ScaleUtilsUi(ScaleController controller) {
        this.controller = controller;
    }

    void setupDateFields() {
        TextFieldSetupHelper.setupDateFields("yyyy-MM-dd", controller.getTxt_startDate(), controller.getTxt_backStart(),
                controller.getTxt_startCut(),  controller.getTxt_endCut(),  controller.getTxt_regrade3(), controller.getTxt_regrade4(),
                controller.getTxt_regrade5(), controller.getTxt_backRegrade(),  controller.getTxt_debloma(),  controller.getTxt_magester(),
                controller.getTxt_doctoraa(),  controller.getDate_kader());
    }
}
