package com.safwat.hr.payroll.vocab.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import com.safwat.hr.network.ApiResponse;
import com.safwat.hr.payroll.dto.SearchVocab;

import com.safwat.hr.shared.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PayrollVocabService {
    public ApiResponse<List<SearchVocab>> searchVocab(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollReview.SEARCH,
                    request,
                    new TypeReference<List<SearchVocab>>() {
                    }
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean downloadVocab(PayrollRequest request, Path filePath) {
        try {
            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollReview.downloadReview,
                    request,
                    filePath

            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
