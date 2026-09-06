package com.safwat.hr.controller.scale.scale;

public class ScaleApiService {
    private static ScaleApiService instance;

    public static ScaleApiService getInstance() {
        if (instance == null) {
            instance = new ScaleApiService();

        }
        return instance;
    }

    public void doSearch() {

    }
}
